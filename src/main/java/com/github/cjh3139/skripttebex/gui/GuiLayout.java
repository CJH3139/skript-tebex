package com.github.cjh3139.skripttebex.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

final class GuiLayout {
    private GuiLayout() {
    }

    static int clampRows(int rows) {
        if (rows < 2) return 2;
        if (rows > 6) return 6;
        return rows;
    }

    static InventoryType inventoryTypeForRows(int rows) {
        return switch (rows) {
            case 2 -> InventoryType.CHEST_2_ROW;
            case 3 -> InventoryType.CHEST_3_ROW;
            case 4 -> InventoryType.CHEST_4_ROW;
            case 5 -> InventoryType.CHEST_5_ROW;
            default -> InventoryType.CHEST_6_ROW;
        };
    }

    static int itemsPerPage(int rows) {
        return (rows - 1) * 9;
    }

    static int previousSlot(int rows) {
        return rows * 9 - 9;
    }

    static int nextSlot(int rows) {
        return rows * 9 - 1;
    }

    static int backSlot(int rows) {
        return rows * 9 - 5;
    }

    static void fillPagingRow(AbstractInventory inventory, int rows, int page, int perPage, int totalItems, String backLabel) {
        if (page > 0) {
            inventory.setItemStack(previousSlot(rows), ItemStack.of(Material.ARROW)
                .withCustomName(Component.text("Previous Page", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false)));
        }
        if ((page + 1) * perPage < totalItems) {
            inventory.setItemStack(nextSlot(rows), ItemStack.of(Material.ARROW)
                .withCustomName(Component.text("Next Page", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false)));
        }
        inventory.setItemStack(backSlot(rows), ItemStack.of(Material.BARRIER)
            .withCustomName(Component.text(backLabel, NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false)));
    }
}
