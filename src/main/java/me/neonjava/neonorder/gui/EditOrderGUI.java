package me.neonjava.neonorder.gui;

import com.github.stefvanschie.inventoryframework.adventuresupport.ComponentHolder;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import me.neonjava.neonorder.load.NeonOrder;
import me.neonjava.neonorder.obj.Order;
import me.neonjava.neonorder.utils.ConvertUtils;
import me.neonjava.neonorder.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class EditOrderGUI {

    public static void open(Player p, Order order) {
        MiniMessage mm = NeonOrder.plugin.mm;
        String title = "§0ORDERS -> Edit Order";
        ChestGui gui = new ChestGui(3, ComponentHolder.of(mm.deserialize(title)));
        gui.setOnGlobalClick(e -> e.setCancelled(true));

        StaticPane pane = new StaticPane(0, 0, 9, 3);

        // Background
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gm = glass.getItemMeta();
        if (gm != null) {
            gm.displayName(Component.empty());
            glass.setItemMeta(gm);
        }
        pane.fillWith(glass);

        // Slot 10: The Item
        pane.addItem(new GuiItem(ConvertUtils.parseOrder(order, NeonOrder.plugin.getConfigs().getYoLore())), 1, 1);

        // Slot 13: Cancel Order
        ItemStack cancel = new ItemStack(Material.RED_WOOL);
        ItemMeta cm = cancel.getItemMeta();
        if (cm != null) {
            cm.displayName(mm.deserialize("<red><bold>Cancel Order").decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(mm.deserialize("<gray>Click to permanently").decoration(TextDecoration.ITALIC, false));
            lore.add(mm.deserialize("<gray>cancel this order.").decoration(TextDecoration.ITALIC, false));
            cm.lore(lore);
            cancel.setItemMeta(cm);
        }
        pane.addItem(new GuiItem(cancel, e -> {
            order.cancel(p);
            p.closeInventory();
        }), 4, 1);

        // Slot 16: Collect Items
        ItemStack collect = new ItemStack(Material.CHEST);
        ItemMeta colm = collect.getItemMeta();
        if (colm != null) {
            colm.displayName(mm.deserialize("<green><bold>Collect Items").decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(mm.deserialize("<gray>Click to view and").decoration(TextDecoration.ITALIC, false));
            lore.add(mm.deserialize("<gray>collect stored items.").decoration(TextDecoration.ITALIC, false));
            colm.lore(lore);
            collect.setItemMeta(colm);
        }
        pane.addItem(new GuiItem(collect, e -> {
            CollectItemsGUI.open(p, order, 0);
        }), 7, 1);

        // Back button (Bottom Left)
        ItemStack back = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta bm = back.getItemMeta();
        if (bm != null) {
            bm.displayName(mm.deserialize("<red>Back to Orders").decoration(TextDecoration.ITALIC, false));
            back.setItemMeta(bm);
        }
        pane.addItem(new GuiItem(back, e -> YourOrderGUI.open(p)), 0, 2);

        gui.addPane(pane);
        PlayerUtils.openGui(p, gui);
    }
}
