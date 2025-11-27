package com.github.ajsnarr98.autorestartreloaded.core.task;

import com.github.ajsnarr98.autorestartreloaded.AutoRestartReloaded;
import com.github.ajsnarr98.autorestartreloaded.core.Config;
import com.github.ajsnarr98.autorestartreloaded.core.TimeUtils;
import com.github.ajsnarr98.autorestartreloaded.core.servercontext.ServerContext;
import com.github.ajsnarr98.autorestartreloaded.core.task.executer.SchedulerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class RestartScheduler implements Closeable {
    private final SchedulerFactory.Scheduler scheduler;

    private final List<ScheduledFuture<?>> tasks = new ArrayList<>();
    private final TreeSet<Long> taskTimesMs = new TreeSet<>();
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
     * Returns the next task time in milliseconds since epoch, or an empty optional.
     */
    public Optional<Long> nextTaskTimeMs() {
        long nowMs = clock.millis();
        long nextMs;
        while (!taskTimesMs.isEmpty()) {
            nextMs = taskTimesMs.first();
            if (nextMs < nowMs) {
                // this task already ran, remove it and continue
                taskTimesMs.removeFirst();
            } else {
                return Optional.of(nextMs);
            }
        }

        return Optional.empty();
    }

    /**
     * @param restartMillis time in milliseconds since epoch. Pass -1 to restart as soon as possible
     * @param messages      messages to send before restart
     * @return true if restart was scheduled successfully, false if we need to pick a later restart time
     */
    public boolean scheduleRestartWithMessages(long restartMillis, Config.RestartMessages messages) {
        long now = clock.millis();

        // handle if -1 was passed in for restart time
        long trueRestartTime; // ms
        if (restartMillis < 0) {
            trueRestartTime = now + messages.highestLeadingMs + 1000;
        } else {
            trueRestartTime = restartMillis;
        }

        if (trueRestartTime - messages.highestLeadingMs < now) {
            // we are too close to this restart time to properly send messages
            AutoRestartReloaded.LOGGER.debug("Too close to attempted restart time to properly send messages");
            return false;
        }

        long trueRestartDelay = trueRestartTime - now;

        for (Config.RestartMessage message : messages.messages) {
            scheduleSingle(
                taskProvider.newRestartMessageTask(serverContext, message.message),
                trueRestartDelay - message.msBeforeRestart
            );
        }
        scheduleSingle(
            taskProvider.newStopTask(serverContext),
            trueRestartDelay
        );

        return true;
    }

    private void scheduleSingle(Runnable command, long delayMs) {
        tasks.add(scheduler.schedule(command, delayMs));
        taskTimesMs.add(clock.millis() + delayMs);
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
        taskTimesMs.clear();
    }

    @Override
    public void close() throws IOException {
        this.cancelAll();
        this.scheduler.shutdownNow();
    }
}
