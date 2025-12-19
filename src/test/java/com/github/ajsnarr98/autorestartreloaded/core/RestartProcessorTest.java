package com.github.ajsnarr98.autorestartreloaded.core;

import com.github.ajsnarr98.autorestartreloaded.AutoRestartReloaded;
import com.github.ajsnarr98.autorestartreloaded.core.servercontext.ServerContext;
import com.github.ajsnarr98.autorestartreloaded.core.task.DefaultTaskProvider;
import com.github.ajsnarr98.autorestartreloaded.core.task.QueuedTaskProvider;
import com.github.ajsnarr98.autorestartreloaded.core.task.executer.TestSchedulerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class RestartProcessorTest {

    static {
        AutoRestartReloaded.LOGGER = mock();
    }

    QueuedTaskProvider taskProvider;
    ServerContext serverContext;
    Config config;
    TestClock clock;
    TestSchedulerFactory schedulerFactory;

    @BeforeEach
    public void setup() {
        this.taskProvider = new DefaultTaskProvider();
        this.config = new Config(
            List.of("13:00")
        );
        this.clock = new TestClock(
            Instant.parse("2025-12-03T10:15:30.00Z"),
            ZoneId.of("America/New_York")
        );
        this.schedulerFactory = new TestSchedulerFactory(this.clock);
        this.serverContext = mock();
    }

    private RestartProcessor getRestartProcessor() {
        return new RestartProcessorImpl(
            taskProvider,
            serverContext,
            config,
            clock,
            schedulerFactory
        );
    }

    private void advanceTimeBy(Duration duration) {
        this.clock.advanceTimeBy(duration);
    }

    @Test
    void triggeringRestartForCommandSendsMessagesAsSoonAsPossibleFor5SecMessages() {
        RestartProcessor restartProcessor = getRestartProcessor();
        restartProcessor.triggerRestartForCommand();

        assertThat(schedulerFactory.schedulers.size())
            .as("After command trigger")
            .isEqualTo(1);

        verify(serverContext, times(0)).runCommand(anyString());
        verify(schedulerFactory.schedulers.getFirst()).schedule(any(), eq(1000L));
        verify(schedulerFactory.schedulers.getFirst()).schedule(any(), eq(2000L));
        verify(schedulerFactory.schedulers.getFirst()).schedule(any(), eq(3000L));
        verify(schedulerFactory.schedulers.getFirst()).schedule(any(), eq(4000L));
        verify(schedulerFactory.schedulers.getFirst()).schedule(any(), eq(5000L));
        verify(schedulerFactory.schedulers.getFirst()).schedule(any(), eq(6000L));
        verify(schedulerFactory.schedulers.getFirst(), times(0)).schedule(any(), eq(7000L));

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
}
