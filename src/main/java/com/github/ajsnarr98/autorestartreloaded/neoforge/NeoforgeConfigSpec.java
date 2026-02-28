package com.github.ajsnarr98.autorestartreloaded.neoforge;

//? neoforge {

import com.github.ajsnarr98.autorestartreloaded.core.Config;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class NeoforgeConfigSpec {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.ConfigValue<List<? extends String>> RAW_AUTO_RESTART_TIMES = BUILDER
        .comment(
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
                .replace("\n", "\n ")
        )
        .defineListAllowEmpty("restart_times", List.of("5:00"), () -> "", NeoforgeConfigSpec::validateRestartTime);

    private static final ModConfigSpec.ConfigValue<String> RAW_TIMEZONE = BUILDER
        .comment(
            """
                The timezone that restart_times are specified in. By default,
                we use UTC (Coordinated Universal Time). You can specify this
                time in one of two ways:
                \t1. an offset from UTC, for example "UTC-5" or "UTC+1"
                \t2. a timezone from the tz database, for example "America/New_York"
                 \s
                You can see tz database entries here https://en.wikipedia.org/wiki/List_of_tz_database_time_zones
           \s"""
        )
        .define("timezone", "UTC+0");

    private static final ModConfigSpec.ConfigValue<List<? extends String>> SCHEDULED_RESTART_MESSAGES = BUILDER
        .comment(
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
            """
        )
        .defineList(
            "scheduled_restart_messages",
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
            ),
            () -> "",
            NeoforgeConfigSpec::validateRestartMessage
        );

    private static final ModConfigSpec.ConfigValue<List<? extends String>> COMMAND_RESTART_MESSAGES = BUILDER
        .comment(
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
            """
        )
        .defineList(
            "command_restart_messages",
            List.of(
                "10: Server restarting in 10 seconds from /restart command...",
                "5: Restarting in 5 seconds...",
                "4: Restarting in 4 seconds...",
                "3: Restarting in 3 seconds...",
                "2: Restarting in 2 seconds...",
                "1: Restarting in 1 second..."
            ),
            () -> "",
            NeoforgeConfigSpec::validateRestartMessage
        );

    private static final ModConfigSpec.ConfigValue<List<? extends String>> DYNAMIC_RESTART_MESSAGES = BUILDER
        .comment(
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
            """
        )
        .defineList(
            "dynamic_restart_messages",
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
            ),
            () -> "",
            NeoforgeConfigSpec::validateRestartMessage
        );

    private static final ModConfigSpec.ConfigValue<Integer> MIN_DELAY_BEFORE_AUTO_RESTART = BUILDER
        .comment(
            """
                The minimum delay (in minutes) before another automatic restart
                can happen after the server restarts/starts. Set to 0 for there
                to be no delay.
            """
        )
        .defineInRange("min_delay_before_auto_restart", 0, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.ConfigValue<Boolean> RESTART_ON_LOW_TPS = BUILDER
        .comment(
            """
                Set this to true if you want the server to restart when the TPS
                (ticks per second) is below the min_tps_level for the given
                min_low_tps_minutes number of minutes. Set to false if
                you want to ignore this rule.
            """
        )
        .define("restart_on_low_tps", true);

    private static final ModConfigSpec.ConfigValue<Double> MIN_TPS_LEVEL = BUILDER
        .comment(
            """
                The ticks per second the server must consistently stay below
                for min_low_tps_minutes in order for the server to restart.
                Only applies if restart_on_low_tps is set to true. Must be
                greater than 0.
               \s
                A server is functioning properly if its ticks per second (TPS)
                is at 20. The TPS does not go any higher than this.
           \s"""
        )
        .define("min_tps_level", 10.0, NeoforgeConfigSpec::isGreaterThan0);

    private static final ModConfigSpec.ConfigValue<Double> MIN_LOW_TPS_MINUTES = BUILDER
        .comment(
            """
                The number of minutes that the server's TPS must stay below
                the min_tps_level before the server automatically restarts.
                Only applies if restart_on_low_tps is set to true. Must be
                greater than 0.
            """
        )
        .define("min_low_tps_minutes", 5.0, NeoforgeConfigSpec::isGreaterThan0);

    static final ModConfigSpec SPEC = BUILDER.build();

    static Config readConfig() {
        return new Config.Builder()
            .restartSchedule(RAW_AUTO_RESTART_TIMES.get())
            .rawTimezone(RAW_TIMEZONE.get())
            .scheduledRestartMessages(SCHEDULED_RESTART_MESSAGES.get())
            .restartCommandMessages(COMMAND_RESTART_MESSAGES.get())
            .dynamicRestartMessages(DYNAMIC_RESTART_MESSAGES.get())
            .minMinutesBeforeAutoRestart(MIN_DELAY_BEFORE_AUTO_RESTART.get())
            .shouldRestartForTps(RESTART_ON_LOW_TPS.get())
            .lowTpsMinMinutes(MIN_LOW_TPS_MINUTES.get())
            .minTpsLevel(MIN_TPS_LEVEL.get())
            .build();
    }

    // ---------- validation functions ---------------
    private static boolean validateRestartTime(final Object obj) {
        if (!(obj instanceof String)) return false;
        return Config.validateRestartTime((String) obj);
    }

    private static boolean validateRestartMessage(final Object obj) {
        if (!(obj instanceof String)) return false;
        return Config.validateRestartMessage((String) obj);
    }

    private static boolean isGreaterThan0(final Object obj) {
        if (!(obj instanceof Number)) return false;

        return ((Number) obj).doubleValue() > 0;
    }
}
//?}
