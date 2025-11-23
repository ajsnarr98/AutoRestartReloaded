package com.github.ajsnarr98.autorestartreloaded.neoforge;

//? neoforge {
import com.github.ajsnarr98.autorestartreloaded.AutoRestartReloaded;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;

public class NeoforgeRestartCommand implements Command<CommandSourceStack> {
    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        AutoRestartReloaded.getInstance().onManualRestartCommand();
        return Command.SINGLE_SUCCESS;
    }
}
//?}
