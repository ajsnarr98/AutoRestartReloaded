package com.github.ajsnarr98.autorestartreloaded.core.task;

import com.github.ajsnarr98.autorestartreloaded.core.servercontext.ServerContext;

public class DefaultTaskProvider implements QueuedTaskProvider {
    @Override
    public AbstractServerContextTask newStopTask(ServerContext serverContext) {
        return MinecraftConsoleTask.forServerStop(serverContext);
    }

    @Override
    public AbstractServerContextTask newRestartMessageTask(ServerContext serverContext, String message) {
        return MinecraftConsoleTask.forRestartMessage(serverContext, message);
    }
}
