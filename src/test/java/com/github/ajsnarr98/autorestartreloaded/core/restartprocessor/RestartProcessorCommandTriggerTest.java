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

public class RestartProcessorCommandTriggerTest extends BaseRestartProcessorTest {

    @Test
    void triggeringRestartForCommandSendsMessagesAsSoonAsPossibleFor5SecMessages() {
        RestartProcessor restartProcessor = getRestartProcessor();

        int initialScheduledTimes = 13;

        assertThat(schedulerFactory.schedulers.size())
            .as("Before command trigger")
            .isEqualTo(1);
        verify(schedulerFactory.schedulers.getFirst(), times(initialScheduledTimes)).schedule(any(), anyLong());

        restartProcessor.triggerRestartForCommand();

        assertThat(schedulerFactory.schedulers.size())
            .as("After command trigger")
            .isEqualTo(1);

        verify(serverContext, times(0)).runCommand(anyString());
        // expect 6 more scheduled
        verify(schedulerFactory.schedulers.getFirst(), times(initialScheduledTimes + 7))
            .schedule(any(), anyLong());
        verify(schedulerFactory.schedulers.getFirst()).schedule(any(), eq(1_000L));
        verify(schedulerFactory.schedulers.getFirst()).schedule(any(), eq(6_000L));
        verify(schedulerFactory.schedulers.getFirst()).schedule(any(), eq(7_000L));
        verify(schedulerFactory.schedulers.getFirst()).schedule(any(), eq(8_000L));
        verify(schedulerFactory.schedulers.getFirst()).schedule(any(), eq(9_000L));
        verify(schedulerFactory.schedulers.getFirst()).schedule(any(), eq(10_000L));
        verify(schedulerFactory.schedulers.getFirst()).schedule(any(), eq(11_000L));
        verify(schedulerFactory.schedulers.getFirst(), times(0)).schedule(any(), eq(12000L));

        verify(serverContext, times(0)).runCommand(anyString());

        advanceTimeBy(Duration.ofSeconds(1));
        verify(serverContext, times(1)).runCommand(anyString());
        verify(serverContext, times(1)).runCommand(
            "tellraw @a {\"text\":\"Server restarting in 10 seconds from /restart command...\",\"color\":\"yellow\"}"
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
    void triggeringRestartForCommandCancelsPreviouslyScheduledRestartForCommand() {
        RestartProcessor restartProcessor = getRestartProcessor();

        int initialScheduledTimes = 13;
        verify(schedulerFactory.schedulers.getFirst(), times(initialScheduledTimes)).schedule(any(), anyLong());

        restartProcessor.triggerRestartForCommand();

        verify(serverContext, times(0)).runCommand(anyString());
        // expect 6 more scheduled
        verify(schedulerFactory.schedulers.getFirst(), times(initialScheduledTimes + 7))
            .schedule(any(), anyLong());

        advanceTimeBy(Duration.ofSeconds(9));

        // some commands have already run, but not "stop"
        verify(serverContext, times(5)).runCommand(anyString());
        verify(serverContext, times(0)).runCommand("stop");

        restartProcessor.triggerRestartForCommand();

        // verify new restart time is scheduled
        verify(schedulerFactory.schedulers.getFirst(), times(initialScheduledTimes + 14))
            .schedule(any(), anyLong());

        // verify second restart
        verify(serverContext, times(5)).runCommand(anyString());

        advanceTimeBy(Duration.ofSeconds(1));
        verify(serverContext, times(6)).runCommand(anyString());
        verify(serverContext, times(2)).runCommand(
            "tellraw @a {\"text\":\"Server restarting in 10 seconds from /restart command...\",\"color\":\"yellow\"}"
        );

        advanceTimeBy(Duration.ofSeconds(5));
        verify(serverContext, times(7)).runCommand(anyString());
        verify(serverContext, times(2)).runCommand(
            "tellraw @a {\"text\":\"Restarting in 5 seconds...\",\"color\":\"yellow\"}"
        );

        advanceTimeBy(Duration.ofSeconds(1));
        verify(serverContext, times(8)).runCommand(anyString());
        verify(serverContext, times(2)).runCommand(
            "tellraw @a {\"text\":\"Restarting in 4 seconds...\",\"color\":\"yellow\"}"
        );

        advanceTimeBy(Duration.ofSeconds(1));
        verify(serverContext, times(9)).runCommand(anyString());
        verify(serverContext, times(2)).runCommand(
            "tellraw @a {\"text\":\"Restarting in 3 seconds...\",\"color\":\"yellow\"}"
        );

        advanceTimeBy(Duration.ofSeconds(1));
        verify(serverContext, times(10)).runCommand(anyString());
        verify(serverContext, times(2)).runCommand(
            "tellraw @a {\"text\":\"Restarting in 2 seconds...\",\"color\":\"yellow\"}"
        );

        advanceTimeBy(Duration.ofSeconds(1));
        verify(serverContext, times(11)).runCommand(anyString());
        verify(serverContext, times(1)).runCommand(
            "tellraw @a {\"text\":\"Restarting in 1 second...\",\"color\":\"yellow\"}"
        );

        advanceTimeBy(Duration.ofSeconds(1));
        verify(serverContext, times(12)).runCommand(anyString());
        verify(serverContext, times(1)).runCommand(
            "stop"
        );
    }
}
