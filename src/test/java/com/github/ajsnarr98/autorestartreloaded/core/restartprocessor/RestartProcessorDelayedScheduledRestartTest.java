package com.github.ajsnarr98.autorestartreloaded.core.restartprocessor;

import com.github.ajsnarr98.autorestartreloaded.core.BaseRestartProcessorTest;
import com.github.ajsnarr98.autorestartreloaded.core.RestartProcessor;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class RestartProcessorDelayedScheduledRestartTest extends BaseRestartProcessorTest {

    private void testDelayedScheduledRestartTimeForFixedMessages(
        Instant instant,
        String zone,
        int minMinutesBeforeRestart,
        List<String> restartSchedule,
        long[] expectedScheduledDelays,
        Runnable advanceTimeUntil1SecondBeforeFirstMessage
    ) {
        this.config = new TestConfigBuilder()
            .restartSchedule(restartSchedule)
            .rawTimezone(zone)
            .minMinutesBeforeAutoRestart(minMinutesBeforeRestart)
            .scheduledRestartMessages(
                List.of(
                    "10: Server restarting in 10 seconds for scheduled time...",
                    "5: Restarting in 5 seconds...",
                    "4: Restarting in 4 seconds...",
                    "3: Restarting in 3 seconds...",
                    "2: Restarting in 2 seconds...",
                    "1: Restarting in 1 second..."
                )
            )
            .build();
        setTime(instant);
        RestartProcessor restartProcessor = getRestartProcessor();

        verify(assertRestartScheduler(), times(expectedScheduledDelays.length))
            .schedule(any(), anyLong());
        for (long delay : expectedScheduledDelays) {
            verify(assertRestartScheduler()).schedule(any(), eq(delay));
        }

        advanceTimeUntil1SecondBeforeFirstMessage.run();

        verify(serverContext, times(0)).runCommand(anyString());

        advanceTimeBy(Duration.ofSeconds(1));
        verify(serverContext, times(1)).runCommand(anyString());
        verify(serverContext, times(1)).runCommand(
            "tellraw @a {\"text\":\"Server restarting in 10 seconds for scheduled time...\",\"color\":\"yellow\"}"
        );

        advanceTimeBy(Duration.ofSeconds(5));
        verify(serverContext, times(2)).runCommand(anyString());
        verify(serverContext, times(1)).runCommand(
            "tellraw @a {\"text\":\"Restarting in 5 seconds...\",\"color\":\"yellow\"}"
        );

        advanceTimeBy(Duration.ofSeconds(1));
        verify(serverContext, times(3)).runCommand(anyString());
        verify(serverContext, times(1)).runCommand(
            "tellraw @a {\"text\":\"Restarting in 4 seconds...\",\"color\":\"yellow\"}"
        );

        advanceTimeBy(Duration.ofSeconds(1));
        verify(serverContext, times(4)).runCommand(anyString());
        verify(serverContext, times(1)).runCommand(
            "tellraw @a {\"text\":\"Restarting in 3 seconds...\",\"color\":\"yellow\"}"
        );

        advanceTimeBy(Duration.ofSeconds(1));
        verify(serverContext, times(5)).runCommand(anyString());
        verify(serverContext, times(1)).runCommand(
            "tellraw @a {\"text\":\"Restarting in 2 seconds...\",\"color\":\"yellow\"}"
        );

        advanceTimeBy(Duration.ofSeconds(1));
        verify(serverContext, times(6)).runCommand(anyString());
        verify(serverContext, times(1)).runCommand(
            "tellraw @a {\"text\":\"Restarting in 1 second...\",\"color\":\"yellow\"}"
        );

        advanceTimeBy(Duration.ofSeconds(1));
        verify(serverContext, times(7)).runCommand(anyString());
        verify(serverContext, times(1)).runCommand(
            "stop"
        );
    }

    @Test
    void givenOneSimpleScheduledRestartTimeOutsideMinDelayOnSameDayUTCRestartIsScheduledForSameDay() {
        testDelayedScheduledRestartTimeForFixedMessages(
            Instant.parse("2025-12-03T10:15:00.00Z"),
            "UTC+0",
            164,
            List.of("13:00"),
            new long[]{
                9_890_000L,
                9_895_000L,
                9_896_000L,
                9_897_000L,
                9_898_000L,
                9_899_000L,
                9_900_000L
            },
            () -> {
                advanceTimeBy(Duration.ofHours(2));
                advanceTimeBy(Duration.ofMinutes(44));
                advanceTimeBy(Duration.ofSeconds(49));
            }
        );
    }

    @Test
    void givenOneSimpleScheduledRestartTimeOutsideMinDelayOnSameDayESTRestartIsScheduledForSameDay() {
        testDelayedScheduledRestartTimeForFixedMessages(
            Instant.parse("2025-12-03T10:15:00.00-05:00"),
            "UTC-5", // EST
            164,
            List.of("13:00"),
            new long[]{
                9_890_000L,
                9_895_000L,
                9_896_000L,
                9_897_000L,
                9_898_000L,
                9_899_000L,
                9_900_000L
            },
            () -> {
                advanceTimeBy(Duration.ofHours(2));
                advanceTimeBy(Duration.ofMinutes(44));
                advanceTimeBy(Duration.ofSeconds(49));
            }
        );
    }

    @Test
    void givenOneSimpleScheduledRestartTimeOutsideMinDelayOnSameDayUTCPlus5RestartIsScheduledForSameDay() {
        testDelayedScheduledRestartTimeForFixedMessages(
            Instant.parse("2025-12-03T10:15:00.00+05:00"),
            "UTC+5",
            164,
            List.of("13:00"),
            new long[]{
                9_890_000L,
                9_895_000L,
                9_896_000L,
                9_897_000L,
                9_898_000L,
                9_899_000L,
                9_900_000L
            },
            () -> {
                advanceTimeBy(Duration.ofHours(2));
                advanceTimeBy(Duration.ofMinutes(44));
                advanceTimeBy(Duration.ofSeconds(49));
            }
        );
    }

    @Test
    void givenOneSimpleScheduledRestartTimeWithinMinDelayOnSameDayUTCRestartIsScheduledForNextDay() {
        testDelayedScheduledRestartTimeForFixedMessages(
            Instant.parse("2025-12-03T10:15:00.00Z"),
            "UTC+0",
            166,
            List.of("13:00"),
            new long[]{
                96_290_000L,
                96_295_000L,
                96_296_000L,
                96_297_000L,
                96_298_000L,
                96_299_000L,
                96_300_000L
            },
            () -> {
                advanceTimeBy(Duration.ofHours(26));
                advanceTimeBy(Duration.ofMinutes(44));
                advanceTimeBy(Duration.ofSeconds(49));
            }
        );
    }

    @Test
    void givenOneSimpleScheduledRestartTimeWithinMinDelayOnSameDayESTRestartIsScheduledForNextDay() {
        testDelayedScheduledRestartTimeForFixedMessages(
            Instant.parse("2025-12-03T10:15:00.00-05:00"),
            "UTC-5",
            166,
            List.of("13:00"),
            new long[]{
                96_290_000L,
                96_295_000L,
                96_296_000L,
                96_297_000L,
                96_298_000L,
                96_299_000L,
                96_300_000L
            },
            () -> {
                advanceTimeBy(Duration.ofHours(26));
                advanceTimeBy(Duration.ofMinutes(44));
                advanceTimeBy(Duration.ofSeconds(49));
            }
        );
    }

    @Test
    void givenOneSimpleScheduledRestartTimeWithinMinDelayOnSameDayUTCPlus5RestartIsScheduledForNextDay() {
        testDelayedScheduledRestartTimeForFixedMessages(
            Instant.parse("2025-12-03T10:15:00.00+05:00"),
            "UTC+5",
            166,
            List.of("13:00"),
            new long[]{
                96_290_000L,
                96_295_000L,
                96_296_000L,
                96_297_000L,
                96_298_000L,
                96_299_000L,
                96_300_000L
            },
            () -> {
                advanceTimeBy(Duration.ofHours(26));
                advanceTimeBy(Duration.ofMinutes(44));
                advanceTimeBy(Duration.ofSeconds(49));
            }
        );
    }


    @Test
    void givenTwoSimpleScheduledRestartTimesWithMinDelayWhereOneIsTooSoonInDelayRestartIsScheduledForSecondTime() {
        testDelayedScheduledRestartTimeForFixedMessages(
            Instant.parse("2025-12-03T11:10:55.00-05:00"),
            "UTC-5", // EST
            19,
            List.of("11:29", "11:30"),
            new long[]{
                1_135_000L,
                1_140_000L,
                1_141_000L,
                1_142_000L,
                1_143_000L,
                1_144_000L,
                1_145_000L
            },
            () -> {
                advanceTimeBy(Duration.ofMinutes(18));
                advanceTimeBy(Duration.ofSeconds(54));
            }
        );
    }

    @Test
    void givenTwoSimpleScheduledRestartTimesReversedWithMinDelayWhereOneIsTooSoonInDelayRestartIsScheduledForSecondTime() {
        testDelayedScheduledRestartTimeForFixedMessages(
            Instant.parse("2025-12-03T11:10:55.00-05:00"),
            "UTC-5", // EST
            19,
            List.of("11:30", "11:29"),
            new long[]{
                1_135_000L,
                1_140_000L,
                1_141_000L,
                1_142_000L,
                1_143_000L,
                1_144_000L,
                1_145_000L
            },
            () -> {
                advanceTimeBy(Duration.ofMinutes(18));
                advanceTimeBy(Duration.ofSeconds(54));
            }
        );
    }
}
