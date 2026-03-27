package com.github.ajsnarr98.autorestartreloaded.fabric;

//? fabric {

/*import com.github.ajsnarr98.autorestartreloaded.AutoRestartReloaded;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;

public class FabricRestartCommand implements Command<CommandSourceStack> {
    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        AutoRestartReloaded.getInstance().onManualRestartCommand();
        return Command.SINGLE_SUCCESS;
    }
}

*///?}
