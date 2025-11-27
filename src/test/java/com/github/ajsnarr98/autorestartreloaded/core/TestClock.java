package com.github.ajsnarr98.autorestartreloaded.core;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

public class TestClock extends Clock {

    private Clock clock;

    public TestClock(Instant startingInstant, ZoneId zone) {
        this.clock = Clock.fixed(startingInstant, zone);
    }

    private TestClock(Clock clock) {
        this.clock = clock;
    }

    public void advanceTimeBy(Duration duration) {
        this.clock = Clock.offset(this.clock, duration);
    }

    @Override
    public ZoneId getZone() {
        return clock.getZone();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new TestClock(clock.withZone(zone));
    }

    @Override
    public Instant instant() {
        return null;
    }
}
