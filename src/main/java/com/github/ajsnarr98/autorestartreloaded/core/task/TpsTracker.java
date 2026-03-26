package com.github.ajsnarr98.autorestartreloaded.core.task;

import com.github.ajsnarr98.autorestartreloaded.core.Config;

import java.time.Clock;
import java.time.Instant;

/**
 * Tracks server TPS over a timeframe, and can detect if a restart is needed
 * due to TPS (based on user configs).
 */
public class TpsTracker {

    private static final long NS_PER_SECOND = 1_000_000_000;

    private Config config;
    private final Clock clock;

    /** The time of our last "healthy" tick where we were above the min tps threshold. **/
    private Instant lastTimeOfTpsAboveThreshold;

    /**
     * Maximum time ticks can take and still be considered "healthy enough".
     * Expressed in nanoseconds/tick.
     */
    private long tickTimeNanosThreshold;

    public TpsTracker(Clock clock, Config config) {
        this.clock = clock;
        this.config = config;
        reset();
    }

    /** Replace the config and reset TPS tracking. */
    public void updateConfig(Config config) {
        this.config = config;
        reset();
    }

    /**
     * Resets all tracking information, and initializes things.
     */
    public void reset() {
        lastTimeOfTpsAboveThreshold = this.clock.instant();

        // convert ticks/second into nanoseconds/tick
        tickTimeNanosThreshold = (long) (NS_PER_SECOND / config.getMinTpsLevel());
    }

    /**
     * Record one server tick.
     *
     * @param avgTickTimeNanos  server's rolling average tick time in milliseconds;
     *                       values {@code < 0} are treated as a normal 50 ms tick
     */
    public void recordTick(long avgTickTimeNanos) {
        if (avgTickTimeNanos <= tickTimeNanosThreshold) {
            // this was a "healthy enough" tick
            lastTimeOfTpsAboveThreshold = this.clock.instant();
        }
    }

    private Instant timeToRestart() {
        return this.lastTimeOfTpsAboveThreshold.plus(config.getLowTPSMinDuration());
    }

    /**
     * Returns true if server TPS has been below {@link Config#getMinTpsLevel()}
     * for the full {@link Config#getLowTPSMinDuration()}.
     */
    public boolean needToRestart() {
        return clock.instant().isAfter(timeToRestart());
    }
}
