package com.github.cjh3139.skripttebex.elements.events;

import ch.njol.skript.registrations.EventValues;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;

public class TebexQueueCheckEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    static {
        EventValues.registerEventValue(EventValue.builder(TebexQueueCheckEvent.class, Number.class)
            .patterns("due players")
            .getter(event -> event.getDuePlayerCount())
            .build());
    }

    private final int duePlayerCount;

    public TebexQueueCheckEvent(int duePlayerCount) {
        this.duePlayerCount = duePlayerCount;
    }

    public int getDuePlayerCount() {
        return duePlayerCount;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
