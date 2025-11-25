package com.github.ajsnarr98.autorestartreloaded.core.task;

import com.github.ajsnarr98.autorestartreloaded.core.servercontext.ServerContext;

public abstract class AbstractServerContextTask implements Runnable {
    protected final ServerContext serverContext;

    public AbstractServerContextTask(ServerContext serverContext) {
        this.serverContext = serverContext;
    }
}
