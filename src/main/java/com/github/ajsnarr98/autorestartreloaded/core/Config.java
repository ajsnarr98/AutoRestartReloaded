package com.github.ajsnarr98.autorestartreloaded.core;

import com.cronutils.builder.CronBuilder;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.field.expression.On;
import com.cronutils.model.field.value.IntegerFieldValue;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.github.ajsnarr98.autorestartreloaded.AutoRestartReloaded;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class Config {
    private final List<Cron> cronRestartSchedule;
    private final List<RestartMessage> restartCommandMessages;
    private final ZoneId timezone;

    public Config(List<String> restartSchedule) {
        this.cronRestartSchedule = restartSchedule.stream()
                .map(Config::parseRestartTime)
                .toList();
        this.restartCommandMessages = Stream.of(
            "5: Restarting in 5 seconds...",
            "4: Restarting in 4 seconds...",
            "3: Restarting in 3 seconds...",
            "2: Restarting in 2 seconds...",
            "1: Restarting in 1 second..."
        )
                .map(Config::parseRestartMessage)
                .toList();
        this.timezone = ZoneId.systemDefault();
    }

    public List<RestartMessage> getRestartCommandMessages() {
        return restartCommandMessages;
    }

    public ZoneId getTimezone() {
        return timezone;
    }

    /**
     * @param now the time to start searching from
     */
    public Optional<Long> nextPreScheduledRestartTime(ZonedDateTime now) {
        long closest = Long.MAX_VALUE;

        for (Cron cron : cronRestartSchedule) {
            ExecutionTime executionTime = ExecutionTime.forCron(cron);
            Optional<ZonedDateTime> nextExec = executionTime.nextExecution(now);
            if (nextExec.isPresent()) {
                long next = nextExec.get().toEpochSecond();
                if (next < closest) {
                    closest = next;
                }
            }
        }

        if (closest != Long.MAX_VALUE) {
            return Optional.of(closest);
        } else {
            return Optional.empty();
        }
    }

    // --------------- static properties/functions --------------------
    private static final CronDefinition CRON_DEFINITION = CronDefinitionBuilder.instanceDefinitionFor(
            CronType.QUARTZ
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

    public static class RestartMessage {
        public final long secondsBeforeRestart;
        public final String message;

        public RestartMessage(long secondsBeforeRestart, String message) {
            this.secondsBeforeRestart = secondsBeforeRestart;
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

        return new RestartMessage(leadingSeconds, message);
    }

    private static Cron parseRestartTime(String argument) throws IllegalArgumentException {
        String trimmed = argument.trim();

        if (!trimmed.contains(" ")) {
            // this probably is not a cron definition, since there are no spaces
            String[] split = trimmed.split(":");
            if (split.length != 2) throw new IllegalArgumentException("Argument was not in the format HH:MM or was not a valid cron expression");

            int hour = Integer.parseInt(split[0]);
            int minute = Integer.parseInt(split[1]);

            // check that the numbers are in the right range
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                throw new IllegalArgumentException("Passed in numbers were outside the range of hour (0-23) or minute (0-59)");
            }

            return CronBuilder.cron(CRON_DEFINITION)
                    .withHour(new On(new IntegerFieldValue(hour)))
                    .withMinute(new On(new IntegerFieldValue(minute)))
                    .instance();
        } else {
            // assume this could by a cron definition
            try {
                return CRON_PARSER.parse(trimmed).validate();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("failed to read cron definition", e);
            }
        }
    }
}
