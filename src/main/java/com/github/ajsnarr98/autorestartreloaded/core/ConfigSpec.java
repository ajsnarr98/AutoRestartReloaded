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

    @Comment({
        "Messages sent before a MANUAL restart (/restart command). Format: \"<seconds>: <message>\"."
    })
    public List<String> command_restart_messages = ValueList.create("",
        "10: Server restarting in 10 seconds from /restart command...",
        "5: Restarting in 5 seconds...",
        "4: Restarting in 4 seconds...",
        "3: Restarting in 3 seconds...",
        "2: Restarting in 2 seconds...",
        "1: Restarting in 1 second..."
    );

    @Comment({
        "Messages sent before a DYNAMIC restart (e.g. triggered by low TPS). Format: \"<seconds>: <message>\"."
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

    @Comment("Minimum delay (in minutes) after server startup before the first auto-restart. 0 means no delay.")
    @IntegerRange(min = 0, max = Integer.MAX_VALUE)
    public int min_delay_before_auto_restart = 0;

    @Comment("Set to true to restart when TPS stays below min_tps_level for min_low_tps_minutes minutes.")
    public boolean restart_on_low_tps = true;

    @Comment("TPS threshold (0-20). Server must stay below this value to trigger a restart.")
    @FloatRange(min = 0.0, max = 20.0)
    public double min_tps_level = 10.0;

    @Comment("Duration (in minutes) TPS must stay below min_tps_level before restarting. Must be > 0.")
    public double min_low_tps_minutes = 5.0;

    // ---------- loading ----------

    private static ConfigSpec INSTANCE = null;

    public static void load(Path configDir) {
        AutoRestartReloaded.LOGGER.debug("Loading config");
        INSTANCE = ConfigSpec.createToml(configDir, "", AutoRestartReloaded.MODID, ConfigSpec.class);
    }

    public static Config readConfig() {
        if (INSTANCE == null) throw new IllegalStateException("ConfigSpec not loaded");
        return new Config.Builder()
            .restartSchedule(INSTANCE.restart_times)
            .rawTimezone(INSTANCE.timezone)
            .scheduledRestartMessages(INSTANCE.scheduled_restart_messages)
            .restartCommandMessages(INSTANCE.command_restart_messages)
            .dynamicRestartMessages(INSTANCE.dynamic_restart_messages)
            .minMinutesBeforeAutoRestart(INSTANCE.min_delay_before_auto_restart)
            .shouldRestartForTps(INSTANCE.restart_on_low_tps)
            .lowTpsMinMinutes(INSTANCE.min_low_tps_minutes)
            .minTpsLevel(INSTANCE.min_tps_level)
            .build();
    }
}
