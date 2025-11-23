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
                            coexist with other list items in simple format.
                            
                            **Simple format**
                            Times are described as a time of day in 24-hour format. These times specify restarting
                            every day at that time.
                            
                            Examples:
                            11:06
                            14:00
                            
                            **Advanced Format**
                            Advanced times are specified in the "cron" format (in the timezone the server is using),
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

    static final ModConfigSpec SPEC = BUILDER.build();

    static Config readConfig() {
        return new Config(
                RAW_AUTO_RESTART_TIMES.get()
        );
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
}
//?}
