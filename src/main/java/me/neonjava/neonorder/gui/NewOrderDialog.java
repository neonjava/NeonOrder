package me.neonjava.neonorder.gui;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.neonjava.neonorder.data.ConfigCache;
import me.neonjava.neonorder.load.NeonOrder;
import me.neonjava.neonorder.obj.Order;
import me.neonjava.neonorder.obj.SortTypes;
import me.neonjava.neonorder.utils.AlgoUtils;
import me.neonjava.neonorder.utils.ConvertUtils;
import me.neonjava.neonorder.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class NewOrderDialog {

    private static MiniMessage mm;
    private static ConfigCache cache;

    public static void init(NeonOrder plugin) {
        mm = plugin.mm;
        cache = plugin.getConfigs();
    }

    public static void start(Player p) {
        if (YourOrderGUI.isAtLimit(p)) {
            p.sendRichMessage("<red>You have reached your order limit (<white>" + YourOrderGUI.getLimit(p)
                    + "</white>). Cancel an existing order first.");
            return;
        }

        ChooseItemGUI.choose(p, 0, 0);
    }

    public static void newSession(Player p, ItemStack displayItem) {
        OrderConfigMenu configMenu = new OrderConfigMenu(p, displayItem);
        if (OrderEnchantGUI.isEnchantable(displayItem)) {
            new OrderEnchantGUI(p, configMenu, displayItem).open();
        } else {
            configMenu.open();
        }
    }

    private static void showSearchAndSelectDialog(Player p, String query) {
        List<ItemStack> items = new ArrayList<>(NeonOrder.plugin.getDataCache().getItems(SortTypes.A_Z));
        final boolean isSearched = query != null && !query.trim().isEmpty();
        if (isSearched) {
            items = AlgoUtils.searchItem(query, items);
        }

        // Limit results to 500 to keep UI responsive and prevent network packet limits
        final List<ItemStack> results = items.stream().limit(500).toList();

        final String titleText = isSearched ? "Choose Item (" + results.size() + " results)" : "Choose Item";
        final String searchInitial = isSearched ? query : "";

        final List<ActionButton> buttons = new ArrayList<>();

        // Add Search button
        buttons.add(ActionButton.builder(Component.text("Search", NamedTextColor.GREEN))
                .action(DialogAction.customClick((view, player) -> {
                    if (player instanceof Player pl) {
                        showSearchAndSelectDialog(pl, view.getText("search"));
                    }
                }, ClickCallback.Options.builder().uses(1).build()))
                .build());

        // Add item buttons
        for (ItemStack item : results) {
            String name = getItemDisplayName(item);
            buttons.add(ActionButton.builder(Component.text(name))
                    .action(DialogAction.customClick((view, player) -> {
                        if (player instanceof Player pl) {
                            checkAndShowEnchantDialog(pl, item.clone(), new java.util.HashMap<>());
                        }
                    }, ClickCallback.Options.builder().uses(1).build()))
                    .build());
        }

        // Add Cancel button
        buttons.add(ActionButton.builder(Component.text("Cancel", NamedTextColor.RED))
                .action(DialogAction.customClick((view, player) -> {
                    if (player instanceof Player pl) {
                        YourOrderGUI.open(pl);
                    }
                }, ClickCallback.Options.builder().uses(1).build()))
                .build());

        final Dialog selectDialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(titleText, NamedTextColor.GOLD))
                        .body(List.of(DialogBody.plainMessage(Component.text("Search for the item you want to order:"))))
                        .inputs(List.of(
                                DialogInput.text("search", Component.text("Search"))
                                        .initial(searchInitial)
                                        .build()
                        ))
                        .build()
                )
                .type(DialogType.multiAction(buttons).build()));

        PlayerUtils.openDialog(p, selectDialog);
    }

    private static void showAmountDialog(Player p, ItemStack selectedItem) {
        final Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Enter Amount", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.item(selectedItem)
                                        .description(DialogBody.plainMessage(Component.text("Specify the amount you want to order.")))
                                        .build()
                        ))
                        .inputs(List.of(
                                DialogInput.text("amount", Component.text("Amount"))
                                        .initial("64")
                                        .build()
                        ))
                        .build()
                )
                .type(DialogType.confirmation(
                        ActionButton.builder(Component.text("Next", NamedTextColor.GREEN))
                                .action(DialogAction.customClick((view, player) -> {
                                    if (player instanceof Player pl) {
                                        String amtStr = view.getText("amount");
                                        double amtDouble = ConvertUtils.formatNumber(amtStr);
                                        int amt = (int) amtDouble;
                                        if (amtDouble <= 0 || amtDouble != amt) {
                                            pl.sendRichMessage("<red>refresh your order!");
                                            showAmountDialog(pl, selectedItem);
                                            return;
                                        }
                                        showPriceDialog(pl, selectedItem, amt);
                                    }
                                }, ClickCallback.Options.builder().uses(1).build()))
                                .build(),
                        ActionButton.builder(Component.text("Cancel", NamedTextColor.RED))
                                .action(DialogAction.customClick((view, player) -> {
                                    if (player instanceof Player pl) {
                                        YourOrderGUI.open(pl);
                                    }
                                }, ClickCallback.Options.builder().uses(1).build()))
                                .build()
                )));
        PlayerUtils.openDialog(p, dialog);
    }

    private static void showPriceDialog(Player p, ItemStack selectedItem, int amount) {
        final Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Enter Price", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.item(selectedItem)
                                        .description(DialogBody.plainMessage(Component.text("Specify the price per item. (Amount: " + ConvertUtils.formatNumber(amount) + ")")))
                                        .build()
                        ))
                        .inputs(List.of(
                                DialogInput.text("price", Component.text("Price per item"))
                                        .initial("1")
                                        .build()
                        ))
                        .build()
                )
                .type(DialogType.confirmation(
                        ActionButton.builder(Component.text("Next", NamedTextColor.GREEN))
                                .action(DialogAction.customClick((view, player) -> {
                                    if (player instanceof Player pl) {
                                        String priceStr = view.getText("price");
                                        double price = ConvertUtils.formatNumber(priceStr);
                                        if (price <= 0) {
                                            pl.sendRichMessage("<red>Invalid price number or format!");
                                            showPriceDialog(pl, selectedItem, amount);
                                            return;
                                        }
                                        showReviewDialog(pl, selectedItem, amount, price);
                                    }
                                }, ClickCallback.Options.builder().uses(1).build()))
                                .build(),
                        ActionButton.builder(Component.text("Cancel", NamedTextColor.RED))
                                .action(DialogAction.customClick((view, player) -> {
                                    if (player instanceof Player pl) {
                                        YourOrderGUI.open(pl);
                                    }
                                }, ClickCallback.Options.builder().uses(1).build()))
                                .build()
                )));
        PlayerUtils.openDialog(p, dialog);
    }

    private static void showReviewDialog(Player p, ItemStack selectedItem, int amount, double price) {
        final Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Review Order", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.item(selectedItem)
                                        .description(DialogBody.plainMessage(Component.text("Please review your order details below:")))
                                        .build(),
                                DialogBody.plainMessage(Component.text("Amount: " + ConvertUtils.formatNumber(amount))),
                                DialogBody.plainMessage(Component.text("Price per item: $" + ConvertUtils.formatNumber(price))),
                                DialogBody.plainMessage(Component.text("Total Cost: $" + ConvertUtils.formatNumber(amount * price)))
                        ))
                        .build()
                )
                .type(DialogType.multiAction(List.of(
                        ActionButton.builder(Component.text("Create Order", NamedTextColor.GREEN))
                                .action(DialogAction.customClick((view, player) -> {
                                    if (player instanceof Player pl) {
                                        PlayerUtils.closeInv(pl);
                                        final Order.Response response = Order.create(pl, selectedItem, ConvertUtils.formatNumber(price), ConvertUtils.formatNumber(amount));
                                        switch (response) {
                                            case INVALID -> pl.sendRichMessage(cache.getInvalidInput());
                                            case FAIL -> pl.sendRichMessage(cache.getNotEnoughMoney());
                                            case SUCCESS -> {
                                                pl.sendRichMessage(cache.getOrderCreationSuccessful());
                                                PlayerUtils.playSound(pl, cache.getNewOrderSound());
                                            }
                                            default -> {}
                                        }
                                    }
                                }, ClickCallback.Options.builder().uses(1).build()))
                                .build(),
                        ActionButton.builder(Component.text("Change Item", NamedTextColor.YELLOW))
                                .action(DialogAction.customClick((view, player) -> {
                                    if (player instanceof Player pl) {
                                        showSearchAndSelectDialog(pl, null);
                                    }
                                }, ClickCallback.Options.builder().uses(1).build()))
                                .build(),
                        ActionButton.builder(Component.text("Change Amount", NamedTextColor.YELLOW))
                                .action(DialogAction.customClick((view, player) -> {
                                    if (player instanceof Player pl) {
                                        showAmountDialog(pl, selectedItem);
                                    }
                                }, ClickCallback.Options.builder().uses(1).build()))
                                .build(),
                        ActionButton.builder(Component.text("Change Price", NamedTextColor.YELLOW))
                                .action(DialogAction.customClick((view, player) -> {
                                    if (player instanceof Player pl) {
                                        showPriceDialog(pl, selectedItem, amount);
                                    }
                                }, ClickCallback.Options.builder().uses(1).build()))
                                .build(),
                        ActionButton.builder(Component.text("Cancel", NamedTextColor.RED))
                                .action(DialogAction.customClick((view, player) -> {
                                    if (player instanceof Player pl) {
                                        YourOrderGUI.open(pl);
                                    }
                                }, ClickCallback.Options.builder().uses(1).build()))
                                .build()
                )).build()));
        PlayerUtils.openDialog(p, dialog);
    }

    private static void checkAndShowEnchantDialog(Player p, ItemStack selectedItem, java.util.Map<org.bukkit.enchantments.Enchantment, Integer> selectedEnchants) {
        if (!isEnchantable(selectedItem)) {
            ItemStack finalItem = applyEnchants(selectedItem.clone(), selectedEnchants);
            showAmountDialog(p, finalItem);
            return;
        }

        final Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Enchantments Selection", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.item(applyEnchants(selectedItem.clone(), selectedEnchants))
                                        .description(DialogBody.plainMessage(Component.text("Select enchantments for this item.")))
                                        .build(),
                                DialogBody.plainMessage(Component.text(getSelectedEnchantsSummary(selectedEnchants)))
                        ))
                        .build()
                )
                .type(DialogType.multiAction(List.of(
                        ActionButton.builder(Component.text("Add Enchantment", NamedTextColor.GREEN))
                                .action(DialogAction.customClick((view, player) -> {
                                    if (player instanceof Player pl) {
                                        showEnchantmentListDialog(pl, selectedItem, selectedEnchants);
                                    }
                                }, ClickCallback.Options.builder().uses(1).build()))
                                .build(),
                        ActionButton.builder(Component.text("Clear Enchantments", NamedTextColor.YELLOW))
                                .action(DialogAction.customClick((view, player) -> {
                                    if (player instanceof Player pl) {
                                        checkAndShowEnchantDialog(pl, selectedItem, new java.util.HashMap<>());
                                    }
                                }, ClickCallback.Options.builder().uses(1).build()))
                                .build(),
                        ActionButton.builder(Component.text("Continue", NamedTextColor.AQUA))
                                .action(DialogAction.customClick((view, player) -> {
                                    if (player instanceof Player pl) {
                                        ItemStack finalItem = applyEnchants(selectedItem.clone(), selectedEnchants);
                                        showAmountDialog(pl, finalItem);
                                    }
                                }, ClickCallback.Options.builder().uses(1).build()))
                                .build(),
                        ActionButton.builder(Component.text("Back", NamedTextColor.RED))
                                .action(DialogAction.customClick((view, player) -> {
                                    if (player instanceof Player pl) {
                                        showSearchAndSelectDialog(pl, null);
                                    }
                                }, ClickCallback.Options.builder().uses(1).build()))
                                .build()
                )).build()));
        PlayerUtils.openDialog(p, dialog);
    }

    private static void showEnchantmentListDialog(Player p, ItemStack selectedItem, java.util.Map<org.bukkit.enchantments.Enchantment, Integer> selectedEnchants) {
        List<ActionButton> buttons = new ArrayList<>();
        ItemStack checkItem = selectedItem.clone();
        checkItem.setAmount(1);

        for (org.bukkit.enchantments.Enchantment enchantment : org.bukkit.Registry.ENCHANTMENT) {
            boolean applicable = false;
            if (checkItem.getType() == org.bukkit.Material.BOOK || checkItem.getType() == org.bukkit.Material.ENCHANTED_BOOK) {
                applicable = true;
            } else {
                try {
                    applicable = enchantment.canEnchantItem(checkItem);
                } catch (Exception e) {
                    applicable = false;
                }
            }

            if (!applicable) continue;

            boolean conflicts = false;
            for (org.bukkit.enchantments.Enchantment selected : selectedEnchants.keySet()) {
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

            if (conflicts) continue;

            String name = capitalize(enchantment.getKey().value().replace("_", " "));
            buttons.add(ActionButton.builder(Component.text(name))
                    .action(DialogAction.customClick((view, player) -> {
                        if (player instanceof Player pl) {
                            showEnchantLevelDialog(pl, selectedItem, selectedEnchants, enchantment);
                        }
                    }, ClickCallback.Options.builder().uses(1).build()))
                    .build());
        }

        buttons.add(ActionButton.builder(Component.text("Back", NamedTextColor.RED))
                .action(DialogAction.customClick((view, player) -> {
                    if (player instanceof Player pl) {
                        checkAndShowEnchantDialog(pl, selectedItem, selectedEnchants);
                    }
                }, ClickCallback.Options.builder().uses(1).build()))
                .build());

        final Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Select Enchantment", NamedTextColor.GOLD))
                        .body(List.of(DialogBody.plainMessage(Component.text("Choose an enchantment to add:"))))
                        .build()
                )
                .type(DialogType.multiAction(buttons).build()));
        PlayerUtils.openDialog(p, dialog);
    }

    private static void showEnchantLevelDialog(Player p, ItemStack selectedItem, java.util.Map<org.bukkit.enchantments.Enchantment, Integer> selectedEnchants, org.bukkit.enchantments.Enchantment enchantment) {
        List<ActionButton> buttons = new ArrayList<>();

        int maxLvl = enchantment.getMaxLevel();
        int limit = maxLvl;

        for (int i = 1; i <= limit; i++) {
            final int level = i;
            buttons.add(ActionButton.builder(Component.text("Level " + level))
                    .action(DialogAction.customClick((view, player) -> {
                        if (player instanceof Player pl) {
                            java.util.Map<org.bukkit.enchantments.Enchantment, Integer> updated = new java.util.HashMap<>(selectedEnchants);
                            updated.put(enchantment, level);
                            checkAndShowEnchantDialog(pl, selectedItem, updated);
                        }
                    }, ClickCallback.Options.builder().uses(1).build()))
                    .build());
        }

        buttons.add(ActionButton.builder(Component.text("Back", NamedTextColor.RED))
                .action(DialogAction.customClick((view, player) -> {
                    if (player instanceof Player pl) {
                        showEnchantmentListDialog(pl, selectedItem, selectedEnchants);
                    }
                }, ClickCallback.Options.builder().uses(1).build()))
                .build());

        final Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Select Level", NamedTextColor.GOLD))
                        .body(List.of(DialogBody.plainMessage(Component.text("Choose a level for " + capitalize(enchantment.getKey().value().replace("_", " ")) + ":"))))
                        .build()
                )
                .type(DialogType.multiAction(buttons).build()));
        PlayerUtils.openDialog(p, dialog);
    }

    private static boolean isEnchantable(ItemStack item) {
        if (item.getType() == org.bukkit.Material.BOOK || item.getType() == org.bukkit.Material.ENCHANTED_BOOK) {
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

    private static ItemStack applyEnchants(ItemStack item, java.util.Map<org.bukkit.enchantments.Enchantment, Integer> selectedEnchants) {
        ItemStack clone = item.clone();
        if (selectedEnchants.isEmpty()) {
            return clone;
        }

        if (clone.getType() == org.bukkit.Material.BOOK) {
            clone.setType(org.bukkit.Material.ENCHANTED_BOOK);
        }

        if (clone.getType() == org.bukkit.Material.ENCHANTED_BOOK) {
            io.papermc.paper.datacomponent.item.ItemEnchantments.Builder builder = io.papermc.paper.datacomponent.item.ItemEnchantments.itemEnchantments();
            builder.addAll(selectedEnchants);
            clone.setData(io.papermc.paper.datacomponent.DataComponentTypes.STORED_ENCHANTMENTS, builder.build());
        } else {
            io.papermc.paper.datacomponent.item.ItemEnchantments.Builder builder = io.papermc.paper.datacomponent.item.ItemEnchantments.itemEnchantments();
            builder.addAll(selectedEnchants);
            clone.setData(io.papermc.paper.datacomponent.DataComponentTypes.ENCHANTMENTS, builder.build());
        }
        return clone;
    }

    private static String getSelectedEnchantsSummary(java.util.Map<org.bukkit.enchantments.Enchantment, Integer> selectedEnchants) {
        if (selectedEnchants.isEmpty()) {
            return "Current Enchantments: None";
        }
        StringBuilder sb = new StringBuilder("Current Enchantments:\n");
        for (java.util.Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : selectedEnchants.entrySet()) {
            String name = entry.getKey().getKey().value().replace("_", " ");
            sb.append("- ").append(capitalize(name)).append(" ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }

    private static String capitalize(String text) {
        String[] words = text.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    private static String getItemDisplayName(ItemStack item) {
        if (item.getItemMeta().hasDisplayName()) {
            return mm.serialize(item.getItemMeta().displayName());
        }

        String name = item.getType().toString().toLowerCase().replace("_", " ");

        // Check for Enchanted Book stored enchantments
        if (item.getType() == org.bukkit.Material.ENCHANTED_BOOK) {
            io.papermc.paper.datacomponent.item.ItemEnchantments enchantments = item.getData(io.papermc.paper.datacomponent.DataComponentTypes.STORED_ENCHANTMENTS);
            if (enchantments != null && !enchantments.enchantments().isEmpty()) {
                java.util.Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry = enchantments.enchantments().entrySet().iterator().next();
                String enchantName = entry.getKey().getKey().value().replace("_", " ");
                int level = entry.getValue();
                name = enchantName + " " + level;
            }
        } else {
            // Also check for regular enchantments on items
            io.papermc.paper.datacomponent.item.ItemEnchantments enchantments = item.getData(io.papermc.paper.datacomponent.DataComponentTypes.ENCHANTMENTS);
            if (enchantments != null && !enchantments.enchantments().isEmpty()) {
                java.util.Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry = enchantments.enchantments().entrySet().iterator().next();
                String enchantName = entry.getKey().getKey().value().replace("_", " ");
                int level = entry.getValue();
                name += " (" + enchantName + " " + level + ")";
            }
        }

        // Capitalize words beautifully
        String[] words = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }
}

