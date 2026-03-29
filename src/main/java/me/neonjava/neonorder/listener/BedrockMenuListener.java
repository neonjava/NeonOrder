package me.neonjava.neonorder.listener;

import me.neonjava.neonorder.gui.OrderConfigMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class BedrockMenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player))
            return;
        if (e.getInventory().getHolder() instanceof OrderConfigMenu menu) {
            e.setCancelled(true);
            menu.handle(e.getRawSlot());
        }
    }
}
