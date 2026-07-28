package me.neonjava.neonorder.listener;

import me.neonjava.neonorder.gui.MainGUI;
import me.neonjava.neonorder.gui.OrderConfigMenu;
import me.neonjava.neonorder.gui.YourOrderGUI;
import me.neonjava.neonorder.gui.EditOrderGUI;
import me.neonjava.neonorder.gui.CollectItemsGUI;
import me.neonjava.neonorder.gui.DeliveryConfirmGUI;
import me.neonjava.neonorder.gui.ChooseItemGUI;
import me.neonjava.neonorder.gui.OrderEnchantGUI;
import me.neonjava.neonorder.gui.OrderEnchantLevelGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class BedrockMenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player))
            return;
        if (e.getInventory().getHolder() instanceof MainGUI menu) {
            e.setCancelled(true);
            menu.handle(e.getRawSlot(), player, e.getClick());
            return;
        }
        if (e.getInventory().getHolder() instanceof MainGUI.DeliveryMenu) {
            return;
        }
        if (e.getInventory().getHolder() instanceof YourOrderGUI menu) {
            e.setCancelled(true);
            menu.handle(e.getRawSlot(), player);
            return;
        }
        if (e.getInventory().getHolder() instanceof EditOrderGUI menu) {
            e.setCancelled(true);
            menu.handle(e.getRawSlot(), player);
            return;
        }
        if (e.getInventory().getHolder() instanceof CollectItemsGUI menu) {
            e.setCancelled(true);
            menu.handle(e.getRawSlot(), player, e.getClick());
            return;
        }
        if (e.getInventory().getHolder() instanceof DeliveryConfirmGUI menu) {
            e.setCancelled(true);
            menu.handle(e.getRawSlot(), player);
            return;
        }
        if (e.getInventory().getHolder() instanceof ChooseItemGUI menu) {
            e.setCancelled(true);
            menu.handle(e.getRawSlot(), player, e.getClick());
            return;
        }
        if (e.getInventory().getHolder() instanceof OrderConfigMenu menu) {
            e.setCancelled(true);
            menu.handle(e.getRawSlot(), e.isRightClick(), e.isShiftClick());
            return;
        }
        if (e.getInventory().getHolder() instanceof OrderEnchantGUI menu) {
            e.setCancelled(true);
            menu.handle(e.getRawSlot(), e.isShiftClick());
            return;
        }
        if (e.getInventory().getHolder() instanceof OrderEnchantLevelGUI menu) {
            e.setCancelled(true);
            menu.handle(e.getRawSlot());
            return;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player))
            return;
        if (e.getInventory().getHolder() instanceof MainGUI.DeliveryMenu menu) {
            menu.handleClose(player);
            return;
        }
        if (e.getInventory().getHolder() instanceof DeliveryConfirmGUI menu) {
            menu.handleClose(player);
        }
    }
}
