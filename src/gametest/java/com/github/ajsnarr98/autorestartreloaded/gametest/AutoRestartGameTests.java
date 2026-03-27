package com.github.ajsnarr98.autorestartreloaded.gametest;

import com.github.ajsnarr98.autorestartreloaded.AutoRestartReloaded;
import com.github.ajsnarr98.autorestartreloaded.core.Config;
import com.github.ajsnarr98.autorestartreloaded.core.ConfigSpec;
import com.github.ajsnarr98.autorestartreloaded.fabric.FabricEntrypoint;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTestHelper;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class AutoRestartGameTests implements CustomTestMethodInvoker {

    static final FakeServerContext TEST_SERVER_CONTEXT = new FakeServerContext();

    static {
        // This static initializer runs when Fabric creates the fabric-gametest entrypoint
        // instance, which happens during Fabric API's onInitialize() — before our mod's
        // FabricEntrypoint.onInitialize() registers its SERVER_STARTED listener.
        // Setting the factories here ensures they are injected when the server starts.
        FabricEntrypoint.serverContextFactory = server -> TEST_SERVER_CONTEXT;
        // Use an immediate scheduler so the stop task fires within the first tick,
        // avoiding the real-time vs game-tick mismatch in headless game test mode.
        FabricEntrypoint.schedulerFactory = new ImmediateSchedulerFactory();
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        TEST_SERVER_CONTEXT.reset();
        method.invoke(this, helper);
    }

    // -------------------------------------------------------------------------
    // Config tests
    // -------------------------------------------------------------------------

    /**
     * Verifies the config file was written to disk by kaleido-config on first startup.
     */
    @GameTest
    public void testConfigFileExistsOnDisk(GameTestHelper helper) {
        Path configFile = FabricLoader.getInstance().getConfigDir()
            .resolve(AutoRestartReloaded.MODID + ".toml");
        if (!Files.exists(configFile)) {
            helper.fail("Config file not found at: " + configFile);
            return;
        }
        helper.succeed();
    }

    /**
     * Verifies the in-memory config (read from disk at startup) contains the expected
     * default values defined in {@link com.github.ajsnarr98.autorestartreloaded.core.ConfigSpec}.
     */
    @GameTest
    public void testConfigDefaultValues(GameTestHelper helper) {
        Config config = ConfigSpec.readConfig();

        // min_delay_before_auto_restart = 15 minutes
        long delayMinutes = config.getMinDelayBeforeAutoRestart().toMinutes();
        if (delayMinutes != 15) {
            helper.fail("Expected min_delay_before_auto_restart=15 but got " + delayMinutes);
            return;
        }

        // schedule.timezone = UTC+0 (Java normalises this to "UTC" or "GMT")
        String timezone = config.getTimezone().getId();
        if (!timezone.equals("UTC") && !timezone.equals("UTC+0") && !timezone.equals("GMT")) {
            helper.fail("Expected timezone UTC+0 but got: " + timezone);
            return;
        }

        // command.restart_command_enabled = true
        if (!config.isRestartCommandEnabled()) {
            helper.fail("Expected restart_command_enabled=true");
            return;
        }

        // command.command_permission_level = 4
        if (config.getCommandPermissionLevel() != 4) {
            helper.fail("Expected command_permission_level=4 but got " + config.getCommandPermissionLevel());
            return;
        }

        // dynamic.restart_on_low_tps = true
        if (!config.shouldRestartForTps()) {
            helper.fail("Expected restart_on_low_tps=true");
            return;
        }

        // dynamic.min_tps_level = 10.0
        if (config.getMinTpsLevel() != 10.0) {
            helper.fail("Expected min_tps_level=10.0 but got " + config.getMinTpsLevel());
            return;
        }

        // dynamic.min_low_tps_minutes = 5.0 → 300 seconds
        long lowTpsSeconds = config.getLowTPSMinDuration().toSeconds();
        if (lowTpsSeconds != 300) {
            helper.fail("Expected min_low_tps_minutes=5 (300 s) but got " + lowTpsSeconds + " s");
            return;
        }

        helper.succeed();
    }

    // -------------------------------------------------------------------------
    // Scheduled restart tests
    // -------------------------------------------------------------------------

    /**
     * Verifies that a scheduled restart (via cron config) eventually calls
     * {@code runCommand("stop")} on the injected {@link FakeServerContext}.
     *
     * <p>With {@link ImmediateSchedulerFactory} all tasks run at 0 ms delay,
     * so the stop fires within the first tick.
     */
    @GameTest(maxTicks = 100)
    public void testScheduledRestartCallsStop(GameTestHelper helper) {
        String expectedRestartMessage = "scheduled restart message";
        Config testConfig = new Config.Builder()
            .restartSchedule(List.of("* * * * *"))
            .rawTimezone("UTC+0")
            .scheduledRestartMessages(List.of(expectedRestartMessage))
            .restartCommandMessages(List.of("restart command message"))
            .dynamicRestartMessages(List.of("dynamic restart message"))
            .minMinutesBeforeAutoRestart(0)
            .shouldRestartForTps(false)
            .lowTpsMinMinutes(1.0)
            .minTpsLevel(10.0)
            .commandPermissionLevel(4)
            .restartCommandEnabled(true)
            .build();
        AutoRestartReloaded.getInstance().onConfigUpdated(testConfig);

        helper.succeedWhen(() -> {
            boolean hasExpectedRestartMessage = !TEST_SERVER_CONTEXT.getCommands().contains(
                String.format("tellraw @a {\"text\":\"%s\",\"color\":\"yellow\"}", expectedRestartMessage)
            );
            if (!hasExpectedRestartMessage) {
                helper.fail(String.format(
                    "'%s' message not yet recorded in TestServerContext",
                    expectedRestartMessage
                ));
            }
            int numStopCommands = TEST_SERVER_CONTEXT.getCommands().stream().filter(text -> text.equals("stop")).toList().size();
            if (numStopCommands < 1) {
                helper.fail("'stop' command not yet recorded in TestServerContext");
            }
            if (numStopCommands > 1) {
                helper.fail("More than one 'stop' command recorded in TestServerContext");
            }
        });
    }

    // -------------------------------------------------------------------------
    // Command tests
    // -------------------------------------------------------------------------

    /**
     * Verifies the /restart command is present in the server's command dispatcher
     * (i.e. it was registered during mod initialisation).
     */
    @GameTest
    public void testRestartCommandIsRegistered(GameTestHelper helper) {
        var root = helper.getLevel().getServer().getCommands().getDispatcher().getRoot();
        if (root.getChild("restart") == null) {
            helper.fail("/restart command is not registered in the command dispatcher");
            return;
        }
        helper.succeed();
    }

    /**
     * Triggers the /restart command and asserts that the mod eventually calls
     * {@code runCommand("stop")} on the injected {@link FakeServerContext}.
     *
     * <p>With {@link ImmediateSchedulerFactory} all tasks run at 0 ms delay,
     * so the stop fires within the first tick of the command being run.
     */
    @GameTest(maxTicks = 100)
    public void testRestartCommandCallsStop(GameTestHelper helper) {
        String expectedRestartMessage = "restart command message";
        Config testConfig = new Config.Builder()
            .restartSchedule(List.of())
            .rawTimezone("UTC+0")
            .scheduledRestartMessages(List.of("scheduled restart message"))
            .restartCommandMessages(List.of(expectedRestartMessage))
            .dynamicRestartMessages(List.of("dynamic restart message"))
            .minMinutesBeforeAutoRestart(0)
            .shouldRestartForTps(false)
            .lowTpsMinMinutes(1.0)
            .minTpsLevel(10.0)
            .commandPermissionLevel(4)
            .restartCommandEnabled(true)
            .build();
        AutoRestartReloaded.getInstance().onConfigUpdated(testConfig);

        // Trigger the restart command.
        AutoRestartReloaded.getInstance().onManualRestartCommand();

        // succeedWhen is checked every tick. The real ScheduledExecutorService fires the
        // stop task after ~1000 ms (~20 ticks). helper.fail() throws GameTestAssertException,
        // which the framework catches and retries next tick; returning normally causes success.
        helper.succeedWhen(() -> {
            boolean hasExpectedRestartMessage = !TEST_SERVER_CONTEXT.getCommands().contains(
                String.format("tellraw @a {\"text\":\"%s\",\"color\":\"yellow\"}", expectedRestartMessage)
            );
            if (!hasExpectedRestartMessage) {
                helper.fail(String.format(
                    "'%s' message not yet recorded in TestServerContext",
                    expectedRestartMessage
                ));
            }
            int numStopCommands = TEST_SERVER_CONTEXT.getCommands().stream().filter(text -> text.equals("stop")).toList().size();
            if (numStopCommands < 1) {
                helper.fail("'stop' command not yet recorded in TestServerContext");
            }
            if (numStopCommands > 1) {
                helper.fail("More than one 'stop' command recorded in TestServerContext");
            }
        });
    }
}
