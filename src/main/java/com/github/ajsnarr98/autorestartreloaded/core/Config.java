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

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class Config {
    private final List<Cron> cronRestartSchedule;
    private final RestartMessages scheduledRestartMessages;
    private final RestartMessages restartCommandMessages;
    private final RestartMessages dynamicRestartMessages;
    private final ZoneId timezone;
    /** Min delay after server starts before an auto restart. **/
    private final Duration minDelayBeforeAutoRestart;
    private final boolean shouldRestartForTps;
    private final Duration lowTPSMinDuration;
    private final double minTpsLevel;
    private final int commandPermissionLevel;
    private final boolean restartCommandEnabled;

    public static class Builder {

        protected List<? extends String> restartSchedule;
        protected String rawTimezone;
        protected List<? extends String> scheduledRestartMessages;
        protected List<? extends String> restartCommandMessages;
        protected List<? extends String> dynamicRestartMessages;
        protected int minMinutesBeforeAutoRestart;
        protected boolean shouldRestartForTps;
        protected double lowTpsMinMinutes;
        protected double minTpsLevel;
        protected int commandPermissionLevel;
        protected boolean restartCommandEnabled;

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

        public Builder minMinutesBeforeAutoRestart(int minMinutesBeforeAutoRestart) {
            this.minMinutesBeforeAutoRestart = minMinutesBeforeAutoRestart;
            return this;
        }

        public Builder shouldRestartForTps(boolean shouldRestartForTps) {
            this.shouldRestartForTps = shouldRestartForTps;
            return this;
        }

        public Builder lowTpsMinMinutes(double lowTpsMinMinutes) {
            this.lowTpsMinMinutes = lowTpsMinMinutes;
            return this;
        }

        public Builder minTpsLevel(double minTpsLevel) {
            this.minTpsLevel = minTpsLevel;
            return this;
        }

        public Builder commandPermissionLevel(int commandPermissionLevel) {
            this.commandPermissionLevel = commandPermissionLevel;
            return this;
        }

        public Builder restartCommandEnabled(boolean restartCommandEnabled) {
            this.restartCommandEnabled = restartCommandEnabled;
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
        this.minDelayBeforeAutoRestart = Duration.ofMinutes(builder.minMinutesBeforeAutoRestart);
        this.shouldRestartForTps = builder.shouldRestartForTps;
        this.lowTPSMinDuration = Duration.ofSeconds((int)(builder.lowTpsMinMinutes * 60));
        this.minTpsLevel = builder.minTpsLevel;
        this.commandPermissionLevel = builder.commandPermissionLevel;
        this.restartCommandEnabled = builder.restartCommandEnabled;
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

    public boolean isShouldRestartForTps() {
        return shouldRestartForTps;
    }

    public Duration getLowTPSMinDuration() {
        return lowTPSMinDuration;
    }

    public double getMinTpsLevel() {
        return minTpsLevel;
    }

    public ZoneId getTimezone() {
        return timezone;
    }

    public Duration getMinDelayBeforeAutoRestart() {
        return minDelayBeforeAutoRestart;
    }

    public int getCommandPermissionLevel() {
        return commandPermissionLevel;
    }

    public boolean isRestartCommandEnabled() {
        return restartCommandEnabled;
    }

    /**
     * @param now the time (ms) to start searching from
     */
    public Optional<Instant> nextPreScheduledRestartTime(Instant now) {
        Instant closest = null;

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

    private static boolean genericValidateRestartTime(final Object obj) {
        if (!(obj instanceof String)) return false;
        return Config.validateRestartTime((String) obj);
    }

    private static boolean genericValidateRestartMessage(final Object obj) {
        if (!(obj instanceof String)) return false;
        return Config.validateRestartMessage((String) obj);
    }

    private static boolean genericIsGreaterThan0(final Object obj) {
        if (!(obj instanceof Number)) return false;

        return ((Number) obj).doubleValue() > 0;
    }

    public static class Description<T> {
        public final String name;
        public final String description;
        public final T defaultValue;

        /** If nothing is set, will always return true. **/
        public Predicate<Object> validator = (Object o) -> true;
        public boolean allowEmptyList = false;

        public Description(String name, String description, T defaultValue) {
            this.name = name;
            this.description = description;
            this.defaultValue = defaultValue;

            // we currently only support lists that contain strings
            if (defaultValue instanceof List<?> defList) {
                assert defList.isEmpty() || defList.stream().allMatch(elem -> elem instanceof String);
            }
        }

        public Description<T> withValidator(Predicate<Object> validator) {
            this.validator = validator;
            return this;
        }

        /**
         * Call this function if this config is a list, and can be empty;
         */
        public Description<T> allowEmptyList(boolean allowEmptyList) {
            this.allowEmptyList = allowEmptyList;
            return this;
        }

        public boolean isList() {
            return defaultValue instanceof List;
        }
    }

    public static class DescriptionWithIntRange extends Description<Integer> {
        public final int min;
        public final int max;

        public DescriptionWithIntRange(
            String name,
            String description,
            Integer defaultValue,
            int min,
            int max
        ) {
            super(name, description, defaultValue);
            this.min = min;
            this.max = max;
        }
    }

    public static class DescriptionWithDoubleRange extends Description<Double> {
        public final double min;
        public final double max;

        public DescriptionWithDoubleRange(
            String name,
            String description,
            Double defaultValue,
            double min,
            double max
        ) {
            super(name, description, defaultValue);
            this.min = min;
            this.max = max;
        }
    }

    public static final Description<List<? extends String>> RAW_AUTO_RESTART_TIMES = new Description<List<? extends String>>(
        "restart_times",
        """
                A list of times when the server will restart. These times can be defined in either a
                simple format, or an advanced format. You can have list items in advanced format
                coexist with other list items in simple format. If you do not want to have the server
                restart at a fixed time, you can leave this list empty.
                
                **Simple format**
                Times are described as a time of day in 24-hour format. These times specify restarting
                every day at that time.
                
                Examples:
                11:06
                14:00
                
                **Advanced Format**
                Advanced times are specified in the "cron" format (in the timezone specified in this config),
                where each line looks like this:
                 * * * * *
                 | | | | |
                 | | | | day of the week (0–6) (Sunday to Saturday)
                 | | day of the month (1–31)
                 | hour (0–23)
                 minute (0–59)
                
                For example:
                  - A restart that happens every day at 1:00 AM:
                    0 1 * * *
                  - A restart that happens every Monday at 5:00 PM:
                    0 17 * * 1
                  - A restart that happens once a month at midnight on the first day of the month:
                    8 8 1 * *
                
                For more advanced examples, see https://en.wikipedia.org/wiki/Cron"""
            .replace("\n", "\n "),
        List.of("5:00")
    )
        .withValidator(Config::genericValidateRestartTime)
        .allowEmptyList(true);

    public static final Description<String> RAW_TIMEZONE = new Description<>(
        "timezone",
        """
                The timezone that restart_times are specified in. By default,
                we use UTC (Coordinated Universal Time). You can specify this
                time in one of two ways:
                \t1. an offset from UTC, for example "UTC-5" or "UTC+1"
                \t2. a timezone from the tz database, for example "America/New_York"
                 \s
                You can see tz database entries here https://en.wikipedia.org/wiki/List_of_tz_database_time_zones
           \s""",
        "UTC+0"
    );

    public static final Description<List<? extends String>> SCHEDULED_RESTART_MESSAGES = new Description<List<? extends String>>(
        "scheduled_restart_messages",
        """
                This describes the messages to send to the server before a
                SCHEDULED restart (a time specified in restart_times),
                and when to send them. All entries should start with the number
                of seconds before the restart the message should be sent,
                with : right after, and the intended message after that.
                For example:
                \s
                "5: Hello, we are about to restart soon" would send
                "Hello we are about to restart soon" in chat 5 seconds before
                the restart happens.
                \s
                "1800: Server restarting in a while" would send "Server
                restarting in a while" in chat 30 minutes before the server
                restarts from a scheduled restart.
                \s
                "3600: Server restarting in a while"  would send "Server
                restarting in a while" in chat 1 hour before the server restarts
                from a from a scheduled restart.
            """,
        List.of(
            "1800: Server restarting in 30 minutes for scheduled restart",
            "600: Server restarting in 10 minutes for scheduled restart",
            "300: Server restarting in 5 minutes",
            "60: Server restarting in 1 minute",
            "30: Restarting in 30 seconds...",
            "15: Restarting in 15 seconds...",
            "10: Restarting in 10 seconds...",
            "5: Restarting in 5 seconds...",
            "4: Restarting in 4 seconds...",
            "3: Restarting in 3 seconds...",
            "2: Restarting in 2 seconds...",
            "1: Restarting in 1 second..."
        )
    ).withValidator(Config::genericValidateRestartTime);

    public static final Description<List<? extends String>> COMMAND_RESTART_MESSAGES = new Description<List<? extends String>>(
        "command_restart_messages",
        """
                This describes the messages to send to the server before a
                MANUAL restart (via the /restart command), and when to send
                them. All entries should start with the number of seconds
                before the restart the message should be sent, with : right
                after, and the intended message after that. For example:
                \s
                "5: Hello, we are about to restart soon" would send
                "Hello we are about to restart soon" in chat 5 seconds before
                the restart happens.
                \s
                "1800: Server restarting in a while" would send "Server
                restarting in a while" in chat 30 minutes before the server
                restarts from a /restart command restart.
                \s
                "3600: Server restarting in a while"  would send "Server
                restarting in a while" in chat 1 hour before the server restarts
                from a from a /restart command restart.
            """,
        List.of(
            "10: Server restarting in 10 seconds from /restart command...",
            "5: Restarting in 5 seconds...",
            "4: Restarting in 4 seconds...",
            "3: Restarting in 3 seconds...",
            "2: Restarting in 2 seconds...",
            "1: Restarting in 1 second..."
        )
    ).withValidator(Config::genericValidateRestartTime);

    public static final Description<List<? extends String>> DYNAMIC_RESTART_MESSAGES = new Description<List<? extends String>>(
        "dynamic_restart_messages",
        """
                This describes the messages to send to the server before a
                DYNAMIC restart (for example a restart that happens because of
                low TPS INSTEAD of happening at a fixed time), and when to send
                them. All entries should start with the number of seconds
                before the restart the message should be sent, with : right
                after, and the intended message after that. For example:
                \s
                "5: Hello, we are about to restart soon" would send
                "Hello we are about to restart soon" in chat 5 seconds before
                the restart happens.
                \s
                "1800: Server restarting in a while" would send "Server
                restarting in a while" in chat 30 minutes before the server
                restarts from low TPS.
                \s
                "3600: Server restarting in a while"  would send "Server
                restarting in a while" in chat 1 hour before the server restarts
                from low TPS.
            """,
        List.of(
            "60: Server restarting in 1 minute due to performance issues...",
            "30: Restarting in 30 seconds...",
            "15: Restarting in 15 seconds...",
            "10: Restarting in 10 seconds...",
            "5: Restarting in 5 seconds...",
            "4: Restarting in 4 seconds...",
            "3: Restarting in 3 seconds...",
            "2: Restarting in 2 seconds...",
            "1: Restarting in 1 second..."
        )
    ).withValidator(Config::genericValidateRestartTime);

    public static final DescriptionWithIntRange MIN_DELAY_BEFORE_AUTO_RESTART = new DescriptionWithIntRange(
        "min_delay_before_auto_restart",
        """
                The minimum delay (in minutes) before another automatic restart
                can happen after the server restarts/starts. Set to 0 for there
                to be no delay.
            """,
        0,
        0,
        Integer.MAX_VALUE
    );

    public static final Description<Boolean> RESTART_ON_LOW_TPS = new Description<>(
        "restart_on_low_tps",
        """
                Set this to true if you want the server to restart when the TPS
                (ticks per second) is below the min_tps_level for the given
                min_low_tps_minutes number of minutes. Set to false if
                you want to ignore this rule.
            """,
        true
    );

    public static final Description<Double> MIN_TPS_LEVEL = new DescriptionWithDoubleRange(
        "min_tps_level",
        """
                The ticks per second the server must consistently stay below
                for min_low_tps_minutes in order for the server to restart.
                Only applies if restart_on_low_tps is set to true. Must be
                between 0 and 20.
               \s
                A server is functioning properly if its ticks per second (TPS)
                is at 20. The TPS does not go any higher than this.
           \s""",
        10.0,
        0.0,
        20.0
    );

    public static final Description<Double> MIN_LOW_TPS_MINUTES = new Description<>(
        "min_low_tps_minutes",
        """
                The number of minutes that the server's TPS must stay below
                the min_tps_level before the server automatically restarts.
                Only applies if restart_on_low_tps is set to true. Must be
                greater than 0.
            """,
        5.0
    ).withValidator(Config::genericIsGreaterThan0);
}
