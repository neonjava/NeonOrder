package me.neonjava.neonorder.gui;

import me.neonjava.neonorder.load.NeonOrder;
import me.neonjava.neonorder.obj.Order;
import me.neonjava.neonorder.utils.ConvertUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class OrderConfigMenu implements InventoryHolder {
    private final Player player;
    private final ItemStack baseItem;
    private int amount = 1;
    private double price = 10.0;
    private final Inventory inventory;

    public OrderConfigMenu(Player player, ItemStack baseItem) {
        this.player = player;
        this.baseItem = baseItem.clone();
        this.inventory = Bukkit.createInventory(this, 27, Component.text("Confirm New Order"));
        render();
    }

    public void render() {
        inventory.clear();

        // Slot 10: Set Amount (Sign Input) - Use the Search Sign Type as requested
        inventory.setItem(11, create(Material.OAK_SIGN, "§b§lSet Amount: §f" + amount,
                "§7Click to enter amount via sign.",
                "§7Format: §e10  10k  10m  1b"));

        // Slot 13: Summary (The Item)
        ItemStack display = baseItem.clone();
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§e§lOrder Review"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Amount: §f" + amount));
            lore.add(Component.text("§7Price/Ea: §f$" + price));
            lore.add(Component.text(" "));
            lore.add(Component.text("§6§lTotal Cost: §e$" + (amount * price)));
            meta.lore(lore);
            display.setItemMeta(meta);
        }
        inventory.setItem(13, display);

        // Slot 15: Set Price (Sign Input)
        inventory.setItem(15, create(Material.GOLD_INGOT, "§6§lSet Price: §f$" + price,
                "§7Click to enter price via sign.",
                "§7Format: §e10  10k  10m  1b"));

        // Slot 22: Confirm (Lime Glass)
        inventory.setItem(22, create(Material.LIME_STAINED_GLASS_PANE, "§a§l✔ Confirm Order",
                "§7Click to place the order."));
    }

    private ItemStack create(Material mat, String name, String... lore) {
        ItemStack is = new ItemStack(mat);
        ItemMeta meta = is.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            List<Component> l = new ArrayList<>();
            for (String s : lore)
                l.add(Component.text(s));
            meta.lore(l);
            is.setItemMeta(meta);
        }
        return is;
    }

    public void open() {
        render();
        player.openInventory(inventory);
    }

    public void handle(int slot) {
        if (slot == 11) { // Amount
            player.closeInventory();
            SignGUI.newSession(player, text -> {
                double val = ConvertUtils.formatNumber(text);
                if (val > 0)
                    this.amount = (int) val;
                Bukkit.getScheduler().runTask(NeonOrder.plugin, this::open);
            }, List.of("Set Amount", "10  10k  10m  1b", "\u2193 type below \u2193", ""), BlockType.OAK_SIGN, 4);
            return;
        }

        if (slot == 15) { // Price
            player.closeInventory();
            SignGUI.newSession(player, text -> {
                double val = ConvertUtils.formatNumber(text);
                if (val > 0)
                    this.price = val;
                Bukkit.getScheduler().runTask(NeonOrder.plugin, this::open);
            }, List.of("Set Price", "10  10k  10m  1b", "\u2193 type below \u2193", ""), BlockType.OAK_SIGN, 4);
            return;
        }

        if (slot == 22) { // Confirm
            player.closeInventory();
            Order.Response res = Order.create(player, baseItem, price, amount);
            if (res == Order.Response.SUCCESS) {
                player.sendRichMessage("<green>Order created successfully!");
            } else if (res == Order.Response.FAIL) {
                player.sendRichMessage("<red>You don't have enough money!");
            } else {
                player.sendRichMessage("<red>Failed to create order.");
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
