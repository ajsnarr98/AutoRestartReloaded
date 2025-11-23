package com.github.ajsnarr98.autorestartreloaded.neoforge;

//? neoforge {
import com.github.ajsnarr98.autorestartreloaded.core.QueudActionProvider;
import com.github.ajsnarr98.autorestartreloaded.core.QueuedAction;
import com.github.ajsnarr98.autorestartreloaded.core.TimeUtils;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class NeoforgeQueuedActionProvider implements QueudActionProvider {

    @Override
    public QueuedAction<? extends QueuedAction.RunContext> newRestartMessageAction(long time, String message) {
        return new NeoforgeConsoleAction(time, "say " + message);
    }

    @Override
    public QueuedAction<? extends QueuedAction.RunContext> newStopAction(long time) {
        return new NeoforgeConsoleAction(time, "stop");
    }

    private static class NeoforgeConsoleAction implements QueuedAction<NeoforgeRunContext> {
        private final long time;
        private final String command;

        public NeoforgeConsoleAction(long time, String command) {
            this.time = time;
            this.command = command;
        }

        @Override
        public long getTime() {
            return time;
        }

        @Override
        public void run(NeoforgeRunContext context) {
            CommandSourceStack consoleSource = context.getEvent().getServer().createCommandSourceStack();
            context.getEvent().getServer().getCommands().performPrefixedCommand(consoleSource, command);
        }

        @Override
        public QueuedAction<NeoforgeRunContext> copy(long time) {
            return new NeoforgeConsoleAction(time, command);
        }

        @Override
        public String toString() {
            return String.format("time: %s | command: '%s'", TimeUtils.getHumanReadableTime(this.time), this.command);
        }
    }
}
//?}
