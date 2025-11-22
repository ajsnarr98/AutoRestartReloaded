package com.github.ajsnarr98.autorestartreloaded.core;

import com.github.ajsnarr98.autorestartreloaded.AutoRestartReloaded;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

public class RestartProcessorImpl<T extends QueuedAction.RunContext> implements RestartProcessor<T> {
    private final TreeSet<QueuedAction<T>> queue = new TreeSet<>();
    private final ReentrantLock mutex = new ReentrantLock();
    private long nextQueueTime = Long.MAX_VALUE;
    private RestartType currentRestartType = RestartType.NONE;
    private Config config;
    private QueudActionProvider<T> actionProvider;

    public RestartProcessorImpl(QueudActionProvider<T> actionProvider, Config config) {
        this.config = config;
        this.actionProvider = actionProvider;
        setupQueueForScheduledTimes();
    }

    @Override
    public void onConfigUpdated(Config config) {
        this.config = config;
        setupQueueForScheduledTimes();
    }

    @Override
    public void onServerTick(T context) {
        long now = Instant.now().getEpochSecond();
        if (nextQueueTime >= now) {
            mutex.lock();
            try {
                popQueue().run(context);
            } finally {
                mutex.unlock();
            }
        }
    }

    @Override
    public void triggerRestartForCommand() {
        mutex.lock();
        try {
            AutoRestartReloaded.LOGGER.info("Scheduling for restart command");
            clearQueue();

            scheduleRestartWithMessages(-1, config.getRestartCommandMessages());
            currentRestartType = RestartType.MANUAL;
        } finally {
            mutex.unlock();
        }
    }

    private void setupQueueForScheduledTimes() {
        mutex.lock();
        try {
            AutoRestartReloaded.LOGGER.info("Attempting to schedule next restart");
            clearQueue();

            ZoneId timezone = config.getTimezone();
            ZonedDateTime now = ZonedDateTime.now(timezone);
            Optional<Long> nextTime = config.nextPreScheduledRestartTime(now);
            while (nextTime.isPresent()) {
                // TODO use different messages
                if (scheduleRestartWithMessages(nextTime.get(), config.getRestartCommandMessages())) {
                    // stop looping on successful scheduling
                    currentRestartType = RestartType.SCHEDULED;
                    break;
                }

                // we could not schedule at that time, start searching for the
                // next time after that one
                now = ZonedDateTime.ofInstant(Instant.ofEpochSecond(nextTime.get() + 1), timezone);
                nextTime = config.nextPreScheduledRestartTime(now);
            }
        } finally {
            mutex.unlock();
        }
    }

    /**
     *
     * @param restartTime time in seconds since epoch. Pass -1 to restart as soon as possible
     * @param messages messages to send before restart
     * @return true if restart was scheduled successfully, false if we need to pick a later restart time
     */
    private boolean scheduleRestartWithMessages(long restartTime, List<Config.RestartMessage> messages) {
        long now = Instant.now().getEpochSecond();
        long highestLeadingSeconds = 0;

        for (Config.RestartMessage message : messages) {
            highestLeadingSeconds = Math.max(highestLeadingSeconds, message.secondsBeforeRestart);
        }

        // handle if -1 was passed in for restart time
        long trueRestartTime;
        if (restartTime < 0) {
            trueRestartTime = now + highestLeadingSeconds + 1;
        } else {
            trueRestartTime = restartTime;
        }

        if (trueRestartTime - highestLeadingSeconds < now) {
            // we are too close to this restart time to properly send messages
            return false;
        }

        for (Config.RestartMessage message : messages) {
            scheduleAction(actionProvider.newRestartMessageAction(
                    trueRestartTime - message.secondsBeforeRestart,
                    message.message
            ));
        }
        scheduleAction(actionProvider.newStopAction(trueRestartTime));

        return true;
    }

    private void scheduleAction(QueuedAction<T> action) {
        queue.add(action);
        nextQueueTime = queue.last().getTime();
    }

    private QueuedAction<T> popQueue() {
        QueuedAction<T> next = queue.removeLast();
        if (queue.isEmpty()) {
            nextQueueTime = Long.MAX_VALUE;
        } else {
            nextQueueTime = queue.last().getTime();
        }
        return next;
    }

    private void clearQueue() {
        queue.clear();
        nextQueueTime = Long.MAX_VALUE;
        currentRestartType = RestartType.NONE;
    }

    private static enum RestartType {
        NONE, MANUAL, SCHEDULED
    }
}
