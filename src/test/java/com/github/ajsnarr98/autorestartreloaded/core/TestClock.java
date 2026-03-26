package com.github.ajsnarr98.autorestartreloaded.core;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class TestClock extends Clock {

    private Clock clock;

    private final List<TimeChangedListener> listeners;

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

    /**
     * Updates this clock's zone in place and returns {@code this}.
     *
     * <p>Unlike the standard {@link Clock#withZone} contract (which returns a new
     * clock), this implementation mutates the zone on the same object so that all
     * code holding a reference to this {@code TestClock} — including
     * {@link com.github.ajsnarr98.autorestartreloaded.core.task.RestartScheduler}
     * and {@link com.github.ajsnarr98.autorestartreloaded.core.task.TpsTracker}
     * — always share one mutable clock instance. This makes it easy to assert that
     * time advances via {@link #advanceTimeBy} still drive the processor under test
     * after a config update that includes a new timezone.
     */
    @Override
    public Clock withZone(ZoneId zone) {
        this.clock = this.clock.withZone(zone);
        return this;
    }

    @Override
    public Instant instant() {
        return clock.instant();
    }

    public interface TimeChangedListener {
        void onTimeUpdated(TestClock clock);
    }
}
