package com.github.ajsnarr98.autorestartreloaded.core;

import com.github.ajsnarr98.autorestartreloaded.core.servercontext.ServerContext;
import com.github.ajsnarr98.autorestartreloaded.core.task.DefaultTaskProvider;
import com.github.ajsnarr98.autorestartreloaded.core.task.QueuedTaskProvider;
import com.github.ajsnarr98.autorestartreloaded.core.task.executer.TestSchedulerFactory;
import org.junit.jupiter.api.BeforeEach;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.Mockito.mock;

public class BaseRestartProcessorTest {

    protected QueuedTaskProvider taskProvider;
    protected ServerContext serverContext;
    protected Config config;
    protected TestClock clock;
    protected TestSchedulerFactory schedulerFactory;

    protected List<? extends String> sampleScheduleRestartMessages = List.of(
        "1800: Server restarting in 30 minutes for scheduled restart",
        "600: Server restarting in 10 minutes for scheduled restart",
        "300: Server restarting in 5 minutes",
        "60: Server restarting in 1 minute",
        "30: Restarting in 30 seconds...",
        "15: Restarting in 15 seconds...",
        "10: Restarting in 10 seconds...",
        "5: Restarting in 5 seconds...",
        "4: Restarting in 4 seconds...",
        "3: Restarting in 3 seconds...",
        "2: Restarting in 2 seconds...",
        "1: Restarting in 1 second..."
    );

    protected List<? extends String> sampleCommandRestartMessages = List.of(
        "10: Server restarting in 10 seconds from /restart command...",
        "5: Restarting in 5 seconds...",
        "4: Restarting in 4 seconds...",
        "3: Restarting in 3 seconds...",
        "2: Restarting in 2 seconds...",
        "1: Restarting in 1 second..."
    );

    protected List<? extends String> sampleDynamicRestartMessages = List.of(
        "60: Server restarting in 1 minute due to performance issues...",
        "30: Restarting in 30 seconds...",
        "15: Restarting in 15 seconds...",
        "10: Restarting in 10 seconds...",
        "5: Restarting in 5 seconds...",
        "4: Restarting in 4 seconds...",
        "3: Restarting in 3 seconds...",
        "2: Restarting in 2 seconds...",
        "1: Restarting in 1 second..."
    );

    public class TestConfigBuilder extends Config.Builder {
        @Override
        protected void setupDefaults() {
            this.restartSchedule = List.of("13:00");
            this.rawTimezone = "UTC";
            this.scheduledRestartMessages = sampleScheduleRestartMessages;
            this.restartCommandMessages = sampleCommandRestartMessages;
            this.dynamicRestartMessages = sampleDynamicRestartMessages;
        }
    }

    @BeforeEach
    public void setup() {
        this.taskProvider = new DefaultTaskProvider();
        this.config = new TestConfigBuilder().build();
        this.clock = new TestClock(
            Instant.parse("2025-12-03T10:15:30.00Z"),
            ZoneOffset.ofHours(-5) // EST
        );
        this.schedulerFactory = new TestSchedulerFactory(this.clock);
        this.serverContext = mock();
    }

    protected RestartProcessor getRestartProcessor() {
        return new RestartProcessorImpl(
            taskProvider,
            serverContext,
            config,
            clock,
            schedulerFactory
        );
    }

    protected void advanceTimeBy(Duration duration) {
        this.clock.advanceTimeBy(duration);
    }

    /**
     * Call after setting config with timezone.
     */
    protected void setTime(Instant instant) {
        this.clock = new TestClock(instant, config.getTimezone());
        this.schedulerFactory.setClock(this.clock);
    }
}
