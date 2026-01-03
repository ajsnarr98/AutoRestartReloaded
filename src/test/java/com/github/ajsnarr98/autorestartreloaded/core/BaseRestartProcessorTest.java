package com.github.ajsnarr98.autorestartreloaded.core;

import com.github.ajsnarr98.autorestartreloaded.core.servercontext.ServerContext;
import com.github.ajsnarr98.autorestartreloaded.core.task.DefaultTaskProvider;
import com.github.ajsnarr98.autorestartreloaded.core.task.QueuedTaskProvider;
import com.github.ajsnarr98.autorestartreloaded.core.task.executer.TestSchedulerFactory;
import org.junit.jupiter.api.BeforeEach;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.Mockito.mock;

public class BaseRestartProcessorTest {

    protected QueuedTaskProvider taskProvider;
    protected ServerContext serverContext;
    protected Config config;
    protected TestClock clock;
    protected TestSchedulerFactory schedulerFactory;

    @BeforeEach
    public void setup() {
        this.taskProvider = new DefaultTaskProvider();
        this.config = new Config(
            List.of("13:00"),
            ZoneOffset.UTC
        );
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
