package com.github.ajsnarr98.autorestartreloaded.core;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class TimeUtils {
    public static String getHumanReadableTime(long epochMillis) {
        Instant inst = Instant.ofEpochMilli(epochMillis);
        ZonedDateTime zdt = inst.atZone(ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
        return formatter.format(zdt);
    }
}
