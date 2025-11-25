package com.github.ajsnarr98.autorestartreloaded.core.servercontext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

public class RealServerContext implements ServerContext {
    private final MinecraftServer server;

    public RealServerContext(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void runCommand(String command) {
        CommandSourceStack consoleSource = server.createCommandSourceStack();
        server.getCommands().performPrefixedCommand(consoleSource, command);
    }
}
