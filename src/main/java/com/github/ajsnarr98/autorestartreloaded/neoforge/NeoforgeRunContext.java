package com.github.ajsnarr98.autorestartreloaded.neoforge;

//? neoforge {
import com.github.ajsnarr98.autorestartreloaded.core.QueuedAction;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class NeoforgeRunContext implements QueuedAction.RunContext {
    private final ServerTickEvent event;

    public NeoforgeRunContext(ServerTickEvent event) {
        this.event = event;
    }

    public ServerTickEvent getEvent() {
        return event;
    }
}
//?}
