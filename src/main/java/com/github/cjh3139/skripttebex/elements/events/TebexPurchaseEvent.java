package com.github.cjh3139.skripttebex.elements.events;

import ch.njol.skript.registrations.EventValues;
import com.github.cjh3139.skripttebex.api.model.Package;
import com.github.cjh3139.skripttebex.api.model.QueuedCommand;
import com.github.cjh3139.skripttebex.api.model.QueuedPlayer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;

import java.util.List;

public class TebexPurchaseEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    static {
        EventValues.registerEventValue(EventValue.simple(TebexPurchaseEvent.class, Player.class,
            TebexPurchaseEvent::getPlayer));
        EventValues.registerEventValue(EventValue.builder(TebexPurchaseEvent.class, QueuedPlayer.class)
            .patterns("tebex player")
            .getter(TebexPurchaseEvent::getTebexPlayer)
            .build());
        EventValues.registerEventValue(EventValue.builder(TebexPurchaseEvent.class, Package.class)
            .patterns("package")
            .getter(TebexPurchaseEvent::getPackage)
            .build());
        EventValues.registerEventValue(EventValue.builder(TebexPurchaseEvent.class, Number.class)
            .patterns("payment id")
            .getter(TebexPurchaseEvent::getPaymentId)
            .build());
        EventValues.registerEventValue(EventValue.builder(TebexPurchaseEvent.class, QueuedCommand[].class)
            .patterns("tebex commands")
            .getter(event -> event.getCommands().toArray(QueuedCommand[]::new))
            .build());
    }

    private final int paymentId;
    private final QueuedPlayer player;
    private final Package pkg;
    private final List<QueuedCommand> commands;

    public TebexPurchaseEvent(int paymentId, QueuedPlayer player, Package pkg, List<QueuedCommand> commands) {
        this.paymentId = paymentId;
        this.player = player;
        this.pkg = pkg;
        this.commands = commands;
    }

    public int getPaymentId() {
        return paymentId;
    }

    public Player getPlayer() {
        if (player == null) return null;
        return MinecraftServer.getConnectionManager().getOnlinePlayers().stream()
            .filter(online -> online.getUuid().toString().replace("-", "").equalsIgnoreCase(player.uuid())
                || online.getUsername().equalsIgnoreCase(player.name()))
            .findFirst()
            .orElse(null);
    }

    public QueuedPlayer getTebexPlayer() {
        return player;
    }

    public Package getPackage() {
        return pkg;
    }

    public List<QueuedCommand> getCommands() {
        return commands;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
