package com.github.cjh3139.skripttebex.elements.events;

import ch.njol.skript.registrations.EventValues;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;

public class TebexQueueErrorEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    static {
        EventValues.registerEventValue(EventValue.builder(TebexQueueErrorEvent.class, String.class)
            .patterns("reason")
            .getter(TebexQueueErrorEvent::getReason)
            .build());
    }

    private final String reason;

    public TebexQueueErrorEvent(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
