package com.github.ajsnarr98.autorestartreloaded.core;

import java.sql.Time;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class TestClock extends Clock {

    private Clock clock;

    private List<TimeChangedListener> listeners;

    public TestClock(Instant startingInstant, ZoneId zone) {
        this(Clock.fixed(startingInstant, zone), new ArrayList<>());
    }

    private TestClock(Clock clock, List<TimeChangedListener> timeChangedListeners) {
        this.clock = clock;
        this.listeners = timeChangedListeners;
    }

    /**
     * Add a listener that will get called on time change.
     */
    public void addOnTimeChangedListener(TimeChangedListener listener) {
        this.listeners.add(listener);
    }

    public boolean removeOnTimeChangedListener(TimeChangedListener listener) {
        return this.listeners.remove(listener);
    }

    /**
     * Advances time by the given duration, for controlling tests.
     */
    public void advanceTimeBy(Duration duration) {
        this.clock = Clock.offset(this.clock, duration);
        for (TimeChangedListener listener : this.listeners) {
            listener.onTimeUpdated(this);
        }
    }

    @Override
    public ZoneId getZone() {
        return clock.getZone();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new TestClock(this.clock.withZone(zone), this.listeners);
    }

    @Override
    public Instant instant() {
        return clock.instant();
    }

    public interface TimeChangedListener {
        void onTimeUpdated(TestClock clock);
    }
}
