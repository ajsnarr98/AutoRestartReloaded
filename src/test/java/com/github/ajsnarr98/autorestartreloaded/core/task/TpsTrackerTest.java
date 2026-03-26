package com.github.ajsnarr98.autorestartreloaded.core.task;

import com.github.ajsnarr98.autorestartreloaded.core.BaseRestartProcessorTest;
import com.github.ajsnarr98.autorestartreloaded.core.Config;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class TpsTrackerTest extends BaseRestartProcessorTest {

    /**
     * 200 ms/tick → 5 TPS.
     * tickTimeNanosThreshold = 1_000_000_000 / minTpsLevel(10) = 100_000_000 ns.
     * 200_000_000 > 100_000_000 → unhealthy.
     */
    private static final long UNHEALTHY_TICK_NANOS = 200_000_000L;
    /**
     * 50 ms/tick → 20 TPS.
     * 50_000_000 ≤ 100_000_000 → healthy.
     */
    private static final long HEALTHY_TICK_NANOS = 50_000_000L;
    /** Default window: lowTpsMinMinutes=1 → 60 seconds */
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private TpsTracker tpsTracker() {
        Config config = new TestConfigBuilder()
            .shouldRestartForTps(true)
            .build();
        return new TpsTracker(clock, config);
    }

    @Test
    void noTriggerInitially() {
        TpsTracker tracker = tpsTracker();
        assertThat(tracker.needToRestart()).isFalse();
    }

    @Test
    void noTriggerBeforeWindowElapses() {
        TpsTracker tracker = tpsTracker();

        advanceTimeBy(WINDOW.minusMillis(1));

        assertThat(tracker.needToRestart()).isFalse();
    }

    @Test
    void triggersAfterWindowWithNoHealthyTicks() {
        TpsTracker tracker = tpsTracker();

        advanceTimeBy(WINDOW.plusMillis(1));

        assertThat(tracker.needToRestart()).isTrue();
    }

    @Test
    void unhealthyTickDoesNotResetTimer() {
        TpsTracker tracker = tpsTracker();

        // Record an unhealthy tick just before the window expires
        advanceTimeBy(WINDOW.minusMillis(1));
        tracker.recordTick(UNHEALTHY_TICK_NANOS);

        // The timer was NOT reset, so crossing the window boundary still triggers
        advanceTimeBy(Duration.ofMillis(2));
        assertThat(tracker.needToRestart()).isTrue();
    }

    @Test
    void healthyTickResetsTimer() {
        TpsTracker tracker = tpsTracker();

        // Record a healthy tick near the window boundary
        advanceTimeBy(WINDOW.minusMillis(1));
        tracker.recordTick(HEALTHY_TICK_NANOS);

        // The timer was reset, so we need another full window before triggering
        advanceTimeBy(WINDOW.minusMillis(1));
        assertThat(tracker.needToRestart()).isFalse();
    }

    @Test
    void resetClearsTimerToNow() {
        TpsTracker tracker = tpsTracker();

        // Advance past window — should trigger
        advanceTimeBy(WINDOW.plusMillis(1));
        assertThat(tracker.needToRestart()).isTrue();

        // Reset clears the timer: should not trigger immediately
        tracker.reset();
        assertThat(tracker.needToRestart()).isFalse();

        // Must wait a full window from the reset point before triggering again
        advanceTimeBy(WINDOW.plusMillis(1));
        assertThat(tracker.needToRestart()).isTrue();
    }

    @Test
    void updateConfigResetsState() {
        TpsTracker tracker = tpsTracker();

        // Advance past window — should trigger
        advanceTimeBy(WINDOW.plusMillis(1));
        assertThat(tracker.needToRestart()).isTrue();

        // updateConfig resets the timer to now
        Config newConfig = new TestConfigBuilder()
            .shouldRestartForTps(true)
            .lowTpsMinMinutes(2)
            .build();
        tracker.updateConfig(newConfig);
        assertThat(tracker.needToRestart()).isFalse();
    }

    @Test
    void zeroNanosTickIsTreatedAsHealthy() {
        TpsTracker tracker = tpsTracker();

        // Record a 0-nanosecond tick (0 ≤ threshold → healthy)
        advanceTimeBy(WINDOW.minusMillis(1));
        tracker.recordTick(0L);

        // Timer was reset by the healthy tick — should not trigger for another window
        advanceTimeBy(WINDOW.minusMillis(1));
        assertThat(tracker.needToRestart()).isFalse();
    }
}
