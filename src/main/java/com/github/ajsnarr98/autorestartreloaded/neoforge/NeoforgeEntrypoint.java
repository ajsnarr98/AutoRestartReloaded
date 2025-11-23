package com.github.ajsnarr98.autorestartreloaded.neoforge;

//? neoforge {

import com.github.ajsnarr98.autorestartreloaded.AutoRestartReloaded;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(value = AutoRestartReloaded.MODID, dist = Dist.DEDICATED_SERVER)
public class NeoforgeEntrypoint {

    private ModEventHandler modEventHandler = new ModEventHandler();
    private MainEveentHandler mainEveentHandler = new MainEveentHandler();

    public NeoforgeEntrypoint(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.register(this.modEventHandler);
        NeoForge.EVENT_BUS.register(this.mainEveentHandler);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.SERVER, NeoforgeConfigSpec.SPEC);
    }

    public static class ModEventHandler {
        private final NeoforgeQueuedActionProvider actionProvider = new NeoforgeQueuedActionProvider();

        @SubscribeEvent
        public void loadingConfig(ModConfigEvent.Loading event) {
            AutoRestartReloaded.getInstance().initialize(
                    actionProvider,
                    NeoforgeConfigSpec.readConfig()
            );
        }
    }

    public static class MainEveentHandler {
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
        public void onServerTick(ServerTickEvent.Pre event) {
            AutoRestartReloaded.getInstance().onServerTick(
                    new NeoforgeRunContext(event)
            );
        }
    }
}
//?}
