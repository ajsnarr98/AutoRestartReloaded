package com.github.ajsnarr98.autorestartmod;

import com.cronutils.model.Cron;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.TreeSet;

public class RestartProcessor {

    private static RestartProcessor INSTANCE = new RestartProcessor();
    public static RestartProcessor getInstance() {
        return INSTANCE;
    }

    // the next tick at the top of the queue
    private long nextQueueTime = Long.MAX_VALUE;
    private TreeSet<Event> queue = new TreeSet<>();

    /**
     * Parses config and begins the restart scheduling process.
     */
    public void initialize() {
        clearAllEvents();


    }

    private Cron getNextRestartTime() {
        
    }

    private void recalibrateQueueForNewServerTick(long serverTickCount) {
        Instant now = Instant.now();
        long realtimeSecondsDiff = now.getEpochSecond() - startingTime.getEpochSecond();
        long tickDiff = serverTickCount - startingTick;

        long realtimeTickDiff = realtimeSecondsDiff * TPS;
        if (realtimeTickDiff >= tickDiff - ticksErrorMargin && realtimeTickDiff <= tickDiff + ticksErrorMargin) {
            // we are at most off by [ticksErrorMargin], change nothing
            return;
        }

        // recalibrate so that ticks in the schedule line up with the correct time
        long adjustment = tickDiff - realtimeTickDiff;
        TreeSet<Event> newQueue = new TreeSet<>();
        for (Event event : queue) {
            newQueue.add(event.copy(event.getTick() + adjustment));
        }

        // get rid of old queue
        queue = newQueue;
    }

    /**
     * Begins a restart as triggered from a command in chat.
     *
     * @param serverTickCount the current server tick count
     */
    public void triggerRestartForCommand(int serverTickCount) {
        AutoRestartReloaded.LOGGER.info("Scheduling for restart command");
        int localTime = 0;
        for (int i = 5; i > 0; i--) {
            localTime += TPS;
            addEvent(new Event.Message(serverTickCount + localTime, "Restarting server in " + i + " seconds..."));
        }
        localTime += TPS;
        addEvent(new Event.Stop(serverTickCount + localTime));
    }

    public void clearAllEvents() {
        queue.clear();
        nextQueueTime = Long.MAX_VALUE;
    }

    @SubscribeEvent
    public static void serverStartingEvent(ServerStartingEvent event) {
        AutoRestartReloaded.LOGGER.debug("Recalibrating restart schedule for server instance start");
        RestartProcessor.initialize(event.getServer().getTickCount());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        int tick = event.getServer().getTickCount();

        if (nextTick >= 0 && tick >= nextTick) {
            if (queue.isEmpty()) return;
            Event next = queue.last();
            while (nextTick >= 0 && tick >= nextTick) {
                next.run(event);
                queue.remove(next);

                // setup next event
                if (queue.isEmpty()) {
                    nextTick = -1;
                    return;
                }
                next = queue.last();
                nextTick = next.getTick();
            }
        }
    }

    private static void addEvent(Event event) {
        queue.add(event);
        nextTick = queue.last().getTick();
    }

    private interface Event extends Comparable<Event> {

        @Override
        default int compareTo(@NotNull RestartProcessor.Event other) {
            return Long.compare(this.getTime(), other.getTime());
        }

        /**
         * @return time that the event should run at in seconds since epoch
         */
        long getTime();

        void run(ServerTickEvent event);

        /**
         * Copies this event with a new tick value.
         * @param time a new time value.
         * @return a shallow copy of this event with the new tick value.
         */
        Event copy(long time);

        abstract class AbstractConsoleEvent implements Event {
            protected long time;

            public AbstractConsoleEvent(long time) {
                this.time = time;
            }

            @Override
            public long getTime() {
                return time;
            }

            void performPrefixedCommand(ServerTickEvent event, String command) {
                CommandSourceStack consoleSource = event.getServer().createCommandSourceStack();
                event.getServer().getCommands().performPrefixedCommand(consoleSource, command);
            }
        }

        class Stop extends AbstractConsoleEvent {
            Stop(long time) {
                super(time);
            }

            @Override
            public void run(ServerTickEvent event) {
                performPrefixedCommand(event, "stop");
            }

            @Override
            public Event copy(long time) {
                return new Stop(time);
            }
        }

        class Message extends AbstractConsoleEvent {
            private final String message;

            Message(long time, String message) {
                super(time);
                this.message = message;
            }

            @Override
            public void run(ServerTickEvent event) {
                performPrefixedCommand(event, "say " + message);
            }

            @Override
            public Event copy(long time) {
                return new Message(time, message);
            }
        }
    }
}
