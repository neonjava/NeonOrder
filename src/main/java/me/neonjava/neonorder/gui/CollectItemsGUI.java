package me.neonjava.neonorder.gui;

import me.neonjava.neonorder.load.NeonOrder;
import me.neonjava.neonorder.obj.Order;
import me.neonjava.neonorder.utils.PlayerUtils;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class CollectItemsGUI implements InventoryHolder {
    private static final int PAGE_SIZE = 45;

    private final Player player;
    private final Order order;
    private final int page;
    private final Inventory inventory;

    public static void open(Player p, Order order, int page) {
        new CollectItemsGUI(p, order, page).open();
    }

    private CollectItemsGUI(Player player, Order order, int page) {
        this.player = player;
        this.order = order;
        this.page = Math.max(0, Math.min(page, pageCount(order) - 1));
        this.inventory = Bukkit.createInventory(this, 54, NeonOrder.plugin.mm.deserialize("<black>ORDERS -> Collect Items"));
        render();
    }

    private static int stackCount(Order order) {
        int maxStack = Math.max(1, order.getItem().getMaxStackSize());
        return Math.max(0, (order.getInStorage() + maxStack - 1) / maxStack);
    }

    private static int pageCount(Order order) {
        return Math.max(1, (stackCount(order) + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private void render() {
        inventory.clear();

        int maxStack = Math.max(1, order.getItem().getMaxStackSize());
        int startStack = page * PAGE_SIZE;
        int endStack = Math.min(startStack + PAGE_SIZE, stackCount(order));
        for (int stackIndex = startStack; stackIndex < endStack; stackIndex++) {
            int remaining = order.getInStorage() - (stackIndex * maxStack);
            int amount = Math.min(maxStack, remaining);
            if (amount <= 0) break;

            ItemStack stack = order.getItem().clone();
            stack.setAmount(amount);
            inventory.setItem(stackIndex - startStack, stack);
        }

        inventory.setItem(45, item(Material.RED_STAINED_GLASS_PANE, "<red>Back"));
        if (page > 0) inventory.setItem(48, item(Material.ARROW, "<yellow>Previous Page"));
        inventory.setItem(49, item(Material.DROPPER, "<green>Drop This Page", "<gray>Drops every visible stack on the ground.", "<gray>Stored: <white>" + order.getInStorage()));
        if (page + 1 < pageCount(order)) inventory.setItem(53, item(Material.ARROW, "<yellow>Next Page"));
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(NeonOrder.plugin.mm.deserialize(name).decoration(TextDecoration.ITALIC, false));
            meta.lore(java.util.Arrays.stream(lore)
                    .map(line -> NeonOrder.plugin.mm.deserialize(line).decoration(TextDecoration.ITALIC, false))
                    .toList());
        });
        return item;
    }

    private static final java.util.Map<java.util.UUID, Long> dropCooldowns = new java.util.concurrent.ConcurrentHashMap<>();

    public void handle(int rawSlot, Player clicker, org.bukkit.event.inventory.ClickType click) {
        if (!clicker.getUniqueId().equals(player.getUniqueId())) return;
        if (rawSlot < 0 || rawSlot >= inventory.getSize()) return;

        if (rawSlot < PAGE_SIZE) {
            ItemStack clicked = inventory.getItem(rawSlot);
            if (clicked != null && !clicked.isEmpty()) {
                if (click == org.bukkit.event.inventory.ClickType.DROP || click == org.bukkit.event.inventory.ClickType.CONTROL_DROP) {
                    drop(clicked.getAmount());
                } else {
                    collect(clicked.getAmount());
                }
            }
            return;
        }

        if (rawSlot == 45) {
            EditOrderGUI.open(player, order);
            return;
        }
        if (rawSlot == 48 && page > 0) {
            open(player, order, page - 1);
            return;
        }
        if (rawSlot == 49) {
            long lastDrop = dropCooldowns.getOrDefault(player.getUniqueId(), 0L);
            if (System.currentTimeMillis() - lastDrop < 5000) {
                player.sendRichMessage("<red>You are dropping items too fast. Wait 5 seconds.");
                return;
            }
            dropCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            dropVisiblePage();
            return;
        }
        if (rawSlot == 53 && page + 1 < pageCount(order)) {
            open(player, order, page + 1);
        }
    }

    private void dropVisiblePage() {
        int total = 0;
        for (int i = 0; i < PAGE_SIZE; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && !item.isEmpty()) total += item.getAmount();
        }
        drop(total);
    }

    private void drop(int amount) {
        if (amount <= 0) return;
        NeonOrder.plugin.getStorage().collectItems(order, amount).thenAccept(success -> {
            if (Boolean.TRUE.equals(success)) {
                player.getScheduler().run(NeonOrder.plugin, task -> {
                    if (!player.isOnline()) return;
                    PlayerUtils.drop(player, PlayerUtils.createStacks(order.getItem(), amount));
                    PlayerUtils.playSound(player, NeonOrder.plugin.getConfigs().getRefreshSound());
                    if (order.getInStorage() <= 0) {
                        EditOrderGUI.open(player, order);
                    } else {
                        CollectItemsGUI.open(player, order, page);
                    }
                }, null);
            }
        });
    }

    private void collect(int amount) {
        if (amount <= 0) return;
        order.collect(amount);
        player.getScheduler().run(NeonOrder.plugin, task -> {
            PlayerUtils.playSound(player, NeonOrder.plugin.getConfigs().getRefreshSound());
            if (order.getInStorage() <= 0) {
                EditOrderGUI.open(player, order);
            } else {
                CollectItemsGUI.open(player, order, page);
            }
        }, null);
    }

    private void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
