package com.github.ajsnarr98.autorestartreloaded;

import com.github.ajsnarr98.autorestartreloaded.core.Config;
import com.github.ajsnarr98.autorestartreloaded.core.RestartProcessor;
import com.github.ajsnarr98.autorestartreloaded.core.RestartProcessorImpl;
import com.github.ajsnarr98.autorestartreloaded.core.servercontext.ServerContext;
import com.github.ajsnarr98.autorestartreloaded.core.task.QueuedTaskProvider;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.time.Clock;

public class AutoRestartReloaded {
    public static final String MODID = /*$ modid*/ "autorestartreloaded";
    public static final String MINECRAFT_VERSION = /*$ minecraft*/ "1.21.10";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static AutoRestartReloaded INSTANCE = new AutoRestartReloaded();

    public static AutoRestartReloaded getInstance() {
        return INSTANCE;
    }

    private RestartProcessor restartProcessor;

    /**
     * Initialize with the given config. This can be called multiple times.
     */
    public void initialize(QueuedTaskProvider taskProvider, ServerContext serverContext, Config config, Clock clock) {
        this.restartProcessor = new RestartProcessorImpl(taskProvider, serverContext, config, clock);
    }

    public void onManualRestartCommand() {
        this.restartProcessor.triggerRestartForCommand();
    }

    public void onServerStopped() throws IOException {
        this.restartProcessor.close();
        this.restartProcessor = null;
    }
}
