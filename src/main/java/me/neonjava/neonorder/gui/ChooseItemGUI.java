package me.neonjava.neonorder.gui;

import me.neonjava.neonorder.data.ConfigCache;
import me.neonjava.neonorder.load.NeonOrder;
import me.neonjava.neonorder.obj.SlotInfo;
import me.neonjava.neonorder.obj.SortTypes;
import me.neonjava.neonorder.utils.AlgoUtils;
import me.neonjava.neonorder.utils.ConvertUtils;
import me.neonjava.neonorder.utils.PDCUtils;
import me.neonjava.neonorder.utils.PlayerUtils;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ChooseItemGUI implements InventoryHolder {
    private static final int PAGE_SIZE = 45;

    private static NeonOrder plugin;
    private static MiniMessage mm;
    private static ConfigCache cache;

    private final Player player;
    private final int sortIdx;
    private final int pageIdx;
    private final String search;
    private final List<ItemStack> items;
    private final Inventory inventory;

    public static void init(NeonOrder plugin) {
        ChooseItemGUI.plugin = plugin;
        mm = plugin.mm;
        cache = plugin.getConfigs();
    }

    public static void choose(Player p, int sortIdx, int pageIdx) {
        new ChooseItemGUI(p, sortIdx, pageIdx, "").open();
    }

    public static void choose(Player p, int sortIdx, String search) {
        new ChooseItemGUI(p, sortIdx, 0, search).open();
    }

    private ChooseItemGUI(Player player, int sortIdx, int pageIdx, String search) {
        this.player = player;
        this.sortIdx = normalizeSort(sortIdx);
        this.search = search == null ? "" : search;
        this.items = loadItems(cache.getChooseSortsOrder().get(this.sortIdx), this.search);
        this.pageIdx = clampPage(pageIdx, pageCount());
        this.inventory = Bukkit.createInventory(this, 54, mm.deserialize(cache.getChooseItemTitle()));
        render();
    }

    private static int normalizeSort(int sortIdx) {
        final int size = cache.getChooseSortsOrder().size();
        if (size <= 0) return 0;
        return Math.floorMod(sortIdx, size);
    }

    private static List<ItemStack> loadItems(SortTypes sortType, String search) {
        final Collection<ItemStack> sorted = plugin.getDataCache().getItems(sortType);
        if (search == null || search.isBlank()) return new ArrayList<>(sorted);
        List<ItemStack> filtered = AlgoUtils.searchItem(search, sorted);
        filtered.sort(AlgoUtils.getComparator(sortType));
        return filtered;
    }

    private static int clampPage(int page, int pages) {
        if (pages <= 1) return 0;
        return Math.max(0, Math.min(page, pages - 1));
    }

    private int pageCount() {
        return Math.max(1, (items.size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private void render() {
        inventory.clear();

        final int start = pageIdx * PAGE_SIZE;
        final int end = Math.min(start + PAGE_SIZE, items.size());
        for (int i = start; i < end; i++) {
            inventory.setItem(i - start, items.get(i).clone());
        }

        setButton(cache.getChooseBackButton(), pageIdx > 0);
        setButton(cache.getChooseNextButton(), pageIdx + 1 < pageCount());
        setSortButton();
        setButton(cache.getChooseSearchButton(), true);
    }

    private void setButton(SlotInfo info, boolean enabled) {
        if (!enabled || info.getSlot() < 0) return;
        inventory.setItem(45 + info.getSlot(), createButton(info));
    }

    private void setSortButton() {
        final SlotInfo info = cache.getChooseSortButton();
        if (info.getSlot() < 0) return;

        final SortTypes sortType = cache.getChooseSortsOrder().get(sortIdx);
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

        if (rawSlot < PAGE_SIZE) {
            int itemIdx = pageIdx * PAGE_SIZE + rawSlot;
            if (itemIdx < 0 || itemIdx >= items.size()) return;
            ItemStack selected = items.get(itemIdx).clone();
            selected.setAmount(1);
            if (clickType == ClickType.RIGHT && player.hasPermission("neonorder.admin.blacklist")) {
                if (PDCUtils.isBlacklist(selected.getItemMeta())) return;
                plugin.getStorage().addBlacklist(selected);
                player.sendRichMessage("<green>Item added to blacklist. Reload to take effect.");
                selected.editMeta(PDCUtils::setBlacklist);
                inventory.setItem(rawSlot, selected);
                return;
            }
            NewOrderDialog.newSession(player, selected);
            return;
        }

        final int buttonSlot = rawSlot - 45;
        if (buttonSlot == cache.getChooseBackButton().getSlot() && pageIdx > 0) {
            new ChooseItemGUI(player, sortIdx, pageIdx - 1, search).open();
            PlayerUtils.playSound(player, cache.getPreviousPageSound());
            return;
        }
        if (buttonSlot == cache.getChooseNextButton().getSlot() && pageIdx + 1 < pageCount()) {
            new ChooseItemGUI(player, sortIdx, pageIdx + 1, search).open();
            PlayerUtils.playSound(player, cache.getNextPageSound());
            return;
        }
        if (buttonSlot == cache.getChooseSortButton().getSlot()) {
            new ChooseItemGUI(player, sortIdx + 1, 0, search).open();
            PlayerUtils.playSound(player, cache.getSortSound());
            return;
        }
        if (buttonSlot == cache.getChooseSearchButton().getSlot()) {
            player.closeInventory();
            AnvilInput.open(player, "Search Item", search, s -> {
                player.getScheduler().run(plugin, task -> ChooseItemGUI.choose(player, sortIdx, s), null);
            });
        }
    }

    private void open() {
        if (items.isEmpty()) {
            player.sendRichMessage("<red>No selectable items loaded. Check plugins/NeonOrder/items.db.");
        }
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
