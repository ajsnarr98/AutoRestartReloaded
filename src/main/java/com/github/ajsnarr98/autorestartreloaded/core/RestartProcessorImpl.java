package com.github.ajsnarr98.autorestartreloaded.core;

import com.github.ajsnarr98.autorestartreloaded.AutoRestartReloaded;
import com.github.ajsnarr98.autorestartreloaded.core.servercontext.ServerContext;
import com.github.ajsnarr98.autorestartreloaded.core.task.QueuedTaskProvider;
import com.github.ajsnarr98.autorestartreloaded.core.task.RestartScheduler;
import com.github.ajsnarr98.autorestartreloaded.core.task.executer.SchedulerFactory;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

public class RestartProcessorImpl implements RestartProcessor {
    private final ReentrantLock mutex = new ReentrantLock();
    private RestartType currentRestartType = RestartType.NONE;
    private Config config;
    private final RestartScheduler restartScheduler;
    private final Clock clock;

    public RestartProcessorImpl(
        QueuedTaskProvider taskProvider,
        ServerContext serverContext,
        Config config,
        Clock clock,
        SchedulerFactory schedulerFactory
    ) {
        this.config = config;
        this.restartScheduler = new RestartScheduler(
            taskProvider,
            serverContext,
            clock,
            schedulerFactory
        );
        this.clock = clock;
        setupQueueForScheduledTimes();
    }

    @Override
    public void onConfigUpdated(Config config) {
        // TODO handle clock updating
        this.config = config;
        setupQueueForScheduledTimes();
    }

    @Override
    public void triggerRestartForCommand() {
        mutex.lock();
        try {
            AutoRestartReloaded.LOGGER.info("Scheduling for restart command");
            restartScheduler.cancelAll();

            restartScheduler.scheduleRestartWithMessages(null, config.getRestartCommandMessages());
            currentRestartType = RestartType.MANUAL;
        } finally {
            mutex.unlock();
        }
    }

    private void setupQueueForScheduledTimes() {
        mutex.lock();
        try {
            AutoRestartReloaded.LOGGER.info("Attempting to schedule next restart");
            restartScheduler.cancelAll();

            Instant now = clock.instant();
            Optional<Instant> nextTime = config.nextPreScheduledRestartTime(now);
            while (nextTime.isPresent()) {
                // TODO use different messages
                if (restartScheduler.scheduleRestartWithMessages(nextTime.get(), config.getRestartCommandMessages())) {
                    // stop looping on successful scheduling
                    currentRestartType = RestartType.SCHEDULED;
                    break;
                }

                // we could not schedule at that time, start searching for the
                // next time after that one (at least 1 min after)
                now = nextTime.get().plus(Duration.ofMinutes(1));
                nextTime = config.nextPreScheduledRestartTime(now);
            }
        } finally {
            mutex.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        restartScheduler.close();
    }

    private enum RestartType {
        NONE, MANUAL, SCHEDULED
    }
}
