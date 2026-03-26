package com.github.ajsnarr98.autorestartreloaded.core.restartprocessor;

import com.github.ajsnarr98.autorestartreloaded.core.BaseRestartProcessorTest;
import com.github.ajsnarr98.autorestartreloaded.core.RestartProcessor;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class RestartProcessorDynamicRestartTest extends BaseRestartProcessorTest {

    /** 200 ms/tick → 5 TPS, below the configured threshold of 10 */
    private static final long LOW_TICK_TIME_NANOS = 200_000_000;
    /** 50 ms/tick → 20 TPS, above the threshold */
    private static final long NORMAL_TICK_TIME_NANOS = 50_000_000;

    private static final int DEFAULT_LOW_TPS_MINUTES = 1;
    /** A non-trivial min-delay longer than the TPS window, used to test their interaction. */
    private static final int MIN_DELAY_MINUTES = 5;

    @Test
    void dynamicRestartTriggersAfterSustainedLowTps() {
        this.config = new TestConfigBuilder()
            .minMinutesBeforeAutoRestart(0)
            .shouldRestartForTps(true)
            .build();
        RestartProcessor processor = getRestartProcessor();

        // Initial scheduled-restart tasks: 12 messages + 1 stop = 13
        int initialScheduledTimes = 13;
        verify(assertRestartScheduler(), times(initialScheduledTimes)).schedule(any(), anyLong());

        processor.onServerTick(LOW_TICK_TIME_NANOS);
        advanceTimeBy(Duration.ofMinutes(DEFAULT_LOW_TPS_MINUTES));
        advanceTimeBy(Duration.ofMillis(1));
        processor.onServerTick(LOW_TICK_TIME_NANOS);
        processor.onServerTick(NORMAL_TICK_TIME_NANOS);

        // Previous 13 tasks cancelled; 10 new tasks scheduled (9 dynamic messages + 1 stop = 10)
        verify(assertRestartScheduler(), times(initialScheduledTimes + 10))
            .schedule(any(), anyLong());

        // Verify the per-message delays relative to the moment the dynamic restart was scheduled.
        // necessaryLeadTime = highestLeadingTime(60s) + 1s = 61s, so trueRestartDelayMs = 61 000 ms.
        verify(assertRestartScheduler()).schedule(any(), eq(1_000L));   // "1 minute" message
        verify(assertRestartScheduler()).schedule(any(), eq(31_000L));  // "30 seconds" message
        verify(assertRestartScheduler()).schedule(any(), eq(46_000L));  // "15 seconds" message
        verify(assertRestartScheduler()).schedule(any(), eq(51_000L));  // "10 seconds" message
        verify(assertRestartScheduler()).schedule(any(), eq(56_000L));  // "5 seconds"  message
        verify(assertRestartScheduler()).schedule(any(), eq(57_000L));  // "4 seconds"  message
        verify(assertRestartScheduler()).schedule(any(), eq(58_000L));  // "3 seconds"  message
        verify(assertRestartScheduler()).schedule(any(), eq(59_000L));  // "2 seconds"  message
        verify(assertRestartScheduler()).schedule(any(), eq(60_000L));  // "1 second"   message
        verify(assertRestartScheduler()).schedule(any(), eq(61_000L));  // stop

        // Nothing has run yet
        verify(serverContext, times(0)).runCommand(anyString());

        // After 1 s, the "1 minute" announcement fires
        advanceTimeBy(Duration.ofSeconds(1));
        verify(serverContext, times(1)).runCommand(
            "tellraw @a {\"text\":\"Server restarting in 1 minute due to performance issues...\",\"color\":\"yellow\"}"
        );

        // The server stop command runs at 61 s after the trigger
        advanceTimeBy(Duration.ofSeconds(60));
        verify(serverContext, times(1)).runCommand("stop");

        // Make sure that previous task got canceled
        advanceTimeBy(Duration.ofHours(10));
        verify(serverContext, times(1)).runCommand("stop");
    }

    @Test
    void dynamicRestartDoesNotTriggerUntilRightAfterMinimumTime() {
        this.config = new TestConfigBuilder()
            .minMinutesBeforeAutoRestart(0)
            .shouldRestartForTps(true)
            .build();
        RestartProcessor processor = getRestartProcessor();

        // Initial scheduled-restart tasks: 12 messages + 1 stop = 13
        int initialScheduledTimes = 13;
        verify(assertRestartScheduler(), times(initialScheduledTimes)).schedule(any(), anyLong());

        processor.onServerTick(LOW_TICK_TIME_NANOS);
        advanceTimeBy(Duration.ofMinutes(DEFAULT_LOW_TPS_MINUTES));
        processor.onServerTick(LOW_TICK_TIME_NANOS);
        processor.onServerTick(NORMAL_TICK_TIME_NANOS);

        // Previous 13 tasks continue
        verify(assertRestartScheduler(), times(initialScheduledTimes))
            .schedule(any(), anyLong());

        advanceTimeBy(Duration.ofHours(10));
        verify(serverContext, times(1)).runCommand(
            "tellraw @a {\"text\":\"Server restarting in 30 minutes for scheduled restart\",\"color\":\"yellow\"}"
        );
        verify(serverContext, times(1)).runCommand("stop");
    }

    @Test
    void dynamicRestartDoesNotOverrideManualRestart() {
        this.config = new TestConfigBuilder()
            .minMinutesBeforeAutoRestart(0)
            .shouldRestartForTps(true)
            .build();
        RestartProcessor processor = getRestartProcessor();

        // Trigger a manual /restart command (cancels 13, schedules 7: 6 messages + stop)
        int initialScheduledTimes = 13 + 7;

        processor.triggerRestartForCommand();
        verify(assertRestartScheduler(), times(initialScheduledTimes))
            .schedule(any(), anyLong());

        // Attempt a dynamic restart — should be blocked because MANUAL takes precedence
        processor.onServerTick(LOW_TICK_TIME_NANOS);
        advanceTimeBy(Duration.ofMinutes(DEFAULT_LOW_TPS_MINUTES));
        advanceTimeBy(Duration.ofMillis(1));
        processor.onServerTick(LOW_TICK_TIME_NANOS);

        // No additional tasks should have been scheduled
        verify(assertRestartScheduler(), times(initialScheduledTimes))
            .schedule(any(), anyLong());
    }

    @Test
    void dynamicRestartDoesNotRetriggerAfterAlreadyScheduled() {
        this.config = new TestConfigBuilder()
            .minMinutesBeforeAutoRestart(0)
            .shouldRestartForTps(true)
            .build();
        RestartProcessor processor = getRestartProcessor();

        int initialScheduledTimes = 13;

        // First dynamic trigger
        processor.onServerTick(LOW_TICK_TIME_NANOS);
        advanceTimeBy(Duration.ofMinutes(DEFAULT_LOW_TPS_MINUTES));
        advanceTimeBy(Duration.ofMillis(1));
        processor.onServerTick(LOW_TICK_TIME_NANOS);
        int afterFirstDynamic = initialScheduledTimes + 10;
        verify(assertRestartScheduler(), times(afterFirstDynamic)).schedule(any(), anyLong());

        // Attempt a second trigger — should be blocked because currentRestartType == DYNAMIC
        // (tpsTracker.reset() was called on the first trigger, so after another window of
        //  low ticks recordTick() returns true again, but triggerRestartForDynamic() returns early)
        processor.onServerTick(LOW_TICK_TIME_NANOS);
        advanceTimeBy(Duration.ofMinutes(DEFAULT_LOW_TPS_MINUTES));
        advanceTimeBy(Duration.ofMillis(1));
        processor.onServerTick(LOW_TICK_TIME_NANOS);

        // No additional tasks scheduled
        verify(assertRestartScheduler(), times(afterFirstDynamic)).schedule(any(), anyLong());
    }

    // -------------------------------------------------------------------------
    // min_delay_before_auto_restart interaction tests
    // -------------------------------------------------------------------------

    @Test
    void dynamicRestartIsBlockedAtExactMinDelayBoundary() {
        // isAfter() is strict — the min delay must be *strictly* exceeded before
        // hasBeenMinTimeBeforeRestart flips to true.
        this.config = new TestConfigBuilder()
            .minMinutesBeforeAutoRestart(MIN_DELAY_MINUTES)
            .shouldRestartForTps(true)
            .build();
        RestartProcessor processor = getRestartProcessor();

        int initialScheduledTimes = 13;

        // Low TPS from startup — the 1-minute TPS window elapses well before the min delay
        processor.onServerTick(LOW_TICK_TIME_NANOS);

        // Advance to exactly the min-delay instant (not past it).
        // hasBeenMinTimeBeforeRestart stays false because clock.instant().isAfter(minInstant)
        // is false when they are equal.
        advanceTimeBy(Duration.ofMinutes(MIN_DELAY_MINUTES));
        processor.onServerTick(LOW_TICK_TIME_NANOS);

        // TPS window has been exceeded for minutes, but min delay hasn't strictly elapsed
        verify(assertRestartScheduler(), times(initialScheduledTimes)).schedule(any(), anyLong());
    }

    @Test
    void dynamicRestartTriggersImmediatelyAfterMinDelayWhenTpsAlreadyLow() {
        // If TPS has been low since startup and the min delay then elapses, the very
        // next tick should trigger a dynamic restart — no additional waiting required.
        this.config = new TestConfigBuilder()
            .minMinutesBeforeAutoRestart(MIN_DELAY_MINUTES)
            .shouldRestartForTps(true)
            .build();
        RestartProcessor processor = getRestartProcessor();

        int initialScheduledTimes = 13;

        // Low TPS from the first tick — TPS window (1 min) elapses long before min delay (5 min)
        processor.onServerTick(LOW_TICK_TIME_NANOS);

        // Advance strictly past the min delay (needs 1 ms extra due to strict isAfter check)
        advanceTimeBy(Duration.ofMinutes(MIN_DELAY_MINUTES));
        advanceTimeBy(Duration.ofMillis(1));

        // Now hasBeenMinTimeBeforeRestart = true and needToRestart() was already true
        processor.onServerTick(LOW_TICK_TIME_NANOS);

        // Dynamic restart should have triggered (13 + 10 tasks scheduled)
        verify(assertRestartScheduler(), times(initialScheduledTimes + 10))
            .schedule(any(), anyLong());
    }

    @Test
    void dynamicRestartDoesNotTriggerBeforeMinDelayEvenWithSustainedLowTps() {
        // Even if TPS has been low for much longer than the TPS window, the min delay
        // must elapse before any auto-restart is allowed.
        this.config = new TestConfigBuilder()
            .minMinutesBeforeAutoRestart(MIN_DELAY_MINUTES)
            .shouldRestartForTps(true)
            .build();
        RestartProcessor processor = getRestartProcessor();

        int initialScheduledTimes = 13;

        // Low TPS from startup; advance to 1 minute before the min delay
        processor.onServerTick(LOW_TICK_TIME_NANOS);
        advanceTimeBy(Duration.ofMinutes(MIN_DELAY_MINUTES - 1));
        processor.onServerTick(LOW_TICK_TIME_NANOS);

        // Still inside the min-delay window — no dynamic restart
        verify(assertRestartScheduler(), times(initialScheduledTimes)).schedule(any(), anyLong());

        // The pre-existing scheduled restart fires normally once enough time passes
        advanceTimeBy(Duration.ofHours(10));
        verify(serverContext, times(1)).runCommand("stop");
    }

    @Test
    void dynamicRestartTriggersAfterMinDelayWhenTpsSubsequentlyBecomesLow() {
        // Realistic scenario: TPS is healthy during the min-delay window, then degrades.
        // Dynamic restart should trigger 1 minute after TPS first drops below threshold,
        // provided the min delay has already elapsed.
        this.config = new TestConfigBuilder()
            .minMinutesBeforeAutoRestart(MIN_DELAY_MINUTES)
            .shouldRestartForTps(true)
            .build();
        RestartProcessor processor = getRestartProcessor();

        int initialScheduledTimes = 13;

        // Healthy TPS throughout the min-delay period
        processor.onServerTick(NORMAL_TICK_TIME_NANOS);
        advanceTimeBy(Duration.ofMinutes(MIN_DELAY_MINUTES));
        advanceTimeBy(Duration.ofMillis(1));
        // This healthy tick updates lastTimeOfTpsAboveThreshold to "now"
        processor.onServerTick(NORMAL_TICK_TIME_NANOS);

        // Min delay is past, TPS was healthy — no dynamic restart yet
        verify(assertRestartScheduler(), times(initialScheduledTimes)).schedule(any(), anyLong());

        // TPS drops; the 1-minute TPS window starts from the last healthy tick above
        processor.onServerTick(LOW_TICK_TIME_NANOS);
        advanceTimeBy(Duration.ofMinutes(DEFAULT_LOW_TPS_MINUTES).plusMillis(1));
        processor.onServerTick(LOW_TICK_TIME_NANOS);

        // Dynamic restart triggered: 1-minute of sustained low TPS after min delay
        verify(assertRestartScheduler(), times(initialScheduledTimes + 10))
            .schedule(any(), anyLong());
    }

    @Test
    void noTriggerWhenTpsIsNormal() {
        this.config = new TestConfigBuilder()
            .minMinutesBeforeAutoRestart(0)
            .shouldRestartForTps(true)
            .build();
        RestartProcessor processor = getRestartProcessor();

        int initialScheduledTimes = 13;

        // Normal TPS for two minutes
        processor.onServerTick(NORMAL_TICK_TIME_NANOS);
        advanceTimeBy(Duration.ofMinutes(2));
        processor.onServerTick(NORMAL_TICK_TIME_NANOS);

        // The pre-existing scheduled restart must remain untouched
        verify(assertRestartScheduler(), times(initialScheduledTimes)).schedule(any(), anyLong());
        verify(serverContext, times(0)).runCommand(anyString());
    }
}
