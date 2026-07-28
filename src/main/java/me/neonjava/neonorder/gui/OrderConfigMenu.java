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
        this.baseItem.setAmount(1);
        this.inventory = Bukkit.createInventory(this, 27, Component.text("Orders -> New Order"));
        render();
    }

    public void render() {
        inventory.clear();

        // Slot 10: Cancel / Back (Red Glass Pane)
        inventory.setItem(10, create(Material.RED_STAINED_GLASS_PANE, " "));

        // Slot 12: Set Amount (Stone)
        inventory.setItem(12, create(Material.STONE, "§b§lAmount: §f" + amount,
                "§7Click to enter amount via sign."));

        // Slot 13: Summary / Enchantments (Chest)
        ItemStack display = baseItem.clone();
        if (isEnchantable(baseItem)) {
            display = create(Material.CHEST, "§d§lSelect Enchantments",
                    "§7Click to customize item enchantments.");
        } else {
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("§e§lSelected Item"));
                display.setItemMeta(meta);
            }
        }
        inventory.setItem(13, display);

        // Slot 14: Set Price Per Item (Emerald)
        inventory.setItem(14, create(Material.EMERALD, "§a§lPrice Per Item: §f$" + price,
                "§7Click to enter price per item via sign."));

        // Slot 16: Confirm Order (Lime Glass Pane)
        inventory.setItem(16, create(Material.LIME_STAINED_GLASS_PANE, " "));
    }

    private ItemStack create(Material mat, String name, String... lore) {
        ItemStack is = new ItemStack(mat);
        ItemMeta meta = is.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            List<Component> l = new ArrayList<>();
            for (String s : lore) {
                if (!s.isEmpty()) l.add(Component.text(s));
            }
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
        if (slot == 10) { // Cancel / Back
            player.closeInventory();
            return;
        }

        if (slot == 12) { // Amount
            player.closeInventory();
            List<String> lines = List.of(
                "",
                "^^^^^^^^^^^",
                "Amount",
                ""
            );
            SignGUI.newSession(player, text -> {
                double val = ConvertUtils.formatNumber(text);
                if (val > 0)
                    this.amount = (int) val;
                player.getScheduler().run(NeonOrder.plugin, task -> this.open(), null);
            }, lines, BlockType.OAK_SIGN);
            return;
        }

        if (slot == 14) { // Price
            player.closeInventory();
            List<String> lines = List.of(
                "",
                "^^^^^^^^^^^",
                "Price Per",
                "Item"
            );
            SignGUI.newSession(player, text -> {
                double val = ConvertUtils.formatNumber(text);
                if (val > 0)
                    this.price = val;
                player.getScheduler().run(NeonOrder.plugin, task -> this.open(), null);
            }, lines, BlockType.OAK_SIGN);
            return;
        }

        if (slot == 13 && isEnchantable(baseItem)) {
            new OrderEnchantGUI(player, this, baseItem).open();
            return;
        }

        if (slot == 16) { // Confirm
            player.closeInventory();
            Order.Response res = Order.create(player, baseItem, price, amount);
            if (res == Order.Response.SUCCESS) {
                player.sendRichMessage("<green>Order created successfully!");
            } else if (res == Order.Response.FAIL) {
                player.sendRichMessage("<red>You don't have enough money!");
            } else if (res == Order.Response.LIMIT_REACHED) {
                player.sendRichMessage("<red>You have reached your active order limit!");
            } else {
                player.sendRichMessage("<red>Failed to create order.");
            }
        }
    }

    private boolean isEnchantable(ItemStack item) {
        if (item.getType() == Material.BOOK || item.getType() == Material.ENCHANTED_BOOK) {
            return true;
        }
        ItemStack checkItem = item.clone();
        checkItem.setAmount(1);
        for (org.bukkit.enchantments.Enchantment enchantment : org.bukkit.Registry.ENCHANTMENT) {
            try {
                if (enchantment.canEnchantItem(checkItem)) {
                    return true;
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return false;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
