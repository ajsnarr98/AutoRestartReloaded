package com.github.ajsnarr98.autorestartreloaded.gametest;

import com.github.ajsnarr98.autorestartreloaded.core.servercontext.ServerContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A test double for {@link ServerContext} that records all commands passed to
 * {@link #runCommand} without actually executing them. Thread-safe so it can be
 * read from the server tick thread while the real {@link java.util.concurrent.ScheduledExecutorService}
 * writes to it from a background daemon thread.
 */
public class FakeServerContext implements ServerContext {

    private final List<String> commands = new ArrayList<>();

    @Override
    public synchronized void runCommand(String command) {
        commands.add(command);
    }

    public synchronized List<String> getCommands() {
        return new ArrayList<>(commands);
    }

    public synchronized void reset() {
        commands.clear();
    }
}
