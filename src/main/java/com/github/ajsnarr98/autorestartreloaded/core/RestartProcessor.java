package com.github.ajsnarr98.autorestartreloaded.core;

public interface RestartProcessor<T extends QueuedAction.RunContext> {
    /**
     * Initialize this restart processor for new config values.
     */
    void onConfigUpdated(Config config);

    void triggerRestartForCommand();

    void onServerTick(T context);
}
