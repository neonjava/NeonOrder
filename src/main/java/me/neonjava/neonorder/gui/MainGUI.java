package me.neonjava.neonorder.gui;

import me.neonjava.neonorder.data.ConfigCache;
import me.neonjava.neonorder.load.NeonOrder;
import me.neonjava.neonorder.obj.Order;
import me.neonjava.neonorder.obj.SlotInfo;
import me.neonjava.neonorder.obj.SortTypes;
import me.neonjava.neonorder.utils.AlgoUtils;
import me.neonjava.neonorder.utils.ConvertUtils;
import me.neonjava.neonorder.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.block.ShulkerBox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static me.neonjava.neonorder.utils.ConvertUtils.ceil_div;

public class MainGUI implements InventoryHolder {
    private static final int ORDERS_PER_PAGE = 45;

    private final Player player;
    private final int sortIdx;
    private final String search;
    private final int pageIdx;
    private final List<Order> orders;
    private final Inventory inventory;

    private static NeonOrder plugin;
    private static ConfigCache cache;
    private static MiniMessage mm;

    public static void init(NeonOrder pl) {
        plugin = pl;
        cache = plugin.getConfigs();
        mm = plugin.mm;
    }

    public MainGUI(Player p, int sortIdx) {
        this(p, sortIdx, "", 0);
    }

    public MainGUI(Player p, int sortIdx, String search) {
        this(p, sortIdx, search, 0);
    }

    private MainGUI(Player p, int sortIdx, String search, int pageIdx) {
        this.player = p;
        this.sortIdx = normalizeSort(sortIdx);
        this.search = search == null ? "" : search;
        this.orders = loadOrders(this.search, cache.getOrdersSortsOrder().get(this.sortIdx));
        this.pageIdx = clampPage(pageIdx, pageCount());
        this.inventory = Bukkit.createInventory(this, 54, mm.deserialize(cache.getMainGuiTitle()));
        render();
        open(p);
    }

    private static int normalizeSort(int sortIdx) {
        final int size = cache.getOrdersSortsOrder().size();
        if (size <= 0) return 0;
        return Math.floorMod(sortIdx, size);
    }

    private static List<Order> loadOrders(String search, SortTypes sortType) {
        final Collection<Order> sorted = plugin.getDataCache().getSortedOrders(sortType);
        final Collection<Order> searched = search == null || search.isBlank()
                ? sorted
                : AlgoUtils.searchOrder(search, sorted);
        return searched.stream().filter(Order::isActive).toList();
    }

    private static int clampPage(int page, int pages) {
        if (pages <= 1) return 0;
        return Math.max(0, Math.min(page, pages - 1));
    }

    private int pageCount() {
        return Math.max(1, ceil_div(orders.size(), ORDERS_PER_PAGE));
    }

    private void render() {
        inventory.clear();

        final int start = pageIdx * ORDERS_PER_PAGE;
        final int end = Math.min(start + ORDERS_PER_PAGE, orders.size());
        for (int i = start; i < end; i++) {
            inventory.setItem(i - start, ConvertUtils.parseOrder(orders.get(i), cache.getOrderLore()));
        }

        setButton(cache.getOrdersBackButton(), pageIdx > 0);
        setButton(cache.getOrdersNextButton(), pageIdx + 1 < pageCount());
        setButton(cache.getRefreshButton(), true);
        setSortButton();
        setButton(cache.getOrdersSearchButton(), true);
        setButton(cache.getYoButton(), true);
    }

    private void setButton(SlotInfo info, boolean enabled) {
        if (!enabled || info.getSlot() < 0) return;
        inventory.setItem(45 + info.getSlot(), createButton(info));
    }

    private void setSortButton() {
        final SlotInfo info = cache.getOrdersSortButton();
        if (info.getSlot() < 0) return;

        final SortTypes sortType = cache.getOrdersSortsOrder().get(sortIdx);
        final List<TagResolver> placeholders = new ArrayList<>(List.of(cache.getSortPlaceholders()));
        placeholders.add(Placeholder.parsed(sortType.getIdentifier(), cache.getSortPrefix() + sortType.getDisplay()));
        inventory.setItem(45 + info.getSlot(), createButton(info, TagResolver.resolver(placeholders)));
    }

    private ItemStack createButton(SlotInfo info, TagResolver... placeholders) {
        final ItemStack item = info.getType().createItemStack();
        item.editMeta(meta -> {
            meta.displayName(mm.deserialize(info.getDisplayName(), placeholders).decoration(TextDecoration.ITALIC, false));
            meta.lore(info.getLore().stream()
                    .map(line -> mm.deserialize(line, placeholders).decoration(TextDecoration.ITALIC, false))
                    .toList());
        });
        return item;
    }

    public void handle(int rawSlot, Player clicker, ClickType clickType) {
        if (!clicker.getUniqueId().equals(player.getUniqueId())) return;
        if (rawSlot < 0 || rawSlot >= inventory.getSize()) return;

        if (rawSlot < ORDERS_PER_PAGE) {
            handleOrderClick(rawSlot, clicker, clickType);
            return;
        }

        final int buttonSlot = rawSlot - 45;
        if (buttonSlot == cache.getOrdersBackButton().getSlot() && pageIdx > 0) {
            openPage(pageIdx - 1);
            PlayerUtils.playSound(player, cache.getPreviousPageSound());
            return;
        }
        if (buttonSlot == cache.getOrdersNextButton().getSlot() && pageIdx + 1 < pageCount()) {
            openPage(pageIdx + 1);
            PlayerUtils.playSound(player, cache.getNextPageSound());
            return;
        }
        if (buttonSlot == cache.getRefreshButton().getSlot()) {
            openPage(pageIdx);
            PlayerUtils.playSound(player, cache.getRefreshSound());
            return;
        }
        if (buttonSlot == cache.getOrdersSortButton().getSlot()) {
            new MainGUI(player, sortIdx + 1, search, 0);
            PlayerUtils.playSound(player, cache.getSortSound());
            return;
        }
        if (buttonSlot == cache.getOrdersSearchButton().getSlot()) {
            player.closeInventory();
            SignGUI.newSession(
                    player,
                    s -> player.getScheduler().run(plugin, t -> new MainGUI(player, sortIdx, s), null),
                    cache.getLines(),
                    cache.getSignBlock()
            );
            return;
        }
        if (buttonSlot == cache.getYoButton().getSlot()) {
            openYourOrders();
        }
    }

    private void handleOrderClick(int slot, Player clicker, ClickType clickType) {
        final int orderIdx = pageIdx * ORDERS_PER_PAGE + slot;
        if (orderIdx < 0 || orderIdx >= orders.size()) return;

        final Order order = orders.get(orderIdx);
        if (clickType == ClickType.RIGHT && clicker.hasPermission("neonorder.admin.edit-orders")) {
            clicker.showDialog(AdminToolGUI.createEditOrder(order));
            return;
        }
        if (player.getUniqueId().equals(order.getOwnerUniqueId())) {
            player.sendRichMessage(cache.getDeliverSelf());
            return;
        }
        new DeliveryMenu(order).open(player);
    }

    private void openYourOrders() {
        YourOrderGUI.open(player);
    }

    private void openPage(int page) {
        new MainGUI(player, sortIdx, search, page);
    }

    private void open(Player p) {
        p.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public static class DeliveryMenu implements InventoryHolder {
        private final Order order;
        private final Inventory inventory;

        private DeliveryMenu(Order order) {
            this.order = order;
            this.inventory = Bukkit.createInventory(this, cache.getDeliverRows() * 9, mm.deserialize(cache.getDeliverTitle()));
        }

        private void open(Player player) {
            player.openInventory(inventory);
        }

        public void handleClose(Player player) {
            final ItemStack comparer = order.getItem();
            int amount = 0;
            final List<ItemStack> items = new ArrayList<>();

            for (final ItemStack item : inventory) {
                if (item == null || item.isEmpty()) continue;
                items.add(item);
                if (AlgoUtils.isSimilar(item, comparer)) {
                    amount += item.getAmount();
                } else if (item.getItemMeta() instanceof BlockStateMeta bsm &&
                           bsm.getBlockState() instanceof ShulkerBox shulkerBox) {
                    for (ItemStack shulkerItem : shulkerBox.getInventory().getContents()) {
                        if (shulkerItem != null && !shulkerItem.isEmpty() && AlgoUtils.isSimilar(shulkerItem, comparer)) {
                            amount += shulkerItem.getAmount();
                        }
                    }
                }
            }

            inventory.clear();

            if (items.isEmpty()) return;
            if (amount == 0) {
                PlayerUtils.give(player, items, false);
                player.getScheduler().run(plugin, t -> player.updateInventory(), null);
                return;
            }

            final int deliveredAmount = amount;
            player.getScheduler().run(plugin, task -> DeliveryConfirmGUI.open(player, order, deliveredAmount, items), null);
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
