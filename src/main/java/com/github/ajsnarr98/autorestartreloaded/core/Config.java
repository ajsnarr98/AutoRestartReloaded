package com.github.ajsnarr98.autorestartreloaded.core;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.github.ajsnarr98.autorestartreloaded.AutoRestartReloaded;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class Config {
    private final List<Cron> cronRestartSchedule;
    private final RestartMessages restartCommandMessages;
    private final ZoneId timezone;

    public Config(
        List<? extends String> restartSchedule,
        String rawTimezone
    ) {
        this.cronRestartSchedule = restartSchedule.stream()
            .map(Config::parseRestartTime)
            .toList();
        this.restartCommandMessages = new RestartMessages(
            Stream.of(
                    "5: Restarting in 5 seconds...",
                    "4: Restarting in 4 seconds...",
                    "3: Restarting in 3 seconds...",
                    "2: Restarting in 2 seconds...",
                    "1: Restarting in 1 second..."
                )
                .map(Config::parseRestartMessage)
                .toList()
        );
        this.timezone = ZoneId.of(rawTimezone);
    }

    public RestartMessages getRestartCommandMessages() {
        return restartCommandMessages;
    }

    public ZoneId getTimezone() {
        return timezone;
    }

    /**
     * @param now the time (ms) to start searching from
     */
    public Optional<Instant> nextPreScheduledRestartTime(Instant now) {
        @Nullable Instant closest = null;

        // use UTC for zone since the cron library assumes our times were specified in UTC,
        // when really they are in the config-specified timezone
        ZonedDateTime zonedNow = ZonedDateTime.ofInstant(now, getTimezone());

        for (Cron cron : cronRestartSchedule) {
            ExecutionTime executionTime = ExecutionTime.forCron(cron);
            Optional<ZonedDateTime> nextExec = executionTime.nextExecution(zonedNow);
            if (nextExec.isPresent()) {
                Instant next = nextExec.get().toInstant();
                if (closest == null) {
                    closest = next;
                } else if (next.isBefore(closest)) {
                    closest = next;
                }
            }
        }

        return Optional.ofNullable(closest);
    }

    // --------------- static properties/functions --------------------
    private static final CronDefinition CRON_DEFINITION = CronDefinitionBuilder.instanceDefinitionFor(
        CronType.UNIX
    );
    private static final CronParser CRON_PARSER = new CronParser(CRON_DEFINITION);

    public static boolean validateRestartTime(String argument) {
        try {
            parseRestartTime(argument);
            return true;
        } catch (IllegalArgumentException e) {
            String msg = String.format("Failed to parse restart time: '%s'", argument);
            AutoRestartReloaded.LOGGER.error(msg, e);
            return false;
        }
    }

    public static boolean validateRestartMessage(String argument) {
        try {
            parseRestartMessage(argument);
            return true;
        } catch (IllegalArgumentException e) {
            String msg = String.format("Failed to parse restart message: '%s'", argument);
            AutoRestartReloaded.LOGGER.error(msg, e);
            return false;
        }
    }

    public static class RestartMessages {
        public final List<RestartMessage> messages;
        /** Difference in time between the first message sent, and the actual restart. */
        public final Duration highestLeadingTime;

        public RestartMessages(List<RestartMessage> messages) {
            this.messages = messages;
            long highestLeadingMs = 0;
            for (Config.RestartMessage message : messages) {
                highestLeadingMs = Math.max(highestLeadingMs, message.msBeforeRestart);
            }
            this.highestLeadingTime = Duration.ofMillis(highestLeadingMs);
        }
    }

    public static class RestartMessage {
        public final long msBeforeRestart;
        public final String message;

        public RestartMessage(long msBeforeRestart, String message) {
            this.msBeforeRestart = msBeforeRestart;
            this.message = message;
        }
    }

    private static RestartMessage parseRestartMessage(String raw) throws IllegalArgumentException {
        int pos = raw.indexOf(':');
        if (pos <= 0 || pos >= (raw.length() - 1)) {
            throw new IllegalArgumentException("Message needs to start with a number of seconds before restart," +
                " followed by \":\", with the printed message after. But message was: \"" + raw + "\"");
        }

        long leadingSeconds = Long.parseLong(raw.substring(0, pos));
        String message = raw.substring(pos + 1);

        return new RestartMessage(leadingSeconds * 1000, message);
    }

    private static Cron parseRestartTime(String argument) throws IllegalArgumentException {
        String trimmed = argument.trim();
        String cronStr = trimmed;

        if (!trimmed.contains(" ")) {
            // this probably is not a cron definition, since there are no spaces
            String[] split = trimmed.split(":");
            if (split.length != 2)
                throw new IllegalArgumentException("Argument was not in the format HH:MM or was not a valid cron expression");

            int hour = Integer.parseInt(split[0]);
            int minute = Integer.parseInt(split[1]);

            // check that the numbers are in the right range
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                throw new IllegalArgumentException("Passed in numbers were outside the range of hour (0-23) or minute (0-59)");
            }

            cronStr = String.format("%d %d * * *", minute, hour);
        }

        // assume this could by a cron definition
        try {
            return CRON_PARSER.parse(cronStr)
                .validate();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("failed to read cron definition '%s'", cronStr), e);
        }
    }
}
