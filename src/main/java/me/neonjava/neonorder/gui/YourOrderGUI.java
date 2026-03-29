package me.neonjava.neonorder.gui;

import com.github.stefvanschie.inventoryframework.adventuresupport.ComponentHolder;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import me.neonjava.neonorder.data.ConfigCache;
import me.neonjava.neonorder.load.NeonOrder;
import me.neonjava.neonorder.obj.Order;
import me.neonjava.neonorder.utils.ConvertUtils;
import me.neonjava.neonorder.utils.PlayerUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

public class YourOrderGUI {
    private static NeonOrder plugin;

    public static void init(NeonOrder pl) {
        plugin = pl;
    }

    // ── Permission-based limit ─────────────────────────────────────────────────
    private static final int[] LIMIT_TIERS = { 100, 50, 25, 10, 5, 3, 1 };
    private static final int DEFAULT_LIMIT = 3;

    public static int getLimit(Player p) {
        for (int tier : LIMIT_TIERS) {
            if (p.hasPermission("neonorder.limit." + tier))
                return tier;
        }
        return DEFAULT_LIMIT;
    }

    public static boolean isAtLimit(Player p) {
        UUID uuid = p.getUniqueId();
        long active = plugin.getDataCache().getOrders(uuid).stream()
                .filter(Order::isActive)
                .count();
        return active >= getLimit(p);
    }

    // ── GUI rendering ──────────────────────────────────────────────────────────
    public static void open(Player p) {
        open(p, 0);
    }

    public static void open(Player p, int page) {
        final UUID pUUID = p.getUniqueId();
        final List<Order> orders = plugin.getDataCache().getOrders(pUUID);
        final ConfigCache cache = plugin.getConfigs();
        final MiniMessage mm = plugin.mm;

        final int pageSize = 45;
        final int totalPages = Math.max(1, (int) Math.ceil(orders.size() / (double) pageSize));
        final int clampedPage = Math.max(0, Math.min(page, totalPages - 1));

        final ChestGui gui = new ChestGui(6, ComponentHolder.of(mm.deserialize(cache.getYoGuiTitle())));
        gui.setOnGlobalClick(e -> e.setCancelled(true));
        gui.setOnGlobalDrag(e -> e.setCancelled(true));

        // Items pane (rows 0-4, all 9 cols)
        final OutlinePane ordersPane = new OutlinePane(0, 0, 9, 5);
        final List<String> rawLore = cache.getYoLore();

        int start = clampedPage * pageSize;
        int end = Math.min(start + pageSize, orders.size());

        for (int i = start; i < end; i++) {
            Order order = orders.get(i);
            ordersPane.addItem(ConvertUtils.parseOrder(order, rawLore, e -> {
                PlayerUtils.closeInv(p);
                ManageOrderDialog.show(order, p);
            }));
        }

        // Show "New Order" button only if player is under their limit
        int limit = getLimit(p);
        long activeCount = orders.stream().filter(Order::isActive).count();
        if (activeCount < limit) {
            ordersPane.addItem(ConvertUtils.parseButton(cache.getNewOrderButton(), e -> {
                NewOrderDialog.start(p);
            }));
        }

        gui.addPane(ordersPane);

        // Bottom navigation bar (row 5)
        StaticPane bar = new StaticPane(0, 5, 9, 1);

        // Previous page arrow
        if (clampedPage > 0) {
            ItemStack prev = makeNavItem(Material.ARROW, "§e« Previous Page",
                    "§7Page " + clampedPage + " of " + totalPages);
            final int prevPage = clampedPage - 1;
            bar.addItem(new GuiItem(prev, e -> open(p, prevPage)), 0, 0);
        } else {
            bar.addItem(new GuiItem(makeNavItem(Material.GRAY_STAINED_GLASS_PANE, " ", ""), e -> {
            }), 0, 0);
        }

        // Limit indicator (center)
        ItemStack info = makeNavItem(Material.PAPER,
                "§b§lYour Orders",
                "§7Active: §f" + activeCount + " §7/ §f" + limit,
                "§7Page: §f" + (clampedPage + 1) + " §7of §f" + totalPages);
        bar.addItem(new GuiItem(info, e -> {
        }), 4, 0);

        // Next page arrow
        if (clampedPage + 1 < totalPages) {
            ItemStack next = makeNavItem(Material.ARROW, "§eNext Page »",
                    "§7Page " + (clampedPage + 2) + " of " + totalPages);
            final int nextPage = clampedPage + 1;
            bar.addItem(new GuiItem(next, e -> open(p, nextPage)), 8, 0);
        } else {
            bar.addItem(new GuiItem(makeNavItem(Material.GRAY_STAINED_GLASS_PANE, " ", ""), e -> {
            }), 8, 0);
        }

        gui.addPane(bar);
        PlayerUtils.openGui(p, gui);
    }

    private static ItemStack makeNavItem(Material mat, String name, String... lore) {
        ItemStack is = new ItemStack(mat);
        ItemMeta meta = is.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text(name));
            List<net.kyori.adventure.text.Component> l = new java.util.ArrayList<>();
            for (String s : lore)
                l.add(net.kyori.adventure.text.Component.text(s));
            meta.lore(l);
            is.setItemMeta(meta);
        }
        return is;
    }
}
