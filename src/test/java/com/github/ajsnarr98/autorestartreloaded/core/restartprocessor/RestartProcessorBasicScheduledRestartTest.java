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

public class RestartProcessorBasicScheduledRestartTest extends BaseRestartProcessorTest {

    private void testScheduledRestartTimeForFixedMessages(
        Instant instant,
        String zone,
        List<String> restartSchedule,
        long[] expectedScheduledDelays,
        Runnable advanceTimeUntil1SecondBeforeFirstMessage
    ) {
        this.config = new TestConfigBuilder()
            .restartSchedule(restartSchedule)
            .rawTimezone(zone)
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
    void givenOneSimpleScheduledRestartTimeOnSameDayUTCRestartIsScheduledForSameDay() {
        testScheduledRestartTimeForFixedMessages(
            Instant.parse("2025-12-03T10:15:00.00Z"),
            "UTC+0",
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
    void givenOneSimpleScheduledRestartTimeOnSameDayESTRestartIsScheduledForSameDay() {
        testScheduledRestartTimeForFixedMessages(
            Instant.parse("2025-12-03T10:15:00.00-05:00"),
            "UTC-5", // EST
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
    void givenOneSimpleScheduledRestartTimeOnNextDayESTRestartIsScheduledForNextDay() {
        testScheduledRestartTimeForFixedMessages(
            Instant.parse("2025-12-03T04:10:00.00-05:00"),
            "UTC-5", // EST
            List.of("4:00"),
            new long[]{
                85_790_000L,
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
                advanceTimeBy(Duration.ofSeconds(49));
            }
        );
    }

    @Test
    void givenOneSimpleScheduledRestartTimeInMinimumSecondsRestartIsScheduledInMinimumSeconds() {
        testScheduledRestartTimeForFixedMessages(
            Instant.parse("2025-12-03T11:10:49.00-05:00"),
            "UTC-5", // EST
            List.of("11:11"),
            new long[]{
                1_000L,
                6_000L,
                7_000L,
                8_000L,
                9_000L,
                10_000L,
                11_000L,
            },
            () -> {
            }
        );
    }

    @Test
    void givenOneSimpleScheduledRestartTimeThatIsTooSoonRestartIsScheduledNextDay() {
        testScheduledRestartTimeForFixedMessages(
            Instant.parse("2025-12-03T11:10:55.00-05:00"),
            "UTC-5", // EST
            List.of("11:11"),
            new long[]{
                86_395_000L,
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
                advanceTimeBy(Duration.ofSeconds(54));
            }
        );
    }

    @Test
    void givenTwoSimpleScheduledRestartTimesWhereOneIsTooSoonRestartIsScheduledForSecondTime() {
        testScheduledRestartTimeForFixedMessages(
            Instant.parse("2025-12-03T11:10:55.00-05:00"),
            "UTC-5", // EST
            List.of("11:11", "11:30"),
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
    void givenTwoSimpleScheduledRestartTimesReversedWhereOneIsTooSoonRestartIsScheduledForSecondTime() {
        testScheduledRestartTimeForFixedMessages(
            Instant.parse("2025-12-03T11:10:55.00-05:00"),
            "UTC-5", // EST
            List.of("11:30", "11:11"),
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
    void givenOneCronScheduledRestartTimeOnSameDayUTCRestartIsScheduledForSameDay() {
        testScheduledRestartTimeForFixedMessages(
            Instant.parse("2025-12-03T10:15:00.00Z"),
            "UTC",
            List.of("0 13 * * *"),
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
    void givenOneCronScheduledRestartTimeOnSameDayESTRestartIsScheduledForSameDay() {
        testScheduledRestartTimeForFixedMessages(
            Instant.parse("2025-12-03T10:15:00.00-05:00"),
            "UTC-5", // EST
            List.of("0 13 * * *"),
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
    void givenOneCronScheduledRestartTimeOnNextDayESTRestartIsScheduledForNextDay() {
        testScheduledRestartTimeForFixedMessages(
            Instant.parse("2025-12-03T04:10:00.00-05:00"),
            "UTC-5", // EST
            List.of("0 4 * * *"),
            new long[]{
                85_790_000L,
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
                advanceTimeBy(Duration.ofSeconds(49));
            }
        );
    }

    @Test
    void givenOneCronScheduledRestartTimeInMinimumSecondsRestartIsScheduledInMinimumSeconds() {
        testScheduledRestartTimeForFixedMessages(
            Instant.parse("2025-12-03T11:10:49.00-05:00"),
            "UTC-5", // EST
            List.of("11 11 * * *"),
            new long[]{
                1_000L,
                6_000L,
                7_000L,
                8_000L,
                9_000L,
                10_000L,
                11_000L,
            },
            () -> {
            }
        );
    }

    @Test
    void givenOneCronScheduledRestartTimeThatIsTooSoonRestartIsScheduledNextDay() {
        testScheduledRestartTimeForFixedMessages(
            Instant.parse("2025-12-03T11:10:55.00-05:00"),
            "UTC-5", // EST
            List.of("11 11 * * *"),
            new long[]{
                86_395_000L,
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
                advanceTimeBy(Duration.ofSeconds(54));
            }
        );
    }

    @Test
    void givenTwoCronScheduledRestartTimesWhereOneIsTooSoonRestartIsScheduledForSecondTime() {
        testScheduledRestartTimeForFixedMessages(
            Instant.parse("2025-12-03T11:10:55.00-05:00"),
            "UTC-5", // EST
            List.of("11 11 * * *", "30 11 * * *"),
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
    void givenTwoCronScheduledRestartTimesReversedWhereOneIsTooSoonRestartIsScheduledForSecondTime() {
        testScheduledRestartTimeForFixedMessages(
            Instant.parse("2025-12-03T11:10:55.00-05:00"),
            "UTC-5", // EST
            List.of("30 11 * * *", "11 11 * * *"),
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
    void scheduledRestartWorksWithForwardTimeChange() {
        // expect 10 hrs 10 min because we skip an hr
        testScheduledRestartTimeForFixedMessages(
            Instant.parse("2025-03-09T01:50:00.00-05:00"),
            "America/New_York",
            List.of("13:00"),
            new long[]{
                36_590_000L,
                36_595_000L,
                36_596_000L,
                36_597_000L,
                36_598_000L,
                36_599_000L,
                36_600_000L // 10 hrs 10 min
            },
            () -> {
                advanceTimeBy(Duration.ofHours(10));
                advanceTimeBy(Duration.ofMinutes(9));
                advanceTimeBy(Duration.ofSeconds(49));
            }
        );
    }

    @Test
    void scheduledRestartWorksWithBackwardTimeChangeBeforeTime() {
        testScheduledRestartTimeForFixedMessages(
            Instant.parse("2025-11-02T01:20:00.00-04:00"),
            "America/New_York",
            List.of("1:30"),
            new long[]{
                590_000L,
                595_000L,
                596_000L,
                597_000L,
                598_000L,
                599_000L,
                600_000L // 10 min
            },
            () -> {
                advanceTimeBy(Duration.ofMinutes(9));
                advanceTimeBy(Duration.ofSeconds(49));
            }
        );
    }

    @Test
    void scheduledRestartWorksWithBackwardTimeChangeAfterTime() {
        testScheduledRestartTimeForFixedMessages(
            Instant.parse("2025-11-02T01:35:00.00-04:00"),
            "America/New_York",
            List.of("1:30"),
            new long[]{
                3_290_000L,
                3_295_000L,
                3_296_000L,
                3_297_000L,
                3_298_000L,
                3_299_000L,
                3_300_000L // 55 min
            },
            () -> {
                advanceTimeBy(Duration.ofMinutes(54));
                advanceTimeBy(Duration.ofSeconds(49));
            }
        );
    }

    @Test
    void scheduledRestartIgnoredWhenRestartListEmpty() {
        this.config = new TestConfigBuilder()
            .restartSchedule(List.of())
            .rawTimezone("UTC-5")
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
        setTime(Instant.parse("2025-12-03T11:10:49.00-05:00"));
        RestartProcessor restartProcessor = getRestartProcessor();
        advanceTimeBy(Duration.ofDays(1));

        verify(schedulerFactory.schedulers.getFirst(), times(0))
            .schedule(any(), anyLong());

        verify(serverContext, times(0)).runCommand(anyString());
    }

    @Test
    void scheduledRestartRightBeforeTimeStillWorksWhenNoMessagesGiven() {
        this.config = new TestConfigBuilder()
            .restartSchedule(
                List.of("13:00")
            )
            .rawTimezone("UTC-5")
            .scheduledRestartMessages(List.of())
            .build();
        setTime(Instant.parse("2025-12-03T12:59:59.00-05:00"));
        RestartProcessor restartProcessor = getRestartProcessor();

        verify(schedulerFactory.schedulers.getFirst(), times(1))
            .schedule(any(), anyLong());

        verify(serverContext, times(0)).runCommand(anyString());

        advanceTimeBy(Duration.ofSeconds(1));

        verify(serverContext, times(1)).runCommand(anyString());
        verify(serverContext, times(1)).runCommand(
            "stop"
        );
    }

    @Test
    void scheduledRestartRightAfterTimeStillWorksWhenNoMessagesGiven() {
        this.config = new TestConfigBuilder()
            .restartSchedule(
                List.of("13:00")
            )
            .rawTimezone("UTC-5")
            .scheduledRestartMessages(List.of())
            .build();
        setTime(Instant.parse("2025-12-03T13:00:00.00-05:00"));
        RestartProcessor restartProcessor = getRestartProcessor();

        verify(schedulerFactory.schedulers.getFirst(), times(1))
            .schedule(any(), anyLong());

        verify(serverContext, times(0)).runCommand(anyString());

        advanceTimeBy(Duration.ofHours(23));
        advanceTimeBy(Duration.ofMinutes(59));
        advanceTimeBy(Duration.ofSeconds(59));

        verify(serverContext, times(0)).runCommand(anyString());

        advanceTimeBy(Duration.ofSeconds(1));

        verify(serverContext, times(1)).runCommand(anyString());
        verify(serverContext, times(1)).runCommand(
            "stop"
        );
    }
}
