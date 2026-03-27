package com.github.ajsnarr98.autorestartreloaded.neoforge;

//? neoforge {

import com.github.ajsnarr98.autorestartreloaded.AutoRestartReloaded;
import com.github.ajsnarr98.autorestartreloaded.core.Config;
import com.github.ajsnarr98.autorestartreloaded.core.ConfigSpec;
import com.github.ajsnarr98.autorestartreloaded.core.servercontext.RealServerContext;
import com.github.ajsnarr98.autorestartreloaded.core.task.DefaultTaskProvider;
import com.github.ajsnarr98.autorestartreloaded.core.task.executer.DefaultSchedulerFactory;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.IOException;
import java.time.Clock;

@Mod(value = AutoRestartReloaded.MODID, dist = Dist.DEDICATED_SERVER)
public class NeoforgeEntrypoint {

    private MainEventHandler mainEventHandler = new MainEventHandler();

    public NeoforgeEntrypoint(IEventBus modEventBus, ModContainer modContainer) {
        ConfigSpec.load(FMLPaths.CONFIGDIR.get());
        NeoForge.EVENT_BUS.register(this.mainEventHandler);
    }

    public static class MainEventHandler {

        private MinecraftServer server = null;

        @SubscribeEvent
        public void onRegisterCommands(RegisterCommandsEvent event) {
            Config config = ConfigSpec.readConfig();
            if (config.isRestartCommandEnabled()) {
                AutoRestartReloaded.LOGGER.debug("registering /restart command");
                event.getDispatcher().register(
                    LiteralArgumentBuilder.<CommandSourceStack>literal("restart")
                        .requires(source -> source.hasPermission(config.getCommandPermissionLevel()))
                        .executes(new NeoforgeRestartCommand())
                );
            }
        }

        @SubscribeEvent
        public void onServerStarted(ServerStartedEvent event) {
            this.server = event.getServer();
            Config config = ConfigSpec.readConfig();
            AutoRestartReloaded.getInstance().initialize(
                new DefaultTaskProvider(),
                new RealServerContext(event.getServer()),
                config,
                Clock.system(config.getTimezone()),
                new DefaultSchedulerFactory()
            );
        }

        @SubscribeEvent
        public void onServerTick(ServerTickEvent.Post event) {
            if (server == null) return;
            AutoRestartReloaded.getInstance().onServerTick(server.getAverageTickTimeNanos());
        }

        @SubscribeEvent
        public void onServerStopped(ServerStoppedEvent event) throws IOException {
            this.server = null;
            AutoRestartReloaded.LOGGER.debug("Server stopped event!");
            AutoRestartReloaded.getInstance().onServerStopped();
        }
    }
}
//?}
