package com.github.ajsnarr98.autorestartreloaded;

import com.github.ajsnarr98.autorestartreloaded.core.*;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class AutoRestartReloaded {
    public static final String MODID = /*$ modid*/ "autorestartreloaded";
    public static final String MINECRAFT_VERSION = /*$ minecraft*/ "1.21.10";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static AutoRestartReloaded INSTANCE = new AutoRestartReloaded();

    private RestartProcessor restartProcessor;

    /**
     * Initialize with the given config. This can be called multiple times.
     */
    void initialize(QueudActionProvider actionProvider, Config config) {
        this.restartProcessor = new RestartProcessorImpl(actionProvider, config);
    }

    void onServerTick(QueuedAction.RunContext context) {
        this.restartProcessor.onServerTick(context);
    }

    void onManualRestartCommand() {
        this.restartProcessor.triggerRestartForCommand();
    }
}
