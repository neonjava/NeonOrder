package me.neonjava.neonorder.gui;

import com.github.stefvanschie.inventoryframework.adventuresupport.ComponentHolder;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import me.neonjava.neonorder.data.ConfigCache;
import me.neonjava.neonorder.load.NeonOrder;
import me.neonjava.neonorder.obj.Order;
import me.neonjava.neonorder.utils.ConvertUtils;
import me.neonjava.neonorder.utils.PlayerUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class YourOrderGUI {
    private static NeonOrder plugin;

    public static void init(NeonOrder plugin) {
        YourOrderGUI.plugin = plugin;
    }
    public static void open(Player p) {
//        final DBManager db = plugin.getDbManager();
        final UUID pUUID = p.getUniqueId();
        final List<Order> orders = plugin.getDataCache().getOrders(pUUID);
        final ConfigCache cache = plugin.getConfigs();
        final MiniMessage mm = plugin.mm;
        final ChestGui gui = new ChestGui(3, ComponentHolder.of(mm.deserialize(cache.getYoGuiTitle())));
        gui.setOnGlobalClick(e -> e.setCancelled(true));
        gui.setOnGlobalDrag(e -> e.setCancelled(true));
        final OutlinePane ordersPane = new OutlinePane(0, 0, 9, 3);
        final List<String> rawLore = cache.getYoLore();
        for (Order order : orders) {
            ordersPane.addItem(ConvertUtils.parseOrder(order, rawLore, e -> {
                PlayerUtils.closeInv(p);
                ManageOrderDialog.show(order, p);
            }));
        }

        if (orders.size() < 27) {
            ordersPane.addItem(ConvertUtils.parseButton(cache.getNewOrderButton(), e -> {
                NewOrderDialog.start(p);
            }));
        }

        gui.addPane(ordersPane);
        PlayerUtils.openGui(p, gui);
    }
}
