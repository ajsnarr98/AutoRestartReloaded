package com.github.ajsnarr98.autorestartreloaded.core.restartprocessor;

import com.github.ajsnarr98.autorestartreloaded.core.BaseRestartProcessorTest;
import com.github.ajsnarr98.autorestartreloaded.core.Config;
import com.github.ajsnarr98.autorestartreloaded.core.RestartProcessor;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class RestartProcessorScheduledRestartTest extends BaseRestartProcessorTest {

    private void testScheduledRestartTime(
        Instant instant,
        ZoneId zone,
        List<String> restartSchedule,
        long[] expectedScheduledDelays,
        Runnable advanceTimeUntil1SecondBeforeFirstMessage
    ) {
        setTime(instant, zone);
        this.config = new Config(restartSchedule);
        RestartProcessor restartProcessor = getRestartProcessor();

        verify(schedulerFactory.schedulers.getFirst(), times(expectedScheduledDelays.length))
            .schedule(any(), anyLong());
        for (long delay : expectedScheduledDelays) {
            verify(schedulerFactory.schedulers.getFirst()).schedule(any(), eq(delay));
        }

        advanceTimeUntil1SecondBeforeFirstMessage.run();

        verify(serverContext, times(0)).runCommand(anyString());

        advanceTimeBy(Duration.ofSeconds(1));
        verify(serverContext, times(1)).runCommand(anyString());
        verify(serverContext, times(1)).runCommand(
            "tellraw @a {\"text\":\" Restarting in 5 seconds...\",\"color\":\"yellow\"}"
        );

        advanceTimeBy(Duration.ofSeconds(1));
        verify(serverContext, times(2)).runCommand(anyString());
        verify(serverContext, times(1)).runCommand(
            "tellraw @a {\"text\":\" Restarting in 4 seconds...\",\"color\":\"yellow\"}"
        );

        advanceTimeBy(Duration.ofSeconds(1));
        verify(serverContext, times(3)).runCommand(anyString());
        verify(serverContext, times(1)).runCommand(
            "tellraw @a {\"text\":\" Restarting in 3 seconds...\",\"color\":\"yellow\"}"
        );

        advanceTimeBy(Duration.ofSeconds(1));
        verify(serverContext, times(4)).runCommand(anyString());
        verify(serverContext, times(1)).runCommand(
            "tellraw @a {\"text\":\" Restarting in 2 seconds...\",\"color\":\"yellow\"}"
        );

        advanceTimeBy(Duration.ofSeconds(1));
        verify(serverContext, times(5)).runCommand(anyString());
        verify(serverContext, times(1)).runCommand(
            "tellraw @a {\"text\":\" Restarting in 1 second...\",\"color\":\"yellow\"}"
        );

        advanceTimeBy(Duration.ofSeconds(1));
        verify(serverContext, times(6)).runCommand(anyString());
        verify(serverContext, times(1)).runCommand(
            "stop"
        );
    }

    @Test
    void givenOneSimpleScheduledRestartTimeOnSameDayUTCRestartIsScheduledForSameDay() {
        testScheduledRestartTime(
            Instant.parse("2025-12-03T10:15:00.00Z"),
            ZoneOffset.UTC,
            List.of("13:00"),
            new long[]{
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
                advanceTimeBy(Duration.ofSeconds(54));
            }
        );
    }

    @Test
    void givenOneSimpleScheduledRestartTimeOnSameDayESTRestartIsScheduledForSameDay() {
        testScheduledRestartTime(
            Instant.parse("2025-12-03T10:15:00.00Z"),
            ZoneOffset.ofHours(-5), // EST
            List.of("13:00"),
            new long[]{
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
                advanceTimeBy(Duration.ofSeconds(54));
            }
        );
    }

    @Test
    void givenOneSimpleScheduledRestartTimeOnNextDayESTRestartIsScheduledForNextDay() {
        testScheduledRestartTime(
            Instant.parse("2025-12-03T04:10:00.00Z"),
            ZoneOffset.ofHours(-5), // EST
            List.of("4:00"),
            new long[]{
                85_795_000L,
                85_796_000L,
                85_797_000L,
                85_798_000L,
                85_799_000L,
                85_800_000L
            },
            () -> {
                advanceTimeBy(Duration.ofHours(23));
                advanceTimeBy(Duration.ofMinutes(49));
                advanceTimeBy(Duration.ofSeconds(54));
            }
        );
    }

    @Test
    void givenOneSimpleScheduledRestartTimeInMinimumSecondsRestartIsScheduledInMinimumSeconds() {
        testScheduledRestartTime(
            Instant.parse("2025-12-03T11:10:54.00Z"),
            ZoneOffset.ofHours(-5), // EST
            List.of("11:11"),
            new long[]{
                1_000L,
                2_000L,
                3_000L,
                4_000L,
                5_000L,
                6_000L
            },
            () -> {}
        );
    }

    @Test
    void givenOneSimpleScheduledRestartTimeThatIsTooSoonRestartIsScheduledNextDay() {
        testScheduledRestartTime(
            Instant.parse("2025-12-03T11:10:55.00Z"),
            ZoneOffset.ofHours(-5), // EST
            List.of("11:11"),
            new long[]{
                86_400_000L,
                86_401_000L,
                86_402_000L,
                86_403_000L,
                86_404_000L,
                86_405_000L
            },
            () -> {
                advanceTimeBy(Duration.ofHours(23));
                advanceTimeBy(Duration.ofMinutes(59));
                advanceTimeBy(Duration.ofSeconds(59));
            }
        );
    }

    @Test
    void givenTwoSimpleScheduledRestartTimesWhereOneIsTooSoonRestartIsScheduledForSecondTime() {
        testScheduledRestartTime(
            Instant.parse("2025-12-03T11:10:55.00Z"),
            ZoneOffset.ofHours(-5), // EST
            List.of("11:11", "11:30"),
            new long[]{
                1_140_000L,
                1_141_000L,
                1_142_000L,
                1_143_000L,
                1_144_000L,
                1_145_000L
            },
            () -> {
                advanceTimeBy(Duration.ofMinutes(18));
                advanceTimeBy(Duration.ofSeconds(59));
            }
        );
    }
}
