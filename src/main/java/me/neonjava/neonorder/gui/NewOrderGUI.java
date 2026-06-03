package me.neonjava.neonorder.gui;

import com.github.stefvanschie.inventoryframework.adventuresupport.ComponentHolder;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import me.neonjava.neonorder.load.NeonOrder;
import me.neonjava.neonorder.obj.Order;
import me.neonjava.neonorder.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.block.BlockType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewOrderGUI {

    private static final Map<java.util.UUID, String> amountCache = new HashMap<>();
    private static final Map<java.util.UUID, String> priceCache = new HashMap<>();

    public static void open(Player p, ItemStack displayItem) {
        if (YourOrderGUI.isAtLimit(p)) {
            p.sendRichMessage("<red>You have reached your order limit (<white>" + YourOrderGUI.getLimit(p) + "</white>). Cancel an existing order first.");
            return;
        }

        MiniMessage mm = NeonOrder.plugin.mm;
        String title = "<black>ORDERS --> New Order</black>";
        ChestGui gui = new ChestGui(4, ComponentHolder.of(mm.deserialize(title)));
        gui.setOnGlobalClick(e -> e.setCancelled(true));
        gui.setOnGlobalDrag(e -> e.setCancelled(true));

        StaticPane pane = new StaticPane(0, 0, 9, 4);

        // Background
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gm = glass.getItemMeta();
        if (gm != null) {
            gm.displayName(Component.empty());
            glass.setItemMeta(gm);
        }
        pane.fillWith(glass);

        // Slot 11 (Row 1, Col 2): Display Item
        pane.addItem(new GuiItem(displayItem.clone(), e -> e.setCancelled(true)), 2, 0);

        // Slot 13 (Row 1, Col 4): Change Item Button
        ItemStack changeItem = new ItemStack(Material.HOPPER);
        ItemMeta cim = changeItem.getItemMeta();
        if (cim != null) {
            cim.displayName(mm.deserialize("<blue>Change Item").decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(mm.deserialize("<gray>Click to select").decoration(TextDecoration.ITALIC, false));
            lore.add(mm.deserialize("<gray>a different item.").decoration(TextDecoration.ITALIC, false));
            cim.lore(lore);
            changeItem.setItemMeta(cim);
        }
        pane.addItem(new GuiItem(changeItem, e -> {
            e.setCancelled(true);
            p.closeInventory();
            ChooseItemGUI.choose(p, 0, 0);
        }), 4, 0);

        // Slot 15 (Row 1, Col 6): Price - Emerald Icon
        ItemStack priceBtn = new ItemStack(Material.EMERALD);
        ItemMeta pim = priceBtn.getItemMeta();
        if (pim != null) {
            String priceVal = priceCache.getOrDefault(p.getUniqueId(), "1");
            pim.displayName(mm.deserialize("<gold>Price Per Item: <yellow>" + priceVal)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(mm.deserialize("<gray>Click to set price").decoration(TextDecoration.ITALIC, false));
            pim.lore(lore);
            priceBtn.setItemMeta(pim);
        }
        pane.addItem(new GuiItem(priceBtn, e -> {
            e.setCancelled(true);
            p.closeInventory();
            openSignGUI(p, displayItem, "price");
        }), 6, 0);

        // Slot 20 (Row 2, Col 2): Amount - Green Glass Pane
        ItemStack amountBtn = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta aim = amountBtn.getItemMeta();
        if (aim != null) {
            String amountVal = amountCache.getOrDefault(p.getUniqueId(), "1");
            aim.displayName(mm.deserialize("<green>Amount to Deliver: <white>" + amountVal)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(mm.deserialize("<gray>Click to set amount").decoration(TextDecoration.ITALIC, false));
            aim.lore(lore);
            amountBtn.setItemMeta(aim);
        }
        pane.addItem(new GuiItem(amountBtn, e -> {
            e.setCancelled(true);
            p.closeInventory();
            openSignGUI(p, displayItem, "amount");
        }), 2, 1);

        // Slot 22 (Row 2, Col 4): Summary - Green Glass Pane
        ItemStack summary = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta sim = summary.getItemMeta();
        if (sim != null) {
            String amount = amountCache.getOrDefault(p.getUniqueId(), "1");
            String price = priceCache.getOrDefault(p.getUniqueId(), "1");
            sim.displayName(mm.deserialize("<green>Order Summary")
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(mm.deserialize("<gray>Items to deliver: <white>" + amount)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(mm.deserialize("<gray>Price per item: <gold>" + price)
                    .decoration(TextDecoration.ITALIC, false));
            try {
                long totalPrice = Long.parseLong(price.replaceAll("[^0-9]", "")) * Long.parseLong(amount);
                lore.add(mm.deserialize("<gray>Total payment: <gold>" + formatPrice(totalPrice))
                        .decoration(TextDecoration.ITALIC, false));
            } catch (NumberFormatException ex) {
                // Ignore
            }
            sim.lore(lore);
            summary.setItemMeta(sim);
        }
        pane.addItem(new GuiItem(summary, e -> e.setCancelled(true)), 4, 1);

        // Slot 24 (Row 2, Col 6): Confirm Button - Lime
        ItemStack confirm = new ItemStack(Material.LIME_WOOL);
        ItemMeta cfm = confirm.getItemMeta();
        if (cfm != null) {
            cfm.displayName(mm.deserialize("<green><bold>Confirm Order")
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(mm.deserialize("<gray>Click to create").decoration(TextDecoration.ITALIC, false));
            cfm.lore(lore);
            confirm.setItemMeta(cfm);
        }
        pane.addItem(new GuiItem(confirm, e -> {
            e.setCancelled(true);
            String amount = amountCache.getOrDefault(p.getUniqueId(), "1");
            String price = priceCache.getOrDefault(p.getUniqueId(), "1");

            p.closeInventory();
            Order.Response response = Order.create(p, displayItem, price, amount);

            switch (response) {
                case INVALID -> p.sendRichMessage("<red>Invalid amount or price!");
                case FAIL -> p.sendRichMessage("<red>You don't have enough money!");
                case SUCCESS -> {
                    p.sendRichMessage("<green>Order created successfully!");
                    amountCache.remove(p.getUniqueId());
                    priceCache.remove(p.getUniqueId());
                    PlayerUtils.playSound(p, NeonOrder.plugin.getConfigs().getNewOrderSound());
                    YourOrderGUI.open(p);
                }
                default -> {}
            }
        }), 6, 1);

        // Slot 31 (Row 3, Col 4): Back Button
        ItemStack back = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta bm = back.getItemMeta();
        if (bm != null) {
            bm.displayName(mm.deserialize("<red>Back").decoration(TextDecoration.ITALIC, false));
            back.setItemMeta(bm);
        }
        pane.addItem(new GuiItem(back, e -> {
            e.setCancelled(true);
            amountCache.remove(p.getUniqueId());
            priceCache.remove(p.getUniqueId());
            p.closeInventory();
            ChooseItemGUI.choose(p, 0, 0);
        }), 4, 2);

        gui.addPane(pane);
        PlayerUtils.openGui(p, gui);
    }

    private static void openSignGUI(Player p, ItemStack item, String type) {
        MiniMessage mm = NeonOrder.plugin.mm;
        List<String> lines = List.of(
            "",
            "^^^^^^^^^^^^",
            type.equals("amount") ? "Amount:" : "Price:",
            "Enter value"
        );

        SignGUI.newSession(p, input -> {
            if (input == null || input.isEmpty()) {
                p.getScheduler().run(NeonOrder.plugin, t -> open(p, item), null);
                return;
            }

            if (type.equals("amount")) {
                try {
                    long val = Long.parseLong(input.trim());
                    if (val > 0) {
                        amountCache.put(p.getUniqueId(), String.valueOf(val));
                        p.sendRichMessage("<green>Amount set to: <white>" + val);
                    } else {
                        p.sendRichMessage("<red>Amount must be greater than 0!");
                    }
                } catch (NumberFormatException ex) {
                    p.sendRichMessage("<red>Invalid amount!");
                }
            } else if (type.equals("price")) {
                try {
                    String cleaned = input.trim();
                    if (cleaned.isEmpty()) {
                        p.sendRichMessage("<red>Invalid price!");
                    } else {
                        priceCache.put(p.getUniqueId(), cleaned);
                        p.sendRichMessage("<green>Price set to: <gold>" + cleaned);
                    }
                } catch (Exception ex) {
                    p.sendRichMessage("<red>Invalid price!");
                }
            }

            p.getScheduler().run(NeonOrder.plugin, t -> open(p, item), null);
        }, lines, BlockType.OAK_SIGN);
    }

    private static String formatPrice(long value) {
        if (value >= 1_000_000_000) {
            return String.format("%.1fb", value / 1_000_000_000.0).replaceAll("\\.0+$", "");
        } else if (value >= 1_000_000) {
            return String.format("%.1fm", value / 1_000_000.0).replaceAll("\\.0+$", "");
        } else if (value >= 1_000) {
            return String.format("%.1fk", value / 1_000.0).replaceAll("\\.0+$", "");
        }
        return String.valueOf(value);
    }
}
