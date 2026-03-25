package com.github.ajsnarr98.autorestartreloaded.fabric;

//? fabric {
import com.github.ajsnarr98.autorestartreloaded.core.Config;
import folk.sisby.kaleido.api.WrappedConfig;

public class FabricConfigSpec extends WrappedConfig {

    // TODO: wire up Kaleido config fields

    static Config readConfig() {
        return new Config.Builder()
            .restartSchedule(Config.RAW_AUTO_RESTART_TIMES.defaultValue)
            .rawTimezone(Config.RAW_TIMEZONE.defaultValue)
            .scheduledRestartMessages(Config.SCHEDULED_RESTART_MESSAGES.defaultValue)
            .restartCommandMessages(Config.COMMAND_RESTART_MESSAGES.defaultValue)
            .dynamicRestartMessages(Config.DYNAMIC_RESTART_MESSAGES.defaultValue)
            .minMinutesBeforeAutoRestart(Config.MIN_DELAY_BEFORE_AUTO_RESTART.defaultValue)
            .shouldRestartForTps(Config.RESTART_ON_LOW_TPS.defaultValue)
            .lowTpsMinMinutes(Config.MIN_LOW_TPS_MINUTES.defaultValue)
            .minTpsLevel(Config.MIN_TPS_LEVEL.defaultValue)
            .build();
    }
}

//?}
