package com.github.ajsnarr98.autorestartreloaded.core;

import java.io.Closeable;

public interface RestartProcessor extends Closeable {
    /**
     * Initialize this restart processor for new config values.
     */
    void onConfigUpdated(Config config);

    void triggerRestartForCommand();
}
