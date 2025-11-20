package com.github.ajsnarr98.autorestartmod;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.cronutils.builder.CronBuilder;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.field.expression.FieldExpression;
import com.cronutils.model.field.expression.On;
import com.cronutils.model.field.value.IntegerFieldValue;
import com.cronutils.parser.CronParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
    private static final CronDefinition CRON_DEFINITION = CronDefinitionBuilder.instanceDefinitionFor(
            CronType.QUARTZ
    );
    private static final CronParser CRON_PARSER = new CronParser(CRON_DEFINITION);
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
                            # | | | | |
                            # | | | | day of the week (0–6) (Sunday to Saturday)
                            # | | day of the month (1–31)
                            # | hour (0–23)
                            # minute (0–59)
                            
                            For example:
                              - A restart that happens every day at 1:00 AM:
                                0 1 * * *
                              - A restart that happens every Monday at 5:00 PM:
                                0 17 * * 1
                              - A restart that happens once a month at midnight on the first day of the month:
                                8 8 1 * *
                            
                            For more advanced examples, see https://en.wikipedia.org/wiki/Cron"""
            )
            .defineListAllowEmpty("restart_times", List.of("5:00"), () -> "", Config::validateRestartTime);

//    public static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
//            .comment("Whether to log the dirt block on common setup")
//            .define("logDirtBlock", true);
//
//    public static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
//            .comment("A magic number")
//            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);
//
//    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
//            .comment("What you want the introduction message to be for the magic number")
//            .define("magicNumberIntroduction", "The magic number is... ");

    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateRestartTime(final Object obj) {
        if (!(obj instanceof String)) return false;

        String argument = ((String) obj).trim();

        try {
            parseRestartTime(argument);
            return true;
        } catch (IllegalArgumentException e) {
            String msg = String.format("Failed to parse restart time: '%s'", argument);
            AutoRestartReloaded.LOGGER.error(msg, e);
            return false;
        }
    }

    public static Cron parseRestartTime(String argument) throws IllegalArgumentException, NumberFormatException {
        if (!argument.contains(" ")) {
            // this probably is not a cron definition, since there are no spaces
            String[] split = argument.split(":");
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
                return CRON_PARSER.parse(argument).validate();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("failed to read cron definition", e);
            }
        }
    }
}
