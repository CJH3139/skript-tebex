package com.github.cjh3139.skripttebex.command;

import com.github.cjh3139.skripttebex.SkriptTebex;
import com.github.cjh3139.skripttebex.config.TebexConfig;
import com.github.cjh3139.skripttebex.gui.StoreGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

public class BuyCommand extends Command {
    public BuyCommand() {
        super("buy");

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Only players can open the store.", NamedTextColor.RED));
                return;
            }

            TebexConfig config = SkriptTebex.getInstance().config();
            if (!config.buyGuiEnabled()) {
                player.sendMessage(Component.text("The store is currently disabled.", NamedTextColor.RED));
                return;
            }

            StoreGui.open(player);
        });
    }
}
