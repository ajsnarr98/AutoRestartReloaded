package com.github.ajsnarr98.autorestartreloaded.core.task.executer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class DefaultSchedulerFactory implements SchedulerFactory {

    @Override
    public Scheduler newDaemonThreadScheduler(Type type) {
        return new DefaultScheduler(type);
    }

    @Override
    public ScheduledFuture<?> newDaemonThreadLoopingTask(Runnable task, long initialDelay, long periodMs) {
        ScheduledExecutorService executorService = newExecutorService();
        return executorService.scheduleAtFixedRate(task, initialDelay, periodMs, TimeUnit.MILLISECONDS);
    }

    private static final ThreadFactory daemonThreadFactory = runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true); // Mark the thread as a daemon
        return thread;
    };

    private static ScheduledExecutorService newExecutorService() {
        return Executors.newScheduledThreadPool(1, daemonThreadFactory);
    }

    private static class DefaultScheduler implements SchedulerFactory.Scheduler {

        private final Type type;
        private final ScheduledExecutorService scheduler = newExecutorService();

        public DefaultScheduler(Type type) {
            this.type = type;
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable task, long delayMs) {
            return scheduler.schedule(task, delayMs, TimeUnit.MILLISECONDS);
        }

        @Override
        public void shutdownNow() {
            scheduler.shutdownNow();
        }
    }
}
