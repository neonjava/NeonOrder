package me.neonjava.neonorder.gui;

import me.neonjava.neonorder.load.NeonOrder;
import me.neonjava.neonorder.obj.Order;
import me.neonjava.neonorder.utils.ConvertUtils;
import me.neonjava.neonorder.utils.PlayerUtils;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class DeliveryConfirmGUI implements InventoryHolder {
    private final Player player;
    private final Order order;
    private final int amount;
    private final List<ItemStack> items;
    private final Inventory inventory;
    private boolean finished;

    public static void open(Player player, Order order, int amount, List<ItemStack> items) {
        new DeliveryConfirmGUI(player, order, amount, items).open();
    }

    private DeliveryConfirmGUI(Player player, Order order, int amount, List<ItemStack> items) {
        this.player = player;
        this.order = order;
        this.amount = amount;
        this.items = items;
        this.inventory = Bukkit.createInventory(this, 27, NeonOrder.plugin.mm.deserialize(NeonOrder.plugin.getConfigs().getConfirmDeliveryTitle()));
        render();
    }

    private void render() {
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, filler);

        ItemStack display = ConvertUtils.parseOrder(order, NeonOrder.plugin.getConfigs().getOrderLore());
        display.editMeta(meta -> {
            List<net.kyori.adventure.text.Component> lore = meta.lore() == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(meta.lore());
            lore.add(net.kyori.adventure.text.Component.empty());
            lore.add(NeonOrder.plugin.mm.deserialize("<gray>Delivering: <white>" + ConvertUtils.formatNumber(amount)).decoration(TextDecoration.ITALIC, false));
            lore.add(NeonOrder.plugin.mm.deserialize("<gray>You earn: <green>$" + ConvertUtils.formatNumber(amount * order.getMoneyPer())).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
        });

        inventory.setItem(11, item(Material.RED_WOOL, "<red><bold>Cancel", "<gray>Return your items."));
        inventory.setItem(13, display);
        inventory.setItem(15, item(Material.LIME_WOOL, "<green><bold>Confirm Delivery", "<gray>Deliver items and receive money."));
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

    public void handle(int rawSlot, Player clicker) {
        if (!clicker.getUniqueId().equals(player.getUniqueId())) return;
        if (rawSlot == 11) {
            finished = true;
            player.closeInventory();
            PlayerUtils.give(player, items, false);
            return;
        }
        if (rawSlot == 15) {
            finished = true;
            player.closeInventory();
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_MINECART_RIDING, 1.0f, 1.0f);

            final int[] step = {0};
            player.getScheduler().runAtFixedRate(NeonOrder.plugin, task -> {
                if (!player.isOnline()) {
                    task.cancel();
                    PlayerUtils.give(player, items, true);
                    return;
                }

                if (step[0] >= 16) {
                    task.cancel();
                    player.sendActionBar(net.kyori.adventure.text.Component.empty());
                    order.deliver(player, items);
                    return;
                }

                String dots = ".".repeat((step[0] % 4) + 1);
                player.sendActionBar(NeonOrder.plugin.mm.deserialize("<gold><bold>Delivering order" + dots));
                step[0]++;
            }, null, 1L, 5L);
        }
    }

    public void handleClose(Player closer) {
        if (finished || !closer.getUniqueId().equals(player.getUniqueId())) return;
        finished = true;
        PlayerUtils.give(player, items, false);
    }

    private void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
