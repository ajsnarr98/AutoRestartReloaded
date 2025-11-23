package com.github.ajsnarr98.autorestartreloaded.core;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public interface Clock {
    long getEpochSecond();
    ZonedDateTime now(ZoneId zoneId);
}
