package com.github.cjh3139.skripttebex.elements.events;

import ch.njol.skript.registrations.EventValues;
import com.github.cjh3139.skripttebex.api.model.ServerInformation;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;

public class TebexConnectEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    static {
        EventValues.registerEventValue(EventValue.builder(TebexConnectEvent.class, ServerInformation.class)
            .patterns("store")
            .getter(TebexConnectEvent::getInformation)
            .build());
    }

    private final ServerInformation information;

    public TebexConnectEvent(ServerInformation information) {
        this.information = information;
    }

    public ServerInformation getInformation() {
        return information;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
