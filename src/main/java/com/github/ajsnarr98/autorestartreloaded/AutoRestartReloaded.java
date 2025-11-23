package com.github.ajsnarr98.autorestartreloaded;

import com.github.ajsnarr98.autorestartreloaded.core.*;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

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
    public void initialize(QueudActionProvider actionProvider, Config config) {
        this.restartProcessor = new RestartProcessorImpl(actionProvider, config);
    }

    public void onServerTick(QueuedAction.RunContext context) {
        this.restartProcessor.onServerTick(context);
    }

    public void onManualRestartCommand() {
        this.restartProcessor.triggerRestartForCommand();
    }
}
