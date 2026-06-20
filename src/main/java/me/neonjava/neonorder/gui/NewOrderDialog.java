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
        new OrderConfigMenu(p, displayItem).open();
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
                            showAmountDialog(pl, item.clone());
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
