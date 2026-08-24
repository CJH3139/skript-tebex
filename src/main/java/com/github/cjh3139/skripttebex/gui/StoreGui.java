package com.github.cjh3139.skripttebex.gui;

import com.github.cjh3139.skripttebex.SkriptTebex;
import com.github.cjh3139.skripttebex.TebexService;
import com.github.cjh3139.skripttebex.api.model.Category;
import com.github.cjh3139.skripttebex.config.TebexConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import java.util.ArrayList;
import java.util.List;

public final class StoreGui {
    private StoreGui() {
    }

    public static void open(Player player) {
        TebexService service = TebexService.get();
        if (service == null || service.headlessApi() == null) {
            player.sendMessage(Component.text("The store listing is unavailable.", NamedTextColor.RED));
            return;
        }

        List<Category> categories = service.cache().categories();
        if (categories.isEmpty()) {
            player.sendMessage(Component.text("The store listing is unavailable.", NamedTextColor.RED));
            return;
        }

        TebexConfig config = SkriptTebex.getInstance().config();
        int rows = GuiLayout.clampRows(config.buyGuiRows());
        InventoryType type = GuiLayout.inventoryTypeForRows(rows);
        int perPage = GuiLayout.itemsPerPage(rows);

        Inventory inventory = new Inventory(type, MiniMessage.miniMessage().deserialize(config.buyGuiTitle()));
        int[] page = {0};

        render(inventory, categories, page[0], rows, perPage);

        GuiSessions.track(inventory, (clickedInventory, clickPlayer, slot) -> {
            if (slot == GuiLayout.previousSlot(rows)) {
                if (page[0] > 0) {
                    page[0]--;
                    render(clickedInventory, categories, page[0], rows, perPage);
                }
            } else if (slot == GuiLayout.nextSlot(rows)) {
                if ((page[0] + 1) * perPage < categories.size()) {
                    page[0]++;
                    render(clickedInventory, categories, page[0], rows, perPage);
                }
            } else if (slot == GuiLayout.backSlot(rows)) {
                clickPlayer.closeInventory();
            } else if (slot < perPage) {
                List<Category> pageItems = GuiItems.paginate(categories, page[0], perPage);
                if (slot < pageItems.size()) {
                    CategoryGui.open(clickPlayer, pageItems.get(slot));
                }
            }
        });

        player.openInventory(inventory);
    }

    private static void render(AbstractInventory inventory, List<Category> categories, int page, int rows, int perPage) {
        inventory.clear();
        List<Category> pageItems = GuiItems.paginate(categories, page, perPage);
        for (int i = 0; i < pageItems.size(); i++) {
            inventory.setItemStack(i, categoryItem(pageItems.get(i)));
        }
        GuiLayout.fillPagingRow(inventory, rows, page, perPage, categories.size(), "Close");
    }

    private static ItemStack categoryItem(Category category) {
        String name = category.name() != null ? category.name() : "Category";
        List<Component> lore = new ArrayList<>();
        for (String line : GuiItems.stripHtml(category.description()).split("\n")) {
            if (!line.isBlank()) {
                lore.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
        }
        return ItemStack.of(Material.CHEST)
            .withCustomName(Component.text(name, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
            .withLore(lore);
    }
}
