package com.github.ajsnarr98.autorestartreloaded.neoforge;

//? neoforge {

import com.github.ajsnarr98.autorestartreloaded.AutoRestartReloaded;
import com.github.ajsnarr98.autorestartreloaded.core.Config;
import com.github.ajsnarr98.autorestartreloaded.core.servercontext.RealServerContext;
import com.github.ajsnarr98.autorestartreloaded.core.task.DefaultTaskProvider;
import com.github.ajsnarr98.autorestartreloaded.core.task.executer.DefaultSchedulerFactory;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.io.IOException;
import java.time.Clock;

@Mod(value = AutoRestartReloaded.MODID, dist = Dist.DEDICATED_SERVER)
public class NeoforgeEntrypoint {

    //    private ModEventHandler modEventHandler = new ModEventHandler();
    private MainEventHandler mainEventHandler = new MainEventHandler();

    public NeoforgeEntrypoint(IEventBus modEventBus, ModContainer modContainer) {
//        modEventBus.register(this.modEventHandler);
        NeoForge.EVENT_BUS.register(this.mainEventHandler);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.SERVER, NeoforgeConfigSpec.SPEC);
    }

//    public static class ModEventHandler {
//
//    }

    public static class MainEventHandler {

        @SubscribeEvent
        public void onRegisterCommands(RegisterCommandsEvent event) {
            AutoRestartReloaded.LOGGER.debug("registering /restart command");
            event.getDispatcher().register(
                LiteralArgumentBuilder.<CommandSourceStack>literal("restart")
                    .requires(source -> source.hasPermission(4))
                    .executes(new NeoforgeRestartCommand())
            );
        }

        @SubscribeEvent
        public void onServerStarted(ServerStartedEvent event) {
            Config config = NeoforgeConfigSpec.readConfig();
            AutoRestartReloaded.getInstance().initialize(
                new DefaultTaskProvider(),
                new RealServerContext(event.getServer()),
                config,
                Clock.system(config.getTimezone()),
                new DefaultSchedulerFactory()
            );
        }

        @SubscribeEvent
        public void onServerStopped(ServerStoppedEvent event) throws IOException {
            AutoRestartReloaded.LOGGER.debug("Server stopped event!");
            AutoRestartReloaded.getInstance().onServerStopped();
        }
    }
}
//?}
