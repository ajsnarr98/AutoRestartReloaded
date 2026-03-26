package com.github.ajsnarr98.autorestartreloaded.core.restartprocessor;

import com.github.ajsnarr98.autorestartreloaded.core.BaseRestartProcessorTest;
import com.github.ajsnarr98.autorestartreloaded.core.Config;
import com.github.ajsnarr98.autorestartreloaded.core.RestartProcessor;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class RestartProcessorConfigUpdateTest extends BaseRestartProcessorTest {

    // -------------------------------------------------------------------------
    // Same-clock-instance tests
    //
    // TestClock.withZone() mutates the zone in place and returns the same object,
    // so even after onConfigUpdated changes the timezone the processor's internal
    // clock is still the TestClock controlled by advanceTimeBy(). Each test below
    // implicitly asserts this: if onConfigUpdated swapped in an independent clock,
    // advanceTimeBy() would no longer drive the processor's scheduled tasks and the
    // verify(serverContext) assertions would fail.
    // -------------------------------------------------------------------------

    @Test
    void scheduledTasksStillFireAfterConfigUpdateConfirmingSameClockIsUsed() {
        // Updating the config must not cause the processor to adopt an independent
        // clock that is no longer driven by the test's advanceTimeBy() calls.
        RestartProcessor processor = getRestartProcessor();

        processor.onConfigUpdated(new TestConfigBuilder().build());

        // Advance past the (re-)scheduled restart at 13:00 UTC
        // (clock start = 2025-12-03T10:15:30Z  →  9 870 s to 13:00:00Z)
        advanceTimeBy(Duration.ofSeconds(9_871));

        verify(serverContext, times(1)).runCommand("stop");
    }

    // -------------------------------------------------------------------------
    // Timezone change tests
    // -------------------------------------------------------------------------

    @Test
    void onConfigUpdatedWithNewTimezoneReschedulesRestart() {
        // Default: UTC timezone, restart at 13:00 UTC.
        // Clock at 2025-12-03T10:15:30Z → first restart in 9 870 s.
        RestartProcessor processor = getRestartProcessor();

        int initialScheduledTimes = 13;
        verify(assertRestartScheduler(), times(initialScheduledTimes)).schedule(any(), anyLong());

        // Update to UTC-8: restart at 13:00 UTC-8 = 21:00 UTC.
        // 2025-12-03T21:00:00Z is 38 670 s from start.
        Config updatedConfig = new TestConfigBuilder()
            .rawTimezone("UTC-8")
            .build();
        processor.onConfigUpdated(updatedConfig);

        // Old 13 tasks cancelled; 13 new tasks scheduled for the UTC-8 restart time.
        verify(assertRestartScheduler(), times(initialScheduledTimes * 2)).schedule(any(), anyLong());

        // The stop task is now scheduled 38 670 000 ms from "now".
        verify(assertRestartScheduler()).schedule(any(), eq(38_670_000L));

        // Advance past the OLD restart time (13:00 UTC = 9 870 s) — old tasks were
        // cancelled so nothing fires.
        advanceTimeBy(Duration.ofSeconds(9_871));
        verify(serverContext, times(0)).runCommand(anyString());

        // Advance to the NEW restart time (21:00 UTC = 38 670 s from start).
        advanceTimeBy(Duration.ofSeconds(38_670 - 9_871 + 1));
        verify(serverContext, times(1)).runCommand("stop");
    }

    @Test
    void onConfigUpdatedWithEarlierTimezoneReschedulesRestart() {
        // Symmetrical case: shifting the effective restart time earlier than the
        // original schedule.
        //
        // Default: UTC, restart at 13:00 UTC → 9 870 s from start.
        // Update to UTC+5: restart at 13:00 UTC+5 = 08:00 UTC.
        // 08:00 UTC today (2025-12-03) has already passed, so the next occurrence is
        // 2025-12-04T08:00:00Z = 21h 44m 30s = 78 270 s from start.
        RestartProcessor processor = getRestartProcessor();

        int initialScheduledTimes = 13;
        verify(assertRestartScheduler(), times(initialScheduledTimes)).schedule(any(), anyLong());

        Config updatedConfig = new TestConfigBuilder()
            .rawTimezone("UTC+5")
            .build();
        processor.onConfigUpdated(updatedConfig);

        verify(assertRestartScheduler(), times(initialScheduledTimes * 2)).schedule(any(), anyLong());
        verify(assertRestartScheduler()).schedule(any(), eq(78_270_000L));

        // The old restart (13:00 UTC = 9 870 s) was cancelled — nothing fires early.
        advanceTimeBy(Duration.ofSeconds(9_871));
        verify(serverContext, times(0)).runCommand(anyString());

        // Advance to the new restart (08:00 UTC next day = 78 270 s).
        advanceTimeBy(Duration.ofSeconds(78_270 - 9_871 + 1));
        verify(serverContext, times(1)).runCommand("stop");
    }

    // -------------------------------------------------------------------------
    // General schedule-change tests (non-timezone)
    // -------------------------------------------------------------------------

    @Test
    void onConfigUpdatedWithNewRestartTimeReschedulesRestart() {
        // Verify that a schedule change (not a timezone change) also results in the
        // restart being rescheduled for the new time.
        //
        // Initial: 13:00 UTC → 9 870 s from start.
        // Updated: 15:00 UTC → 17 070 s from start.
        RestartProcessor processor = getRestartProcessor();

        int initialScheduledTimes = 13;
        verify(assertRestartScheduler(), times(initialScheduledTimes)).schedule(any(), anyLong());

        Config updatedConfig = new TestConfigBuilder()
            .restartSchedule(List.of("15:00"))
            .build();
        processor.onConfigUpdated(updatedConfig);

        verify(assertRestartScheduler(), times(initialScheduledTimes * 2)).schedule(any(), anyLong());
        verify(assertRestartScheduler()).schedule(any(), eq(17_070_000L));

        // Old 13:00 restart was cancelled.
        advanceTimeBy(Duration.ofSeconds(9_871));
        verify(serverContext, times(0)).runCommand("stop");

        // New 15:00 restart fires.
        advanceTimeBy(Duration.ofSeconds(17_070 - 9_871 + 1));
        verify(serverContext, times(1)).runCommand("stop");
    }
}
