package com.github.ajsnarr98.autorestartreloaded.gametest;

import com.github.ajsnarr98.autorestartreloaded.core.task.executer.SchedulerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A {@link SchedulerFactory} for game tests that ignores the requested delay and
 * runs all tasks as soon as possible (0 ms delay). This avoids the timing mismatch
 * where the game test server processes ticks faster than real wall-clock time.
 */
public class ImmediateSchedulerFactory implements SchedulerFactory {

    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, r -> {
        Thread t = new Thread(r, "gametest-immediate-scheduler");
        t.setDaemon(true);
        return t;
    });

    @Override
    public Scheduler newDaemonThreadScheduler(Type type) {
        return new Scheduler() {
            @Override
            public Type getType() {
                return type;
            }

            @Override
            public ScheduledFuture<?> schedule(Runnable task, long delayMs) {
                // Ignore delayMs — run immediately so the stop fires within the first tick.
                return executor.schedule(task, 0, TimeUnit.MILLISECONDS);
            }

            @Override
            public void shutdownNow() {
                executor.shutdownNow();
            }
        };
    }

    @Override
    public ScheduledFuture<?> newDaemonThreadLoopingTask(Runnable task, long initialDelay, long periodMs) {
        // No-op: looping tasks (e.g. TPS checker) are not needed in game tests.
        return NoOpScheduledFuture.INSTANCE;
    }

    private static final class NoOpScheduledFuture<V> implements ScheduledFuture<V> {
        @SuppressWarnings("rawtypes")
        static final NoOpScheduledFuture INSTANCE = new NoOpScheduledFuture<>();

        @Override public long getDelay(TimeUnit unit) { return 0; }
        @Override public int compareTo(Delayed o) { return 0; }
        @Override public boolean cancel(boolean mayInterruptIfRunning) { return false; }
        @Override public boolean isCancelled() { return false; }
        @Override public boolean isDone() { return true; }
        @Override public V get() { return null; }
        @Override public V get(long timeout, TimeUnit unit) { return null; }
    }
}
