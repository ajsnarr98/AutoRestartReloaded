package com.github.ajsnarr98.autorestartreloaded.core.task.executer;

import java.util.concurrent.ScheduledFuture;

public interface SchedulerFactory {

    Scheduler newDaemonThreadScheduler();

    /**
     * Creates and executes a periodic action that becomes enabled first after
     * the given initial delay, and subsequently with the given period; that is
     * executions will commence after initialDelay then initialDelay+period,
     * then initialDelay + 2 * period, and so on.
     *
     * @return a ScheduledFuture representing pending completion of the task,
     *         and whose get() method will throw an exception upon cancellation
     */
    ScheduledFuture<?> newDaemonThreadLoopingTask(Runnable task, long initialDelay, long periodMs);

    interface Scheduler {
        /**
         * Creates and executes a ScheduledFuture that runs after the given delay.
         *
         * @return a ScheduledFuture that can be used to extract result or cancel
         */
        ScheduledFuture<?> schedule(Runnable task, long delayMs);

        /**
         * Attempts to stop all actively executing tasks, and halts the processing
         * of waiting tasks.
         *
         * This method does not wait for actively executing tasks to terminate.
         *
         * There are no guarantees beyond best-effort attempts to stop
         * processing actively executing tasks. For example, typical
         * implementations will cancel via Thread.interrupt(), so any task that
         * fails to respond to interrupts may never terminate.
         */
        void shutdownNow();
    }
}
