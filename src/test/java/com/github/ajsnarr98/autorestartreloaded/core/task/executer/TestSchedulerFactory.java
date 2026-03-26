package com.github.ajsnarr98.autorestartreloaded.core.task.executer;

import com.github.ajsnarr98.autorestartreloaded.core.TestClock;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import static org.mockito.Mockito.spy;

public class TestSchedulerFactory implements SchedulerFactory, TestClock.TimeChangedListener {

    private TestClock clock;

    /**
     * Sets up this scheduler factory to create schedulers that advance in time
     * alongside the given {@link TestClock}.
     */
    public TestSchedulerFactory(TestClock clock) {
        this.clock = clock;
        addListeners();
    }

    /**
     * List of spy'ed schedulers.
     */
    public List<TestScheduler> schedulers = new ArrayList<>();

    public List<TestScheduler> schedulers(SchedulerFactory.Type type) {
        return schedulers.stream()
            .filter(scheduler -> scheduler.getType() == type)
            .toList();
    }

    /**
     * Sets up this scheduler factory to create schedulers that advance in time
     * alongside the given {@link TestClock}.
     */
    public void setClock(TestClock clock) {
        removeListeners();
        this.clock = clock;
        addListeners();
    }

    private void removeListeners() {
        clock.removeOnTimeChangedListener(this);
        for (TestScheduler scheduler : this.schedulers) {
            clock.removeOnTimeChangedListener(scheduler);
        }
    }

    private void addListeners() {
        clock.addOnTimeChangedListener(this);
        for (TestScheduler scheduler : this.schedulers) {
            clock.addOnTimeChangedListener(scheduler);
        }
    }

    @Override
    public Scheduler newDaemonThreadScheduler(Type type) {
        TestScheduler scheduler = spy(new TestScheduler(type, clock));
        clock.addOnTimeChangedListener(scheduler);
        schedulers.add(scheduler);
        return scheduler;
    }

    @Override
    public ScheduledFuture<?> newDaemonThreadLoopingTask(Runnable task, long initialDelay, long periodMs) {
        // TODO
        return null;
    }

    @Override
    public void onTimeUpdated(TestClock clock) {
        // TODO for looping task?
    }

    public static class TestScheduler implements Scheduler, TestClock.TimeChangedListener {

        public final ReentrantLock mutex = new ReentrantLock();
        public final List<TestFuture<?>> futures = new ArrayList<>();
        private TestClock clock;
        private Type type;

        public TestScheduler(Type type, TestClock clock) {
            this.clock = clock;
            this.type = type;
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable task, long delayMs) {
            TestFuture<?> toSchedule = new TestFuture<>(
                task, clock.instant().plus(Duration.ofMillis(delayMs)), clock
            );
            mutex.lock();
            futures.add(toSchedule);
            mutex.unlock();
            return toSchedule;
        }

        @Override
        public void shutdownNow() {
        }

        @Override
        public void onTimeUpdated(TestClock clock) {
            // Collect all due tasks first (under the lock), then run them without
            // holding the lock. This prevents ConcurrentModificationException when
            // a task calls schedule() and adds to `futures` during iteration.
            List<TestFuture<?>> toRun = new ArrayList<>();
            mutex.lock();
            try {
                this.clock = clock;
                Iterator<TestFuture<?>> it = futures.iterator();
                while (it.hasNext()) {
                    TestFuture<?> next = it.next();
                    next.setClock(clock);
                    if (next.hasBeenCanceled) {
                        it.remove();
                    } else if (next.getDelay(TimeUnit.MILLISECONDS) <= 0) {
                        // this task should have already run, prepare to run it
                        toRun.add(next);
                        it.remove();
                    }
                }
            } finally {
                mutex.unlock();
            }
            // run all tasks we still wanted toRun
            for (TestFuture<?> future : toRun) {
                try {
                    future.get();
                } catch (ExecutionException | InterruptedException ignored) {
                }
            }
        }
    }

    public static class TestFuture<T> implements ScheduledFuture<T> {

        public Runnable task;
        public Instant time;
        public boolean hasBeenCanceled = false;
        public boolean isDone = false;
        private TestClock clock;

        public TestFuture(Runnable task, Instant time, TestClock clock) {
            this.task = task;
            this.time = time;
            this.clock = clock;
        }

        public void setClock(TestClock clock) {
            this.clock = clock;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(Duration.between(clock.instant(), this.time));
        }

        @Override
        public int compareTo(@NonNull Delayed o) {
            return Long.compare(this.getDelay(TimeUnit.NANOSECONDS), o.getDelay(TimeUnit.NANOSECONDS));
        }

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
            return isDone;
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            task.run();
            this.isDone = true;
            return null;
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return get();
        }
    }
}
