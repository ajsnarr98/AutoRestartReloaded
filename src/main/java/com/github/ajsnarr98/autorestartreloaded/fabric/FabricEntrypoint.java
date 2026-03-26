package com.github.ajsnarr98.autorestartreloaded.fabric;

//? fabric {

import com.github.ajsnarr98.autorestartreloaded.AutoRestartReloaded;
import com.github.ajsnarr98.autorestartreloaded.core.Config;
import com.github.ajsnarr98.autorestartreloaded.core.ConfigSpec;
import com.github.ajsnarr98.autorestartreloaded.core.servercontext.RealServerContext;
import com.github.ajsnarr98.autorestartreloaded.core.task.DefaultTaskProvider;
import com.github.ajsnarr98.autorestartreloaded.core.task.executer.DefaultSchedulerFactory;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;

import java.io.IOException;
import java.time.Clock;

public class FabricEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        ConfigSpec.load(FabricLoader.getInstance().getConfigDir());
        Config config = ConfigSpec.readConfig();

        if (config.isRestartCommandEnabled()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
                AutoRestartReloaded.LOGGER.debug("registering /restart command");
                dispatcher.register(
                    LiteralArgumentBuilder.<CommandSourceStack>literal("restart")
                        .requires(source -> source.hasPermission(config.getCommandPermissionLevel()))
                        .executes(new FabricRestartCommand())
                );
            });
        }

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            AutoRestartReloaded.getInstance().initialize(
                new DefaultTaskProvider(),
                new RealServerContext(server),
                config,
                Clock.system(config.getTimezone()),
                new DefaultSchedulerFactory()
            );
        });

        ServerTickEvents.END_SERVER_TICK.register(server ->
            AutoRestartReloaded.getInstance().onServerTick(server.getAverageTickTimeNanos())
        );

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            AutoRestartReloaded.LOGGER.debug("Server stopped event!");
            try {
                AutoRestartReloaded.getInstance().onServerStopped();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
//?}
