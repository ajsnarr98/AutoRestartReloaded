package com.github.ajsnarr98.autorestartreloaded.core.task;

import com.github.ajsnarr98.autorestartreloaded.AutoRestartReloaded;
import com.github.ajsnarr98.autorestartreloaded.core.Config;
import com.github.ajsnarr98.autorestartreloaded.core.TimeUtils;
import com.github.ajsnarr98.autorestartreloaded.core.servercontext.ServerContext;
import com.github.ajsnarr98.autorestartreloaded.core.task.executer.SchedulerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

public class RestartScheduler implements Closeable {
    private final SchedulerFactory.Scheduler scheduler;

    private final List<ScheduledFuture<?>> tasks = new ArrayList<>();
    private final QueuedTaskProvider taskProvider;
    private final ServerContext serverContext;
    private final Clock clock;

    public RestartScheduler(QueuedTaskProvider taskProvider, ServerContext serverContext, Clock clock, SchedulerFactory schedulerFactory) {
        this.taskProvider = taskProvider;
        this.clock = clock;
        this.serverContext = serverContext;
        this.scheduler = schedulerFactory.newDaemonThreadScheduler();
    }

    public boolean hasRunningTasks() {
        return !tasks.isEmpty();
    }

    /**
     * @param restartTime time to restart. Pass null to restart as soon as possible
     * @param messages      messages to send before restart
     * @return true if restart was scheduled successfully, false if we need to pick a later restart time
     */
    public boolean scheduleRestartWithMessages(Instant restartTime, Config.RestartMessages messages) {
        Instant now = clock.instant();

        TemporalAmount necessaryLeadTime = messages.highestLeadingTime.plus(Duration.ofSeconds(1));

        // handle if null was passed in for restart time
        Instant trueRestartTime; // ms
        if (restartTime == null) {
            trueRestartTime = now.plus(necessaryLeadTime);
        } else {
            trueRestartTime = restartTime;
        }

        if ((trueRestartTime.minus(necessaryLeadTime)).isBefore(now)) {
            // we are too close to this restart time to properly send messages
            AutoRestartReloaded.LOGGER.debug("Too close to attempted restart time to properly send messages");
            return false;
        }

        long trueRestartDelayMs = Duration.between(now, trueRestartTime).toMillis();

        for (Config.RestartMessage message : messages.messages) {
            scheduleSingle(
                taskProvider.newRestartMessageTask(serverContext, message.message),
                trueRestartDelayMs - message.msBeforeRestart
            );
        }
        scheduleSingle(
            taskProvider.newStopTask(serverContext),
            trueRestartDelayMs
        );

        return true;
    }

    private void scheduleSingle(Runnable command, long delayMs) {
        tasks.add(scheduler.schedule(command, delayMs));
        AutoRestartReloaded.LOGGER.debug(
            String.format(
                "Scheduling task: time: %s | %s",
                TimeUtils.getHumanReadableTime(clock.millis() + delayMs),
                command
            )
        );
    }

    public void cancelAll() {
        for (ScheduledFuture<?> task : tasks) {
            task.cancel(false);
        }
        tasks.clear();
    }

    @Override
    public void close() throws IOException {
        this.cancelAll();
        this.scheduler.shutdownNow();
    }
}
