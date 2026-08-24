package com.github.cjh3139.skripttebex.gui;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.inventory.AbstractInventory;

import java.util.Map;
import java.util.WeakHashMap;

public final class GuiSessions {
    private static final Map<AbstractInventory, ClickHandler> SESSIONS = new WeakHashMap<>();
    private static boolean registered;

    private GuiSessions() {
    }

    @FunctionalInterface
    interface ClickHandler {
        void onClick(AbstractInventory inventory, Player player, int slot);
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        MinecraftServer.getGlobalEventHandler().addListener(InventoryPreClickEvent.class, event -> {
            AbstractInventory inventory = event.getInventory();
            ClickHandler handler;
            synchronized (GuiSessions.class) {
                handler = SESSIONS.get(inventory);
            }

            if (handler == null) return;
            event.setCancelled(true);

            handler.onClick(inventory, event.getPlayer(), event.getSlot());
        });
    }

    static synchronized void track(AbstractInventory inventory, ClickHandler handler) {
        SESSIONS.put(inventory, handler);
    }
}
