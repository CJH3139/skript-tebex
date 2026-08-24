package com.github.cjh3139.skripttebex;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import com.github.cjh3139.skripttebex.command.BuyCommand;
import com.github.cjh3139.skripttebex.command.TebexCommand;
import com.github.cjh3139.skripttebex.config.TebexConfig;
import com.github.cjh3139.skripttebex.gui.GuiSessions;
import com.github.cjh3139.skripttebex.queue.CommandQueue;
import com.github.cjh3139.skripttebex.registration.Registration;
import com.github.hapily04.skriptminestom.util.FileUtils;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.player.PlayerSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class SkriptTebex extends JavaPlugin {
    private static SkriptTebex instance;

    private final Logger logger = LoggerFactory.getLogger(SkriptTebex.class);
    private File dataFolder;
    private SkriptAddon addonInstance;
    private TebexConfig config;
    private TebexService service;

    @Override
    public void onEnable() {
        instance = this;
        dataFolder = FileUtils.defendFile(new File(FileUtils.getServerDirectory(), "skript-tebex"), true);
        config = TebexConfig.load(dataFolder);

        addonInstance = Skript.registerAddon(this);
        new Registration();
        try {
            addonInstance.loadClasses("com.github.cjh3139.skripttebex", "elements");
        } catch (IOException e) {
            logger.error("Failed to load skript-tebex elements: {}", e.getMessage(), e);
        }

        service = new TebexService(config);
        service.connect();

        MinecraftServer.getCommandManager().register(new TebexCommand());
        if (config.buyGuiEnabled()) {
            MinecraftServer.getCommandManager().register(new BuyCommand());
        }

        GuiSessions.register();

        MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, event -> {
            if (!event.isFirstSpawn()) return;
            CommandQueue queue = service.queue();
            if (queue == null) return;
            queue.onPlayerJoin(event.getPlayer().getUuid().toString().replace("-", ""));
        });
    }

    @Override
    public void onDisable() {
        if (service != null) service.disconnect();
    }

    public File dataFolder() {
        return dataFolder;
    }

    public Logger logger() {
        return logger;
    }

    public TebexConfig config() {
        return config;
    }

    public TebexService service() {
        return service;
    }

    public static SkriptTebex getInstance() {
        return instance;
    }
}
