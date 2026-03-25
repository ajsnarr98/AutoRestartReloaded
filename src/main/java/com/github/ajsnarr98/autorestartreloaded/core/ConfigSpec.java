package com.github.ajsnarr98.autorestartreloaded.core;

import com.github.ajsnarr98.autorestartreloaded.AutoRestartReloaded;
import folk.sisby.kaleido.api.WrappedConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.FloatRange;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.IntegerRange;
import folk.sisby.kaleido.lib.quiltconfig.api.values.ValueList;

import java.nio.file.Path;
import java.util.List;

public class ConfigSpec extends WrappedConfig {

    @Comment("Minimum delay (in minutes) after server startup before the first auto-restart. 0 means no delay.")
    @IntegerRange(min = 0, max = Integer.MAX_VALUE)
    public int min_delay_before_auto_restart = 0;

    public Schedule schedule = new Schedule();
    public Command command = new Command();
    public Dynamic dynamic = new Dynamic();

    public static class Schedule implements Section {

        @Comment({
            "A list of times when the server will restart.",
            "Simple format: HH:MM (e.g. \"5:00\" for 5:00 AM daily).",
            "Advanced format: cron expression (e.g. \"0 5 * * *\").",
            "Leave empty for no scheduled restarts."
        })
        public List<String> restart_times = ValueList.create("", "5:00");

        @Comment({
            "The timezone restart_times are specified in.",
            "Use a UTC offset (e.g. \"UTC+0\", \"UTC-5\") or a tz database name (e.g. \"America/New_York\")."
        })
        public String timezone = "UTC+0";

        @Comment({
            "Messages sent before a SCHEDULED restart. Format: \"<seconds>: <message>\".",
            "E.g. \"60: Restarting in 1 minute\" sends that message 60 seconds before restart."
        })
        public List<String> scheduled_restart_messages = ValueList.create("",
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
        );
    }

    public static class Command implements Section {

        @Comment("Whether the /restart command is registered and available on the server.")
        public boolean restart_command_enabled = true;

        @Comment({
            "The permission level required to run the /restart command.",
            "Minecraft permission levels range from 0 (all players) to 4 (full operator).",
            "See https://minecraft.wiki/w/Permission_level for details."
        })
        @IntegerRange(min = 0, max = 4)
        public int command_permission_level = 4;

        @Comment({
            "Messages sent before a MANUAL restart (/restart command). Format: \"<seconds>: <message>\".",
            "E.g. \"60: Restarting in 1 minute\" sends that message 60 seconds before restart."
        })
        public List<String> command_restart_messages = ValueList.create("",
            "10: Server restarting in 10 seconds from /restart command...",
            "5: Restarting in 5 seconds...",
            "4: Restarting in 4 seconds...",
            "3: Restarting in 3 seconds...",
            "2: Restarting in 2 seconds...",
            "1: Restarting in 1 second..."
        );
    }

    public static class Dynamic implements Section {

        @Comment("Set to true to restart when TPS stays below min_tps_level for min_low_tps_minutes minutes.")
        public boolean restart_on_low_tps = true;

        @Comment("TPS threshold (0-20). Server must stay below this value to trigger a restart.")
        @FloatRange(min = 0.0, max = 20.0)
        public double min_tps_level = 10.0;

        @Comment("Duration (in minutes) TPS must stay below min_tps_level before restarting. Must be > 0.")
        public double min_low_tps_minutes = 5.0;

        @Comment({
            "Messages sent before a DYNAMIC restart (e.g. triggered by low TPS). Format: \"<seconds>: <message>\".",
            "E.g. \"60: Restarting in 1 minute\" sends that message 60 seconds before restart."
        })
        public List<String> dynamic_restart_messages = ValueList.create("",
            "60: Server restarting in 1 minute due to performance issues...",
            "30: Restarting in 30 seconds...",
            "15: Restarting in 15 seconds...",
            "10: Restarting in 10 seconds...",
            "5: Restarting in 5 seconds...",
            "4: Restarting in 4 seconds...",
            "3: Restarting in 3 seconds...",
            "2: Restarting in 2 seconds...",
            "1: Restarting in 1 second..."
        );
    }

    // ---------- loading ----------

    private static ConfigSpec INSTANCE = null;

    public static void load(Path configDir) {
        AutoRestartReloaded.LOGGER.debug("Loading config");
        INSTANCE = ConfigSpec.createToml(configDir, "", AutoRestartReloaded.MODID, ConfigSpec.class);
    }

    public static Config readConfig() {
        if (INSTANCE == null) throw new IllegalStateException("ConfigSpec not loaded");
        return new Config.Builder()
            .restartSchedule(INSTANCE.schedule.restart_times)
            .rawTimezone(INSTANCE.schedule.timezone)
            .scheduledRestartMessages(INSTANCE.schedule.scheduled_restart_messages)
            .restartCommandMessages(INSTANCE.command.command_restart_messages)
            .dynamicRestartMessages(INSTANCE.dynamic.dynamic_restart_messages)
            .minMinutesBeforeAutoRestart(INSTANCE.min_delay_before_auto_restart)
            .shouldRestartForTps(INSTANCE.dynamic.restart_on_low_tps)
            .lowTpsMinMinutes(INSTANCE.dynamic.min_low_tps_minutes)
            .minTpsLevel(INSTANCE.dynamic.min_tps_level)
            .commandPermissionLevel(INSTANCE.command.command_permission_level)
            .restartCommandEnabled(INSTANCE.command.restart_command_enabled)
            .build();
    }
}
