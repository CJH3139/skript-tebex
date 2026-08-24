package com.github.cjh3139.skripttebex.gui;

import com.github.cjh3139.skripttebex.SkriptTebex;
import com.github.cjh3139.skripttebex.TebexService;
import com.github.cjh3139.skripttebex.api.TebexHeadlessApi;
import com.github.cjh3139.skripttebex.api.model.Category;
import com.github.cjh3139.skripttebex.api.model.Package;
import com.github.cjh3139.skripttebex.config.TebexConfig;
import com.github.cjh3139.skripttebex.util.BasketCheckout;
import com.github.cjh3139.skripttebex.util.BasketReturnUrl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import java.util.ArrayList;
import java.util.List;

public final class CategoryGui {
    private static final CheckoutCooldown COOLDOWN = new CheckoutCooldown();

    private CategoryGui() {
    }

    public static void open(Player player, Category category) {
        TebexService service = TebexService.get();
        if (service == null || service.headlessApi() == null) {
            player.sendMessage(Component.text("The store listing is unavailable.", NamedTextColor.RED));
            return;
        }

        List<Package> packages = category.packagesOrEmpty();
        if (packages.isEmpty()) {
            player.sendMessage(Component.text("This category has no packages available.", NamedTextColor.RED));
            return;
        }

        TebexConfig config = SkriptTebex.getInstance().config();
        int rows = GuiLayout.clampRows(config.buyGuiRows());
        InventoryType type = GuiLayout.inventoryTypeForRows(rows);
        int perPage = GuiLayout.itemsPerPage(rows);

        String categoryName = category.name() != null ? category.name() : "Category";
        Inventory inventory = new Inventory(type, Component.text(categoryName, NamedTextColor.GOLD));
        int[] page = {0};

        render(inventory, packages, page[0], rows, perPage);

        GuiSessions.track(inventory, (clickedInventory, clickPlayer, slot) -> {
            if (slot == GuiLayout.previousSlot(rows)) {
                if (page[0] > 0) {
                    page[0]--;
                    render(clickedInventory, packages, page[0], rows, perPage);
                }
            } else if (slot == GuiLayout.nextSlot(rows)) {
                if ((page[0] + 1) * perPage < packages.size()) {
                    page[0]++;
                    render(clickedInventory, packages, page[0], rows, perPage);
                }
            } else if (slot == GuiLayout.backSlot(rows)) {
                StoreGui.open(clickPlayer);
            } else if (slot < perPage) {
                List<Package> pageItems = GuiItems.paginate(packages, page[0], perPage);
                if (slot < pageItems.size()) {
                    beginCheckout(clickPlayer, service, pageItems.get(slot));
                }
            }
        });

        player.openInventory(inventory);
    }

    private static void render(AbstractInventory inventory, List<Package> packages, int page, int rows, int perPage) {
        inventory.clear();
        List<Package> pageItems = GuiItems.paginate(packages, page, perPage);
        for (int i = 0; i < pageItems.size(); i++) {
            inventory.setItemStack(i, packageItem(pageItems.get(i)));
        }
        GuiLayout.fillPagingRow(inventory, rows, page, perPage, packages.size(), "Back");
    }

    private static ItemStack packageItem(Package pkg) {
        String name = pkg.name() != null ? pkg.name() : "Package";
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(GuiItems.priceLabel(pkg), NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        for (String line : GuiItems.stripHtml(pkg.description()).split("\n")) {
            if (!line.isBlank()) {
                lore.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
        }
        return ItemStack.of(Material.PAPER)
            .withCustomName(Component.text(name, NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false))
            .withLore(lore);
    }

    private static void beginCheckout(Player player, TebexService service, Package pkg) {
        player.closeInventory();

        TebexHeadlessApi headlessApi = service.headlessApi();
        if (headlessApi == null) {
            player.sendMessage(Component.text("Could not start checkout, try again later.", NamedTextColor.RED));
            return;
        }

        if (!COOLDOWN.tryStart(player.getUuid())) {
            player.sendMessage(Component.text("You are checking out too quickly, try again in a moment.",
                NamedTextColor.RED));
            return;
        }

        String usernameId = player.getUuid().toString().replace("-", "");
        String returnUrl = BasketReturnUrl.resolve(service);

        BasketCheckout.checkoutLink(headlessApi, player.getUsername(), usernameId, returnUrl, List.of(pkg.id()))
            .whenComplete((checkout, error) -> {
                if (error != null || checkout == null) {
                    player.sendMessage(Component.text(
                        "Could not start checkout, try again later.", NamedTextColor.RED));
                    return;
                }
                sendCheckoutLink(player, checkout);
            });
    }

    private static void sendCheckoutLink(Player player, String url) {
        player.sendMessage(Component.text("Click to complete your purchase: ", NamedTextColor.GREEN)
            .append(Component.text(url, NamedTextColor.AQUA).clickEvent(ClickEvent.openUrl(url))));
    }
}
