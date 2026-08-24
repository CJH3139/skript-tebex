package com.github.cjh3139.skripttebex.elements.events;

import ch.njol.skript.registrations.EventValues;
import com.github.cjh3139.skripttebex.api.model.Package;
import com.github.cjh3139.skripttebex.api.model.QueuedCommand;
import com.github.cjh3139.skripttebex.api.model.QueuedPlayer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;

public class TebexCommandEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    static {
        EventValues.registerEventValue(EventValue.simple(TebexCommandEvent.class, Player.class,
            TebexCommandEvent::getPlayer));
        EventValues.registerEventValue(EventValue.builder(TebexCommandEvent.class, String.class)
            .patterns("command")
            .getter(TebexCommandEvent::getCommand)
            .build());
        EventValues.registerEventValue(EventValue.builder(TebexCommandEvent.class, QueuedCommand.class)
            .patterns("tebex command")
            .getter(TebexCommandEvent::getQueuedCommand)
            .build());
        EventValues.registerEventValue(EventValue.builder(TebexCommandEvent.class, Package.class)
            .patterns("package")
            .getter(TebexCommandEvent::getPackage)
            .build());
        EventValues.registerEventValue(EventValue.builder(TebexCommandEvent.class, Number.class)
            .patterns("payment id")
            .getter(event -> event.getQueuedCommand().payment())
            .build());
    }

    private final QueuedCommand command;
    private final QueuedPlayer queuedPlayer;
    private final Package pkg;

    private boolean cancelled;
    private boolean markExecuted = true;

    public TebexCommandEvent(QueuedCommand command, QueuedPlayer queuedPlayer, Package pkg) {
        this.command = command;
        this.queuedPlayer = queuedPlayer;
        this.pkg = pkg;
    }

    public QueuedCommand getQueuedCommand() {
        return command;
    }

    public String getCommand() {
        return command.command();
    }

    public QueuedPlayer getTebexPlayer() {
        return queuedPlayer;
    }

    public Package getPackage() {
        return pkg;
    }

    public Player getPlayer() {
        if (queuedPlayer == null) return null;
        return MinecraftServer.getConnectionManager().getOnlinePlayers().stream()
            .filter(player -> player.getUuid().toString().replace("-", "").equalsIgnoreCase(queuedPlayer.uuid())
                || player.getUsername().equalsIgnoreCase(queuedPlayer.name()))
            .findFirst()
            .orElse(null);
    }

    public boolean isMarkExecuted() {
        return markExecuted;
    }

    public void setMarkExecuted(boolean markExecuted) {
        this.markExecuted = markExecuted;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
