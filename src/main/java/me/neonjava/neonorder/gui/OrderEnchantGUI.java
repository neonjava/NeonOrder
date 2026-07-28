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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OrderEnchantGUI implements InventoryHolder {
    private static final int[] BOOK_SLOTS = {
        3, 4, 5, 6, 7, 8,
        12, 13, 14, 15, 16, 17,
        18, 19, 21, 22, 23, 24, 25, 26,
        27, 28, 29, 30, 31, 32, 33, 34, 35,
        36, 37, 38, 39, 40, 41, 42, 43, 44
    };

    private final Player player;
    private final OrderConfigMenu parentMenu;
    private final ItemStack baseItem;
    private final ItemStack workingItem;
    private final Inventory inventory;
    private final List<Enchantment> applicableEnchants;

    public OrderEnchantGUI(Player player, OrderConfigMenu parentMenu, ItemStack baseItem) {
        this.player = player;
        this.parentMenu = parentMenu;
        this.baseItem = baseItem;
        this.workingItem = baseItem.clone();
        this.applicableEnchants = getApplicableEnchantments(workingItem);
        this.inventory = Bukkit.createInventory(this, 54, Component.text("Pick Enchantments"));
        render();
    }

    public static boolean isEnchantable(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (item.getType() == Material.BOOK || item.getType() == Material.ENCHANTED_BOOK) return true;
        ItemStack checkItem = item.clone();
        checkItem.setAmount(1);
        for (Enchantment enchantment : org.bukkit.Registry.ENCHANTMENT) {
            try {
                if (enchantment.canEnchantItem(checkItem)) {
                    return true;
                }
            } catch (Exception ignored) {}
        }
        return false;
    }

    private List<Enchantment> getApplicableEnchantments(ItemStack item) {
        List<Enchantment> list = new ArrayList<>();
        ItemStack checkItem = item.clone();
        checkItem.setAmount(1);
        
        boolean isBook = item.getType() == Material.BOOK || item.getType() == Material.ENCHANTED_BOOK;

        for (Enchantment enchantment : org.bukkit.Registry.ENCHANTMENT) {
            if (isBook) {
                list.add(enchantment);
            } else {
                try {
                    if (enchantment.canEnchantItem(checkItem)) {
                        list.add(enchantment);
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        return list;
    }

    public void render() {
        inventory.clear();

        // 1. Put border glass panes
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.displayName(Component.text(" "));
            border.setItemMeta(borderMeta);
        }
        int[] borderSlots = {1, 2, 9, 10, 11, 20};
        for (int s : borderSlots) {
            inventory.setItem(s, border);
        }

        // 2. Put main item at slot 0
        Map<Enchantment, Integer> currentEnchants = getEnchantments(workingItem);
        inventory.setItem(0, workingItem);

        // 3. Render books
        int bookIdx = 0;
        for (Enchantment enchantment : applicableEnchants) {
            if (bookIdx >= BOOK_SLOTS.length) break;

            boolean conflicts = false;
            for (Enchantment selected : currentEnchants.keySet()) {
                if (selected != enchantment) {
                    try {
                        if (enchantment.conflictsWith(selected) || selected.conflictsWith(enchantment)) {
                            conflicts = true;
                            break;
                        }
                    } catch (Exception ignored) {}
                }
            }

            ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
            ItemMeta meta = book.getItemMeta();
            if (meta != null) {
                String name = enchantment.getKey().value().replace("_", " ");
                name = capitalize(name);
                
                boolean isSelected = currentEnchants.containsKey(enchantment);
                int level = isSelected ? currentEnchants.get(enchantment) : 1;

                meta.displayName(Component.text("§bEnchanted Book"));

                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("§7" + name + " " + toRoman(level)));
                if (isSelected) {
                    lore.add(Component.text("§aSelected"));
                    meta.setEnchantmentGlintOverride(true);
                } else if (conflicts) {
                    lore.add(Component.text("§cCannot add this enchantment"));
                } else {
                    lore.add(Component.text("§7Click to select"));
                }
                meta.lore(lore);
                book.setItemMeta(meta);
            }
            inventory.setItem(BOOK_SLOTS[bookIdx++], book);
        }

        // 4. Back / Cancel button at slot 47
        ItemStack cancel = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cancelMeta = cancel.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.displayName(Component.text(" "));
            cancel.setItemMeta(cancelMeta);
        }
        inventory.setItem(47, cancel);

        // 5. Confirm / Save button at slot 52
        ItemStack save = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta saveMeta = save.getItemMeta();
        if (saveMeta != null) {
            saveMeta.displayName(Component.text(" "));
            save.setItemMeta(saveMeta);
        }
        inventory.setItem(52, save);

        // 6. Arrow at slot 53
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta arrowMeta = arrow.getItemMeta();
        if (arrowMeta != null) {
            arrowMeta.displayName(Component.text(" "));
            arrow.setItemMeta(arrowMeta);
        }
        inventory.setItem(53, arrow);
    }

    public void handle(int slot, boolean isShift) {
        if (slot == 46 || slot == 47) {
            parentMenu.open();
            return;
        }

        if (slot == 52) {
            baseItem.setItemMeta(workingItem.getItemMeta());
            parentMenu.open();
            return;
        }

        int bookIdx = -1;
        for (int i = 0; i < BOOK_SLOTS.length; i++) {
            if (BOOK_SLOTS[i] == slot) {
                bookIdx = i;
                break;
            }
        }

        if (bookIdx == -1) return;

        ItemStack clicked = inventory.getItem(slot);
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Enchantment enchantment = getEnchantmentByBookIndex(bookIdx);
        if (enchantment == null) return;

        Map<Enchantment, Integer> currentEnchants = getEnchantments(workingItem);
        boolean isSelected = currentEnchants.containsKey(enchantment);

        if (!isSelected) {
            for (Enchantment selected : currentEnchants.keySet()) {
                try {
                    if (enchantment.conflictsWith(selected) || selected.conflictsWith(enchantment)) {
                        return; // Conflicting book is not clickable!
                    }
                } catch (Exception ignored) {}
            }
        }

        if (isSelected && isShift) {
            removeEnchantment(workingItem, enchantment);
            render();
            player.openInventory(inventory);
            return;
        }

        if (enchantment.getMaxLevel() > 1) {
            new OrderEnchantLevelGUI(player, this, workingItem, enchantment).open();
        } else {
            if (isSelected) {
                removeEnchantment(workingItem, enchantment);
            } else {
                setEnchantment(workingItem, enchantment, 1);
            }
            render();
            player.openInventory(inventory);
        }
    }

    private Enchantment getEnchantmentByBookIndex(int targetBookIdx) {
        Map<Enchantment, Integer> currentEnchants = getEnchantments(workingItem);
        int idx = 0;
        for (Enchantment enchantment : applicableEnchants) {
            boolean conflicts = false;
            for (Enchantment selected : currentEnchants.keySet()) {
                if (selected == enchantment) {
                    conflicts = true;
                    break;
                }
                try {
                    if (enchantment.conflictsWith(selected) || selected.conflictsWith(enchantment)) {
                        conflicts = true;
                        break;
                    }
                } catch (Exception e) {
                    // ignore
                }
            }

            if (conflicts && !currentEnchants.containsKey(enchantment)) {
                continue;
            }

            if (idx == targetBookIdx) {
                return enchantment;
            }
            idx++;
        }
        return null;
    }

    public void open() {
        render();
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public static void setEnchantment(ItemStack item, Enchantment enchantment, int level) {
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

    private static Map<Enchantment, Integer> getEnchantments(ItemStack item) {
        if (item.getType() == Material.ENCHANTED_BOOK) {
            io.papermc.paper.datacomponent.item.ItemEnchantments component = item.getData(io.papermc.paper.datacomponent.DataComponentTypes.STORED_ENCHANTMENTS);
            if (component == null) return new HashMap<>();
            return new HashMap<>(component.enchantments());
        } else {
            io.papermc.paper.datacomponent.item.ItemEnchantments component = item.getData(io.papermc.paper.datacomponent.DataComponentTypes.ENCHANTMENTS);
            if (component == null) return new HashMap<>();
            return new HashMap<>(component.enchantments());
        }
    }

    public static void removeEnchantment(ItemStack item, Enchantment enchantment) {
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
        item.setData(
                isBook ? io.papermc.paper.datacomponent.DataComponentTypes.STORED_ENCHANTMENTS : io.papermc.paper.datacomponent.DataComponentTypes.ENCHANTMENTS,
                builder.build()
        );
    }

    private String capitalize(String text) {
        String[] words = text.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    private static String toRoman(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(number);
        };
    }
}
