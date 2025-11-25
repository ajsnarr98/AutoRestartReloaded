package com.github.ajsnarr98.autorestartreloaded.core.task;

import com.github.ajsnarr98.autorestartreloaded.core.servercontext.ServerContext;

/**
 * Instantiates events created for a specific mod loader.
 */
public interface QueuedTaskProvider {
    /**
     * Create a new server stop task.
     */
    AbstractServerContextTask newStopTask(ServerContext serverContext);

    /**
     * Create a new message-sending task.
     */
    AbstractServerContextTask newRestartMessageTask(ServerContext serverContext, String message);
}
