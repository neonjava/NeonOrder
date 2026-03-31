package me.neonjava.neonorder.gui;

import com.github.stefvanschie.inventoryframework.adventuresupport.ComponentHolder;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import me.neonjava.neonorder.load.NeonOrder;
import me.neonjava.neonorder.obj.Order;
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

public class CollectItemsGUI {

    public static void open(Player p, Order order, int page) {
        MiniMessage mm = NeonOrder.plugin.mm;
        String title = "§0ORDERS --> Collect Items (1/1)";
        ChestGui gui = new ChestGui(6, ComponentHolder.of(mm.deserialize(title)));
        gui.setOnGlobalClick(e -> e.setCancelled(true));

        // Logic for items:
        // We show as many as inStorage, up to 45 per page
        OutlinePane pane = new OutlinePane(0, 0, 9, 5);
        int inStorage = order.inStorage;
        int maxItems = 45;
        int countShowing = Math.min(inStorage, maxItems);

        ItemStack displayItem = order.getItem().clone();
        for (int i = 0; i < countShowing; i++) {
            int stackSize = Math.min(inStorage - (i * 64), 64);
            if (stackSize <= 0)
                break;
            ItemStack clone = displayItem.clone();
            clone.setAmount(stackSize);
            pane.addItem(new GuiItem(clone));
        }
        gui.addPane(pane);

        // Navigation / Bottom Bar
        StaticPane bar = new StaticPane(0, 5, 9, 1);

        // Drop Button (Middle)
        ItemStack dropButton = new ItemStack(Material.DROPPER);
        ItemMeta dm = dropButton.getItemMeta();
        if (dm != null) {
            dm.displayName(mm.deserialize("<gradient:#84fab0:#8fd3f4><bold>Drop Items")
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(mm.deserialize("<gray>Clicking this will drop items").decoration(TextDecoration.ITALIC, false));
            lore.add(mm.deserialize("<gray>on the ground at your location.").decoration(TextDecoration.ITALIC, false));
            lore.add(mm.deserialize("<gray>Currently stored: <white>" + inStorage).decoration(TextDecoration.ITALIC,
                    false));
            dm.lore(lore);
            dropButton.setItemMeta(dm);
        }
        bar.addItem(new GuiItem(dropButton, e -> {
            if (order.inStorage > 0) {
                int amountToCollect = Math.min(order.inStorage, 64);
                NeonOrder.plugin.getStorage().collectItems(order, amountToCollect).thenAccept(success -> {
                    if (Boolean.TRUE.equals(success)) {
                        PlayerUtils.drop(p, PlayerUtils.createStacks(order.getItem(), amountToCollect));
                        // Refresh the GUI
                        p.getScheduler().run(NeonOrder.plugin, (t) -> {
                            open(p, order, page);
                        }, null);
                    }
                });
            }
        }), 4, 0);

        // Back button
        ItemStack back = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta bm = back.getItemMeta();
        if (bm != null) {
            bm.displayName(mm.deserialize("<red>Back").decoration(TextDecoration.ITALIC, false));
            back.setItemMeta(bm);
        }
        bar.addItem(new GuiItem(back, e -> EditOrderGUI.open(p, order)), 0, 0);

        gui.addPane(bar);
        PlayerUtils.openGui(p, gui);
    }
}
