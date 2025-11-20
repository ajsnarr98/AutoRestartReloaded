package com.github.ajsnarr98.autorestartmod;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;

public class RestartCommand implements Command<CommandSourceStack> {
    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        RestartProcessor.triggerRestartForCommand(context.getSource().getServer().getTickCount());
        return Command.SINGLE_SUCCESS;
    }

}
