package com.github.ajsnarr98.autorestartreloaded.core;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class RealClock implements Clock {
    public Instant nowInstant() {
        return Instant.now();
    }

    @Override
    public long getEpochSecond() {
        return nowInstant().getEpochSecond();
    }

    @Override
    public ZonedDateTime now(ZoneId zoneId) {
        return ZonedDateTime.now(zoneId);
    }
}
