package com.github.ajsnarr98.autorestartreloaded.neoforge;

//? neoforge {

/*import com.github.ajsnarr98.autorestartreloaded.core.Config;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class NeoforgeConfigSpec {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.ConfigValue<List<? extends String>> RAW_AUTO_RESTART_TIMES
        = defineConfig(Config.RAW_AUTO_RESTART_TIMES);

    private static final ModConfigSpec.ConfigValue<String> RAW_TIMEZONE
        = defineConfig(Config.RAW_TIMEZONE);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> SCHEDULED_RESTART_MESSAGES
        = defineConfig(Config.SCHEDULED_RESTART_MESSAGES);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> COMMAND_RESTART_MESSAGES
        = defineConfig(Config.COMMAND_RESTART_MESSAGES);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> DYNAMIC_RESTART_MESSAGES
        = defineConfig(Config.DYNAMIC_RESTART_MESSAGES);

    private static final ModConfigSpec.ConfigValue<Integer> MIN_DELAY_BEFORE_AUTO_RESTART
        = defineConfig(Config.MIN_DELAY_BEFORE_AUTO_RESTART);

    private static final ModConfigSpec.ConfigValue<Boolean> RESTART_ON_LOW_TPS
        = defineConfig(Config.RESTART_ON_LOW_TPS);

    private static final ModConfigSpec.ConfigValue<Double> MIN_TPS_LEVEL
        = defineConfig(Config.MIN_TPS_LEVEL);

    private static final ModConfigSpec.ConfigValue<Double> MIN_LOW_TPS_MINUTES
        = defineConfig(Config.MIN_LOW_TPS_MINUTES);

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

    // ---------- helper functions ---------------
    @SuppressWarnings("unchecked")
    private static <T> ModConfigSpec.ConfigValue<T> defineConfig(
        Config.Description<T> desc
    ) {
        ModConfigSpec.Builder builder = BUILDER.comment(desc.description);

        if (desc instanceof Config.DescriptionWithIntRange rangeDesc) {
            return (ModConfigSpec.ConfigValue<T>) builder.defineInRange(
                rangeDesc.name, rangeDesc.defaultValue, rangeDesc.min, rangeDesc.max
            );
        }
        if (desc instanceof Config.DescriptionWithDoubleRange rangeDesc) {
            return (ModConfigSpec.ConfigValue<T>) builder.defineInRange(
                rangeDesc.name, rangeDesc.defaultValue, rangeDesc.min, rangeDesc.max
            );
        }

        if (desc.isList()) {
            // assume the list contains strings
            if (desc.allowEmptyList) {
                return (ModConfigSpec.ConfigValue<T>) builder.defineListAllowEmpty(
                    desc.name, (List<? extends String>) desc.defaultValue, () -> "", desc.validator
                );
            } else {
                return (ModConfigSpec.ConfigValue<T>) builder.defineList(
                    desc.name, (List<? extends String>) desc.defaultValue, () -> "", desc.validator
                );
            }
        }

        return builder.define(desc.name, desc.defaultValue);
    }
}
*///?}
