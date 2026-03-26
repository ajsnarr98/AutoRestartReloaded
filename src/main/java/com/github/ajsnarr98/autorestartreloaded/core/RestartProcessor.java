package com.github.ajsnarr98.autorestartreloaded.core;

import java.io.Closeable;

public interface RestartProcessor extends Closeable {
    /**
     * Initialize this restart processor for new config values.
     */
    void onConfigUpdated(Config config);

    void triggerRestartForCommand();

    /**
     * Called once per server tick. Used to track TPS and trigger a dynamic restart
     * if the server has been unhealthy for long enough.
     *
     * @param avgTickTimeNanos server's rolling average tick time in nanoseconds
     */
    void onServerTick(long avgTickTimeNanos);
}
