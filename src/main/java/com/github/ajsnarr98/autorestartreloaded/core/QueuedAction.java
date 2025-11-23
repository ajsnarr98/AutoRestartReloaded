package com.github.ajsnarr98.autorestartreloaded.core;

import org.jetbrains.annotations.NotNull;

public interface QueuedAction<T extends QueuedAction.RunContext> extends Comparable<QueuedAction<T>> {

    /**
     * @return time that the action should run at in _seconds_ since epoch
     */
    long getTime();

    /**
     * Run in context specific to the current mod loader.
     */
    void run(T context);

    /**
     * Copies this action with a new time value.
     *
     * @param time a new time value.
     * @return a shallow copy of this action with the new time.
     */
    QueuedAction<T> copy(long time);

    /**
     * Run context specific to a mod loader.
     */
    interface RunContext {}

    @Override
    default int compareTo(@NotNull QueuedAction other) {
        return Long.compare(this.getTime(), other.getTime());
    }
}
