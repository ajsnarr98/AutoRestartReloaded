package com.github.ajsnarr98.autorestartreloaded.core;

import com.github.ajsnarr98.autorestartreloaded.AutoRestartReloaded;
import com.github.ajsnarr98.autorestartreloaded.core.servercontext.ServerContext;
import com.github.ajsnarr98.autorestartreloaded.core.task.QueuedTaskProvider;
import com.github.ajsnarr98.autorestartreloaded.core.task.RestartScheduler;
import com.github.ajsnarr98.autorestartreloaded.core.task.TpsTracker;
import com.github.ajsnarr98.autorestartreloaded.core.task.executer.SchedulerFactory;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

public class RestartProcessorImpl implements RestartProcessor {
    private final ReentrantLock mutex = new ReentrantLock();
    private RestartType currentRestartType = RestartType.NONE;
    private Config config;
    private final RestartScheduler restartScheduler;
    private final SchedulerFactory.Scheduler minTimeCheckScheduler;
    private final List<ScheduledFuture<?>> minTimeCheckTasks = new ArrayList<>();

    private Clock clock;
    private final Instant serverStartTime;
    private final TpsTracker tpsTracker;

    private boolean hasBeenMinTimeBeforeRestart;

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
        this.tpsTracker = new TpsTracker(config, clock);
        this.minTimeCheckScheduler = schedulerFactory.newDaemonThreadScheduler(
            SchedulerFactory.Type.MIN_TIME_CHECKER
        );
        setHasBeenMinTimeBeforeRestartAndScheduleUpdateIfNeeded();

        setupQueueForScheduledTimes();
    }

    /**
     * Sets the best current value of {@link RestartProcessorImpl#hasBeenMinTimeBeforeRestart},
     * and has the side effect of scheduling a future check for when it needs to change to true.
     */
    private void setHasBeenMinTimeBeforeRestartAndScheduleUpdateIfNeeded() {
        Instant minInstant = serverStartTime.plus(config.getMinDelayBeforeAutoRestart());
        this.hasBeenMinTimeBeforeRestart = clock.instant().isAfter(minInstant);

        if (!this.hasBeenMinTimeBeforeRestart) {
            Duration dif = Duration.between(clock.instant(), minInstant);

            cancelAllMinTimeCheckTasks();
            minTimeCheckTasks.add(
                minTimeCheckScheduler.schedule(
                    this::setHasBeenMinTimeBeforeRestartAndScheduleUpdateIfNeeded,
                    // make sure just in case that we always schedule something in the future
                    Math.max(dif.toMillis(), 1)
                )
            );
        }
    }

    private void cancelAllMinTimeCheckTasks() {
        for (ScheduledFuture<?> task : minTimeCheckTasks) {
            task.cancel(false);
        }
        minTimeCheckTasks.clear();
    }

    @Override
    public void onConfigUpdated(Config config) {
        this.config = config;
        // Adopt the new timezone. In tests, Clock.withZone() is overridden to mutate
        // the existing TestClock in place and return the same instance, so all internal
        // components (RestartScheduler, TpsTracker) that share the clock reference
        // automatically see the updated zone without requiring their own clock fields
        // to be reassigned.
        this.clock = this.clock.withZone(config.getTimezone());
        this.tpsTracker.updateConfig(config, clock);
        this.restartScheduler.setClock(clock);
        setHasBeenMinTimeBeforeRestartAndScheduleUpdateIfNeeded();
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

    @Override
    public void onServerTick(long avgTickTimeNanos) {
        if (config.shouldRestartForTps()) {
            tpsTracker.recordTick(avgTickTimeNanos);
            if (hasBeenMinTimeBeforeRestart && tpsTracker.needToRestart() && currentRestartType != RestartType.DYNAMIC) {
                triggerDynamicRestart();
            }
        }
    }

    private void triggerDynamicRestart() {
        mutex.lock();
        try {
            // Manual and already-triggered dynamic restarts take precedence
            if (currentRestartType == RestartType.MANUAL || currentRestartType == RestartType.DYNAMIC) {
                return;
            }
            AutoRestartReloaded.LOGGER.info("Scheduling dynamic restart due to sustained low TPS");
            restartScheduler.cancelAll();
            restartScheduler.scheduleRestartWithMessages(null, config.getDynamicRestartMessages());
            currentRestartType = RestartType.DYNAMIC;
            tpsTracker.reset();
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
        NONE, MANUAL, SCHEDULED, DYNAMIC
    }
}
