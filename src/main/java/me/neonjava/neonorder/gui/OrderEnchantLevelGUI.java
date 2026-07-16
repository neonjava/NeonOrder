package me.neonjava.neonorder.gui;

import me.neonjava.neonorder.load.NeonOrder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrderEnchantLevelGUI implements InventoryHolder {
    private final Player player;
    private final OrderEnchantGUI parentMenu;
    private final ItemStack baseItem;
    private final Enchantment enchantment;
    private final Inventory inventory;

    public OrderEnchantLevelGUI(Player player, OrderEnchantGUI parentMenu, ItemStack baseItem, Enchantment enchantment) {
        this.player = player;
        this.parentMenu = parentMenu;
        this.baseItem = baseItem;
        this.enchantment = enchantment;
        this.inventory = Bukkit.createInventory(this, 9, Component.text("Select Level"));
        render();
    }

    public void render() {
        inventory.clear();

        int maxLvl = enchantment.getMaxLevel();
        for (int i = 1; i <= maxLvl; i++) {
            ItemStack glass = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
            ItemMeta meta = glass.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("§a§lLevel " + i));
                glass.setItemMeta(meta);
            }
            inventory.setItem(i - 1, glass);
        }

        ItemStack back = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(Component.text("§c§lBack"));
            back.setItemMeta(backMeta);
        }
        inventory.setItem(8, back);
    }

    public void handle(int slot) {
        if (slot == 8) {
            parentMenu.open();
            return;
        }

        int maxLvl = enchantment.getMaxLevel();
        if (slot < 0 || slot >= maxLvl) return;

        int level = slot + 1;
        setEnchantment(baseItem, enchantment, level);
        parentMenu.open();
    }

    private void setEnchantment(ItemStack item, Enchantment enchantment, int level) {
        if (item.getType() == Material.BOOK) {
            item.setType(Material.ENCHANTED_BOOK);
        }
        boolean isBook = item.getType() == Material.ENCHANTED_BOOK;
        
        io.papermc.paper.datacomponent.item.ItemEnchantments component = item.getData(
                isBook ? io.papermc.paper.datacomponent.DataComponentTypes.STORED_ENCHANTMENTS : io.papermc.paper.datacomponent.DataComponentTypes.ENCHANTMENTS
        );

        io.papermc.paper.datacomponent.item.ItemEnchantments.Builder builder = io.papermc.paper.datacomponent.item.ItemEnchantments.itemEnchantments();
        if (component != null) {
            for (Map.Entry<Enchantment, Integer> entry : component.enchantments().entrySet()) {
                if (entry.getKey() != enchantment) {
                    builder.add(entry.getKey(), entry.getValue());
                }
            }
        }
        builder.add(enchantment, level);
        
        item.setData(
                isBook ? io.papermc.paper.datacomponent.DataComponentTypes.STORED_ENCHANTMENTS : io.papermc.paper.datacomponent.DataComponentTypes.ENCHANTMENTS,
                builder.build()
        );
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
