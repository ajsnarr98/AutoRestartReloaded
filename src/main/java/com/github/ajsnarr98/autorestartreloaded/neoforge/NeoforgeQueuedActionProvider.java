package com.github.ajsnarr98.autorestartreloaded.neoforge;

//? neoforge {
import com.github.ajsnarr98.autorestartreloaded.core.QueudActionProvider;
import com.github.ajsnarr98.autorestartreloaded.core.QueuedAction;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class NeoforgeQueuedActionProvider implements QueudActionProvider {

    @Override
    public QueuedAction<? extends QueuedAction.RunContext> newRestartMessageAction(long time, String message) {
        return new NeoforgeConsoleAction(time, "say " + message);
    }

    @Override
    public QueuedAction<? extends QueuedAction.RunContext> newStopAction(long time) {
        return new NeoforgeConsoleAction(time, "stop");
    }

    public static class ServerTickContext implements QueuedAction.RunContext {
        private final ServerTickEvent event;

        public ServerTickContext(ServerTickEvent event) {
            this.event = event;
        }

        public ServerTickEvent getEvent() {
            return event;
        }
    }

    private static class NeoforgeConsoleAction implements QueuedAction<ServerTickContext> {
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
        public void run(ServerTickContext context) {
            CommandSourceStack consoleSource = context.getEvent().getServer().createCommandSourceStack();
            context.getEvent().getServer().getCommands().performPrefixedCommand(consoleSource, command);
        }

        @Override
        public QueuedAction<ServerTickContext> copy(long time) {
            return new NeoforgeConsoleAction(time, command);
        }
    }
}
//?}
