package com.github.ajsnarr98.autorestartreloaded.core;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.github.ajsnarr98.autorestartreloaded.AutoRestartReloaded;
import com.github.ajsnarr98.autorestartreloaded.core.error.InvalidRestartMessageException;
import com.github.ajsnarr98.autorestartreloaded.core.error.InvalidRestartTimeException;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public class Config {
    private final List<Cron> cronRestartSchedule;
    private final RestartMessages scheduledRestartMessages;
    private final RestartMessages restartCommandMessages;
    private final RestartMessages dynamicRestartMessages;
    private final ZoneId timezone;

    public static class Builder {

        protected List<? extends String> restartSchedule;
        protected String rawTimezone;
        protected List<? extends String> scheduledRestartMessages;
        protected List<? extends String> restartCommandMessages;
        protected List<? extends String> dynamicRestartMessages;

        public Builder() {
            setupDefaults();
        }

        protected void setupDefaults() {
            // override this in testing
        }

        public Builder restartSchedule(List<? extends String> restartSchedule) {
            this.restartSchedule = restartSchedule;
            return this;
        }

        public Builder rawTimezone(String rawTimezone) {
            this.rawTimezone = rawTimezone;
            return this;
        }

        public Builder scheduledRestartMessages(List<? extends String> scheduledRestartMessages) {
            this.scheduledRestartMessages = scheduledRestartMessages;
            return this;
        }

        public Builder restartCommandMessages(List<? extends String> restartCommandMessages) {
            this.restartCommandMessages = restartCommandMessages;
            return this;
        }

        public Builder dynamicRestartMessages(List<? extends String> dynamicRestartMessages) {
            this.dynamicRestartMessages = dynamicRestartMessages;
            return this;
        }

        public Config build() {
            return new Config(this);
        }

        /**
         * Assert that all required properties are present.
         */
        protected void verify() {
            assertNotNull("restartSchedule", this.restartSchedule);
            assertNotNull("scheduledRestartMessages", this.scheduledRestartMessages);
            assertNotNull("restartCommandMessages", this.restartCommandMessages);
            assertNotNull("dynamicRestartMessages", this.dynamicRestartMessages);
            assertNotNull("rawTimezone", this.rawTimezone);
        }

        private void assertNotNull(String name, Object value) {
            if (value == null) {
                throw new IllegalArgumentException(
                    String.format("%s is required for config, but was not provided and had no default value", name)
                );
            }
        }
    }

    public Config(
        Config.Builder builder
    ) {
        builder.verify();

        this.timezone = ZoneId.of(builder.rawTimezone);
        this.cronRestartSchedule = builder.restartSchedule.stream()
            .map(Config::parseRestartTime)
            .toList();
        this.restartCommandMessages = new RestartMessages(
            builder.restartCommandMessages.stream()
                .map(Config::parseRestartMessage)
                .toList()
        );
        this.scheduledRestartMessages = new RestartMessages(
            builder.scheduledRestartMessages.stream()
                .map(Config::parseRestartMessage)
                .toList()
        );
        this.dynamicRestartMessages = new RestartMessages(
            builder.dynamicRestartMessages.stream()
                .map(Config::parseRestartMessage)
                .toList()
        );
    }

    public RestartMessages getRestartCommandMessages() {
        return restartCommandMessages;
    }

    public RestartMessages getScheduledRestartMessages() {
        return scheduledRestartMessages;
    }

    public RestartMessages getDynamicRestartMessages() {
        return scheduledRestartMessages;
    }

    public ZoneId getTimezone() {
        return timezone;
    }

    /**
     * @param now the time (ms) to start searching from
     */
    public Optional<Instant> nextPreScheduledRestartTime(Instant now) {
        @Nullable Instant closest = null;

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
        } catch (InvalidRestartTimeException e) {
            String msg = String.format("Failed to parse restart time: '%s'", argument);
            AutoRestartReloaded.LOGGER.error(msg, e);
            return false;
        }
    }

    public static boolean validateRestartMessage(String argument) {
        try {
            parseRestartMessage(argument);
            return true;
        } catch (InvalidRestartMessageException e) {
            String msg = String.format("Failed to parse restart message: '%s'", argument);
            AutoRestartReloaded.LOGGER.error(msg, e);
            throw e;
//            return false;
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

    private static RestartMessage parseRestartMessage(String raw) throws InvalidRestartMessageException {
        int pos = raw.indexOf(':');
        if (pos <= 0 || pos >= (raw.length() - 1)) {
            throw new InvalidRestartMessageException("Message needs to start with a number of seconds before restart," +
                " followed by \":\", with the printed message after. But message was: \"" + raw + "\"");
        }

        long leadingSeconds = Long.parseLong(raw.substring(0, pos).trim());
        String message = raw.substring(pos + 1).trim();

        return new RestartMessage(leadingSeconds * 1000, message);
    }

    private static Cron parseRestartTime(String argument) throws InvalidRestartTimeException {
        String trimmed = argument.trim();
        String cronStr = trimmed;

        if (!trimmed.contains(" ")) {
            // this probably is not a cron definition, since there are no spaces
            String[] split = trimmed.split(":");
            if (split.length != 2) {
                throw new InvalidRestartTimeException("Restart time \""
                    + trimmed + "\" was not in the format HH:MM or was not a valid cron expression");
            }

            int hour = Integer.parseInt(split[0]);
            int minute = Integer.parseInt(split[1]);

            // check that the numbers are in the right range
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                throw new InvalidRestartTimeException("Passed in numbers for the the restart time \""
                    + trimmed + "\" were outside the range of hour (0-23) or minute (0-59)");
            }

            cronStr = String.format("%d %d * * *", minute, hour);
        }

        // assume this could by a cron definition
        try {
            return CRON_PARSER.parse(cronStr)
                .validate();
        } catch (IllegalArgumentException e) {
            throw new InvalidRestartTimeException(String.format("Failed to read cron definition \"%s\"", cronStr), e);
        }
    }
}
