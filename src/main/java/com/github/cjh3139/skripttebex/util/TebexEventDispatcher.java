package com.github.cjh3139.skripttebex.util;

import net.minestom.server.MinecraftServer;
import net.minestom.server.thread.TickThread;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;

public final class TebexEventDispatcher {
    private TebexEventDispatcher() {}

    public static void fireOnTickThread(Event event) {
        if (onTickThread(Thread.currentThread())) {
            Bukkit.getPluginManager().callEvent(event);
            return;
        }
        try {
            MinecraftServer.getSchedulerManager().scheduleNextTick(() -> Bukkit.getPluginManager().callEvent(event));
        } catch (RuntimeException e) {
        }
    }

    static boolean onTickThread(Thread thread) {
        return thread instanceof TickThread;
    }
}
