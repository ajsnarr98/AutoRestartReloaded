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
    private final Instant serverStartTime;

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
        this.serverStartTime = clock.instant();
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
            restartScheduler.cancelAll();
            currentRestartType = RestartType.NONE;

            Instant now = max(clock.instant(), serverStartTime.plus(config.getMinDelayBeforeAutoRestart()));
            Optional<Instant> nextTime = config.nextPreScheduledRestartTime(now);
            while (nextTime.isPresent()) {
                if (restartScheduler.scheduleRestartWithMessages(nextTime.get(), config.getScheduledRestartMessages())) {
                    // stop looping on successful scheduling
                    currentRestartType = RestartType.SCHEDULED;
                    break;
                }

                // we could not schedule at that time, start searching for the
                // next time after that one (at least 1 min after)
                now = nextTime.get().plus(Duration.ofMinutes(1));
                nextTime = config.nextPreScheduledRestartTime(now);
            }
            if (currentRestartType == RestartType.SCHEDULED) {
                AutoRestartReloaded.LOGGER.info("Scheduled next server restart");
            } else {
                AutoRestartReloaded.LOGGER.info("Skipped scheduling next server restart");
            }
        } finally {
            mutex.unlock();
        }
    }

    private <C extends Comparable<C>> C max(C first, C second) {
        return (first.compareTo(second) >= 0) ? first : second;
    }

    @Override
    public void close() throws IOException {
        restartScheduler.close();
    }

    private enum RestartType {
        NONE, MANUAL, SCHEDULED
    }
}
