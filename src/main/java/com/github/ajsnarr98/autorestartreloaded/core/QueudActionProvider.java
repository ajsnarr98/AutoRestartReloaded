package com.github.ajsnarr98.autorestartreloaded.core;

/**
 * Instantiates events created for a specific mod loader.
 */
public interface QueudActionProvider<T extends QueuedAction.RunContext> {
    /**
     * Create a new server stop event to run at the given time.
     * @param time event time in seconds since epoch
     */
    QueuedAction<T> newStopAction(long time);

    /**
     * Create a new message event to run at the given time.
     * @param time event time in seconds since epoch
     */
    QueuedAction<T> newRestartMessageAction(long time, String message);
}
