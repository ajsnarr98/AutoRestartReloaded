package com.github.ajsnarr98.autorestartreloaded.neoforge;

//? neoforge {
import com.github.ajsnarr98.autorestartreloaded.AutoRestartReloaded;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = AutoRestartReloaded.MODID, dist = Dist.DEDICATED_SERVER)
public class NeoforgeEntrypoint {

    private NeoforgeQueuedActionProvider actionProvider = new NeoforgeQueuedActionProvider();

    public NeoforgeEntrypoint(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.SERVER, NeoforgeConfigSpec.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        AutoRestartReloaded.getInstance().initialize(
                actionProvider,
                NeoforgeConfigSpec.readConfig()
        );
    }
}
//?}
