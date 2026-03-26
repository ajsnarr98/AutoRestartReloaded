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

//    /**
//     * Sends a low-TPS tick, advances time just past the 1-minute window
//     * (lowTpsMinMinutes = 1 in TestConfigBuilder), then sends another
//     * low-TPS tick — which should fire the dynamic-restart trigger.
//     */
//    private void triggerDynamicRestart(
//        RestartProcessor processor
//    ) {
//        processor.onServerTick(LOW_TICK_TIME_NANOS);
//        advanceTimeBy(Duration.ofMillis(60_001));
//        processor.onServerTick(NORMAL_TICK_TIME_NANOS);
//    }

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

//    @Test
//    void dynamicRestartDoesNotRetriggerAfterAlreadyScheduled() {
//        this.config = new TestConfigBuilder().shouldRestartForTps(true).build();
//        RestartProcessor processor = getRestartProcessor();
//
//        int initialScheduledTimes = 13;
//
//        // First dynamic trigger
//        triggerDynamicRestart(processor);
//        int afterFirstDynamic = initialScheduledTimes + 10;
//        verify(assertRestartScheduler(), times(afterFirstDynamic)).schedule(any(), anyLong());
//
//        // Attempt a second trigger — should be blocked because currentRestartType == DYNAMIC
//        // (tpsTracker.reset() was called on the first trigger, so after another window of
//        //  low ticks recordTick() returns true again, but triggerRestartForDynamic() returns early)
//        triggerDynamicRestart(processor);
//
//        // No additional tasks scheduled
//        verify(assertRestartScheduler(), times(afterFirstDynamic)).schedule(any(), anyLong());
//    }
//
//    @Test
//    void noTriggerWhenTpsIsNormal() {
//        this.config = new TestConfigBuilder().shouldRestartForTps(true).build();
//        RestartProcessor processor = getRestartProcessor();
//
//        int initialScheduledTimes = 13;
//
//        // Normal TPS for two minutes
//        processor.onServerTick(NORMAL_TICK_TIME_MS);
//        advanceTimeBy(Duration.ofMinutes(2));
//        processor.onServerTick(NORMAL_TICK_TIME_MS);
//
//        // The pre-existing scheduled restart must remain untouched
//        verify(assertRestartScheduler(), times(initialScheduledTimes)).schedule(any(), anyLong());
//        verify(serverContext, times(0)).runCommand(anyString());
//    }
}
