package com.github.ajsnarr98.autorestartreloaded.core.task;

import com.github.ajsnarr98.autorestartreloaded.core.servercontext.ServerContext;

public class MinecraftConsoleTask extends AbstractServerContextTask {
    private final String command;

    public MinecraftConsoleTask(ServerContext serverContext, String command) {
        super(serverContext);
        this.command = command;
    }

    @Override
    public void run() {
        serverContext.runCommand(command);
    }

    public static MinecraftConsoleTask forServerStop(ServerContext serverContext) {
        return new MinecraftConsoleTask(serverContext, "stop");
    }

    public static MinecraftConsoleTask forRestartMessage(ServerContext serverContext, String message) {
        return new MinecraftConsoleTask(serverContext, String.format("tellraw @a {\"text\":\"%s\",\"color\":\"yellow\"}", message));
    }

    @Override
    public String toString() {
        return String.format("command: '%s'", this.command);
    }
}
