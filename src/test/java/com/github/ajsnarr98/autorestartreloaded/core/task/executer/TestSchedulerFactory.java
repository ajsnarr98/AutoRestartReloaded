package com.github.ajsnarr98.autorestartreloaded.core.task.executer;

import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TestSchedulerFactory implements SchedulerFactory {

    /**
     * List of mocked schedulers.
     */
    public List<Scheduler> schedulers = new ArrayList<>();

    @Override
    public Scheduler newDaemonThreadScheduler() {
        schedulers.add(spy(new TestScheduler()));
        return schedulers.getLast();
    }

    @Override
    public ScheduledFuture<?> newDaemonThreadLoopingTask(Runnable task, long initialDelay, long periodMs) {
        // TODO
        return null;
    }

    public static class TestScheduler implements Scheduler {

        @Override
        public ScheduledFuture<?> schedule(Runnable task, long delayMs) {
            return new TestFuture<>();
        }

        @Override
        public void shutdownNow() {}
    }

    public static class TestFuture<T> implements ScheduledFuture<T> {

        public boolean hasBeenCanceled = false;

        @Override
        public long getDelay(TimeUnit unit) { return 0; }

        @Override
        public int compareTo(Delayed o) { return 0; }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (hasBeenCanceled) {
                return false;
            } else {
                hasBeenCanceled = true;
                return true;
            }
        }

        @Override
        public boolean isCancelled() {
            return hasBeenCanceled;
        }

        @Override
        public boolean isDone() {
            return hasBeenCanceled;
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            return null;
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }
    }
}
