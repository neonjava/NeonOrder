package me.neonjava.neonorder.gui;

import me.neonjava.neonorder.load.NeonOrder;
import me.neonjava.neonorder.obj.Order;
import me.neonjava.neonorder.utils.ConvertUtils;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class EditOrderGUI implements InventoryHolder {
    private final Player player;
    private final Order order;
    private final Inventory inventory;

    public static void open(Player p, Order order) {
        new EditOrderGUI(p, order).open();
    }

    private EditOrderGUI(Player player, Order order) {
        this.player = player;
        this.order = order;
        this.inventory = Bukkit.createInventory(this, 27, NeonOrder.plugin.mm.deserialize("<black>ORDERS -> Edit Order"));
        render();
    }

    private void render() {
        inventory.clear();
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, filler);

        inventory.setItem(10, ConvertUtils.parseOrder(order, NeonOrder.plugin.getConfigs().getYoLore()));
        inventory.setItem(13, item(Material.RED_WOOL, "<red><bold>Cancel Order", "<gray>Click to cancel this order.", "<gray>Unpaid money will be returned."));
        inventory.setItem(16, item(Material.CHEST, "<green><bold>Collect Items", "<gray>Stored: <white>" + order.getInStorage(), "<gray>Click to collect delivered items."));
        inventory.setItem(18, item(Material.RED_STAINED_GLASS_PANE, "<red>Back to Your Orders"));
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
        if (rawSlot == 13) {
            player.closeInventory();
            order.cancel(player);
            return;
        }
        if (rawSlot == 16) {
            CollectItemsGUI.open(player, order, 0);
            return;
        }
        if (rawSlot == 18) {
            YourOrderGUI.open(player);
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
