package me.neonjava.neonorder.gui;

import me.neonjava.neonorder.data.ConfigCache;
import me.neonjava.neonorder.load.NeonOrder;
import me.neonjava.neonorder.obj.Order;
import me.neonjava.neonorder.utils.ConvertUtils;
import me.neonjava.neonorder.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class YourOrderGUI implements InventoryHolder {
    private static final int PAGE_SIZE = 18; // 2 rows for items, last row for controls (total 3 rows = 27 slots)
    private static final int[] LIMIT_TIERS = {100, 50, 25, 10, 5, 3, 1};
    private static final int DEFAULT_LIMIT = 5;

    private static NeonOrder plugin;

    private final Player player;
    private final int page;
    private final List<Order> orders;
    private final Inventory inventory;

    public static void init(NeonOrder pl) {
        plugin = pl;
    }

    public static int getLimit(Player p) {
        for (int tier : LIMIT_TIERS) {
            if (p.hasPermission("neonorder.limit." + tier)) return tier;
        }
        return DEFAULT_LIMIT;
    }

    public static boolean isAtLimit(Player p) {
        UUID uuid = p.getUniqueId();
        long active = plugin.getDataCache().getOrders(uuid).stream().filter(Order::isActive).count();
        return active >= getLimit(p);
    }

    public static void open(Player p) {
        open(p, 0);
    }

    public static void open(Player p, int page) {
        new YourOrderGUI(p, page).open();
    }

    private YourOrderGUI(Player player, int page) {
        this.player = player;
        this.orders = plugin.getDataCache().getOrders(player.getUniqueId());
        this.page = Math.max(0, Math.min(page, pageCount() - 1));

        ConfigCache cache = plugin.getConfigs();
        // 3 rows total (27 slots): first 2 rows (18 slots) for items, last row (9 slots) for controls
        this.inventory = Bukkit.createInventory(this, 27, plugin.mm.deserialize(cache.getYoGuiTitle()));
        render();
    }

    private int pageCount() {
        return Math.max(1, (orders.size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private void render() {
        inventory.clear();

        ConfigCache cache = plugin.getConfigs();
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, orders.size());
        for (int i = start; i < end; i++) {
            inventory.setItem(i - start, ConvertUtils.parseOrder(orders.get(i), cache.getYoLore()));
        }

        // Map control buttons to last row (slots 18..26)
        if (page > 0) inventory.setItem(18, item(Material.ARROW, "<yellow>Back Page", "<gray>Go to page " + page));
        inventory.setItem(19, item(Material.BOOK, "<aqua>Your Orders", "<gray>Page <white>" + (page + 1) + "<gray>/<white>" + pageCount(), "<gray>Limit <white>" + activeCount() + "<gray>/<white>" + getLimit(player)));
        inventory.setItem(22, isAtLimit(player)
                ? item(Material.BARRIER, "<red>Order Limit Reached", "<gray>Cancel an active order first.")
                : item(Material.MAP, "<green>Create New Order", "<gray>Change item to select from DB.", "<gray>Click to set amount and price."));
        inventory.setItem(25, item(Material.CHEST, "<aqua>All Orders", "<gray>Back to public orders."));
        if (page + 1 < pageCount()) inventory.setItem(26, item(Material.ARROW, "<yellow>Next Page", "<gray>Go to page " + (page + 2)));
    }

    private long activeCount() {
        return orders.stream().filter(Order::isActive).count();
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(plugin.mm.deserialize(name).decoration(TextDecoration.ITALIC, false));
            meta.lore(java.util.Arrays.stream(lore)
                    .map(line -> plugin.mm.deserialize(line).decoration(TextDecoration.ITALIC, false))
                    .toList());
        });
        return item;
    }

    public void handle(int rawSlot, Player clicker) {
        if (!clicker.getUniqueId().equals(player.getUniqueId())) return;
        if (rawSlot < 0 || rawSlot >= inventory.getSize()) return;

        if (rawSlot < PAGE_SIZE) {
            int orderIndex = page * PAGE_SIZE + rawSlot;
            if (orderIndex >= 0 && orderIndex < orders.size()) {
                EditOrderGUI.open(player, orders.get(orderIndex));
            }
            return;
        }

        if (rawSlot == 18 && page > 0) {
            open(player, page - 1);
            return;
        }
        if (rawSlot == 22 && !isAtLimit(player)) {
            int protocol = me.neonjava.neonorder.utils.PlayerUtils.getPlayerProtocolVersion(player);
            if (NeonOrder.hasDialogAPI && !me.neonjava.neonorder.utils.BedrockUtils.isBedrock(player) && protocol >= 772) {
                PlayerUtils.closeInv(player);
            }
            NewOrderDialog.start(player);
            return;
        }
        if (rawSlot == 25) {
            new MainGUI(player, 0);
            return;
        }
        if (rawSlot == 26 && page + 1 < pageCount()) {
            open(player, page + 1);
        }
    }

    private void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
