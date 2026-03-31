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
import me.neonjava.neonorder.utils.ConvertUtils;
import me.neonjava.neonorder.utils.PlayerUtils;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class ManageOrderDialog {
        private static ConfigCache cache;
        private static MiniMessage mm;

        public static void init(NeonOrder plugin) {
                cache = plugin.getConfigs();
                mm = plugin.mm;
        }

        public static void show(Order order, Player player_) {
                // Bedrock Redirect
                if (me.neonjava.neonorder.utils.BedrockUtils.isBedrock(player_)) {
                        EditOrderGUI.open(player_, order);
                        return;
                }

                if (activeDropTasks.containsKey(player_.getUniqueId())) {
                        showDropInProgress(order, player_);
                        return;
                }

                final String name = player_.getName();
                final ItemStack item = ConvertUtils.parseOrder(order, cache.getYoLore());
                final Dialog collectItemsDialog = Dialog.create(builder -> builder.empty()
                                .base(DialogBase.builder(mm.deserialize(cache.getCollectItemsTitle()))
                                                .body(
                                                                List.of(
                                                                                DialogBody.item(item)
                                                                                                .description(DialogBody
                                                                                                                .plainMessage(ConvertUtils
                                                                                                                                .delOrder(cache.getCollectItemsBody(),
                                                                                                                                                order,
                                                                                                                                                name)))
                                                                                                .build()))
                                                .inputs(
                                                                List.of(
                                                                                DialogInput.text("amount",
                                                                                                mm.deserialize(cache
                                                                                                                .getCollectItemsAmountLabel()))
                                                                                                .initial("1")
                                                                                                .build()))
                                                .build())
                                .type(DialogType.confirmation(
                                                ActionButton.builder(
                                                                mm.deserialize(cache.getCollectItemsConfirmLabel()))
                                                                .tooltip(mm.deserialize(
                                                                                cache.getCollectItemsConfirmHover()))
                                                                .action(DialogAction.customClick((view, player) -> {
                                                                        if (!(player instanceof Player p))
                                                                                return;

                                                                        final String rawAmount = view.getText("amount");
                                                                        order.collect(rawAmount);
                                                                        YourOrderGUI.open(p);
                                                                }, ClickCallback.Options.builder().build()))
                                                                .build(),
                                                ActionButton.builder(mm.deserialize(cache.getCollectItemsCancelLabel()))
                                                                .tooltip(mm.deserialize(
                                                                                cache.getCollectItemsCancelHover()))
                                                                .build())));

                final Dialog cancelOrderDialog = Dialog.create(builder -> builder.empty()
                                .base(DialogBase.builder(mm.deserialize(cache.getCancelOrderTitle()))
                                                .body(
                                                                List.of(
                                                                                DialogBody.item(item)
                                                                                                .description(DialogBody
                                                                                                                .plainMessage(ConvertUtils
                                                                                                                                .delOrder(cache.getCancelOrderBody(),
                                                                                                                                                order,
                                                                                                                                                name)))
                                                                                                .build()))
                                                .build())
                                .type(DialogType.confirmation(
                                                ActionButton.builder(mm.deserialize(cache.getCancelOrderConfirmLabel()))
                                                                .tooltip(mm.deserialize(
                                                                                cache.getCancelOrderConfirmHover()))
                                                                .action(DialogAction.customClick((v, player) -> {
                                                                        if (!(player instanceof Player p))
                                                                                return;
                                                                        order.cancel(p);
                                                                }, ClickCallback.Options.builder().build()))
                                                                .build(),
                                                ActionButton.builder(mm.deserialize(cache.getCancelOrderCancelLabel()))
                                                                .tooltip(mm.deserialize(
                                                                                cache.getCancelOrderCancelHover()))
                                                                .build())));

                final Dialog dialog = Dialog.create(builder -> builder.empty()
                                .base(DialogBase.builder(mm.deserialize(cache.getManageOrderTitle()))
                                                .body(
                                                                List.of(
                                                                                DialogBody.item(item)
                                                                                                .description(DialogBody
                                                                                                                .plainMessage(ConvertUtils
                                                                                                                                .delOrder(cache.getManageOrderBody(),
                                                                                                                                                order,
                                                                                                                                                name)))
                                                                                                .build()))
                                                .build())
                                .type(DialogType.multiAction(
                                                List.of(
                                                                ActionButton.builder(mm.deserialize(
                                                                                cache.getCollectItemsLabel()))
                                                                                .tooltip(mm.deserialize(cache
                                                                                                .getCollectItemsHover()))
                                                                                .action(DialogAction.customClick(
                                                                                                (v, player) -> {
                                                                                                        if (!(player instanceof Player p))
                                                                                                                return;
                                                                                                        PlayerUtils.openDialog(
                                                                                                                        p,
                                                                                                                        collectItemsDialog);
                                                                                                },
                                                                                                ClickCallback.Options
                                                                                                                .builder()
                                                                                                                .build()))
                                                                                .build(),
                                                                ActionButton.builder(mm
                                                                                .deserialize(cache.getDropItemsLabel()))
                                                                                .tooltip(mm.deserialize(cache
                                                                                                .getDropItemsHover()))
                                                                                .action(DialogAction.customClick(
                                                                                                (v, player) -> {
                                                                                                        if (!(player instanceof Player p))
                                                                                                                return;
                                                                                                        startDropTask(order,
                                                                                                                        p);
                                                                                                },
                                                                                                ClickCallback.Options
                                                                                                                .builder()
                                                                                                                .build()))
                                                                                .build(),
                                                                ActionButton.builder(mm.deserialize(
                                                                                cache.getCancelOrderLabel()))
                                                                                .tooltip(mm.deserialize(cache
                                                                                                .getCancelOrderHover()))
                                                                                .action(DialogAction.customClick(
                                                                                                (v, player) -> {
                                                                                                        if (!(player instanceof Player p))
                                                                                                                return;
                                                                                                        PlayerUtils.openDialog(
                                                                                                                        p,
                                                                                                                        cancelOrderDialog);
                                                                                                },
                                                                                                ClickCallback.Options
                                                                                                                .builder()
                                                                                                                .build()))
                                                                                .build()))
                                                .build()));
                PlayerUtils.openDialog(player_, dialog);
        }

        private static final java.util.Map<java.util.UUID, org.bukkit.scheduler.BukkitTask> activeDropTasks = new java.util.concurrent.ConcurrentHashMap<>();

        private static void startDropTask(Order order, Player p) {
                if (activeDropTasks.containsKey(p.getUniqueId()))
                        return;

                org.bukkit.scheduler.BukkitTask task = org.bukkit.Bukkit.getScheduler().runTaskTimer(NeonOrder.plugin,
                                () -> {
                                        if (!p.isOnline() || order.getInStorage() <= 0) {
                                                stopDropTask(p.getUniqueId());
                                                return;
                                        }

                                        int toDrop = Math.min(order.getInStorage(), 1000);
                                        order.collect(toDrop);
                                        // The items are dropped by PlayerUtils.give if we don't handle it, but wait
                                        // order.collect(int) calls plugin.getStorage().collectItems(...) which then
                                        // calls PlayerUtils.give(p, item, amount, true)
                                        // Setting 'true' in PlayerUtils.give drops items if inventory is full.
                                        // So it works as intended!

                                        p.sendActionBar(mm.deserialize(
                                                        "<gradient:#f6d365:#fda085><bold>[!]</bold> Dispenser Dropping "
                                                                        + toDrop + " items... (" + order.getInStorage()
                                                                        + " left)"));
                                }, 0, 100); // 5 seconds = 100 ticks

                activeDropTasks.put(p.getUniqueId(), task);
                showDropInProgress(order, p);
        }

        private static void stopDropTask(java.util.UUID uuid) {
                org.bukkit.scheduler.BukkitTask task = activeDropTasks.remove(uuid);
                if (task != null)
                        task.cancel();
        }

        private static void showDropInProgress(Order order, Player p) {
                final ItemStack item = ConvertUtils.parseOrder(order, cache.getYoLore());
                final Dialog dropDialog = Dialog.create(builder -> builder.empty()
                                .base(DialogBase.builder(mm.deserialize(cache.getDropInProgressTitle()))
                                                .body(List.of(DialogBody.item(item)
                                                                .description(DialogBody.plainMessage(mm.deserialize(
                                                                                "<gray>Dropping items automatically...\n<yellow>Items left: <white>"
                                                                                                + order.getInStorage())))
                                                                .build()))
                                                .build())
                                .type(DialogType.confirmation(
                                                ActionButton.builder(mm.deserialize(cache.getStopDropLabel()))
                                                                .action(DialogAction.customClick(
                                                                                (v, pl) -> {
                                                                                        if (pl instanceof Player player)
                                                                                                stopDropTask(player
                                                                                                                .getUniqueId());
                                                                                },
                                                                                ClickCallback.Options.builder()
                                                                                                .build()))
                                                                .build(),
                                                ActionButton.builder(mm.deserialize("<gray>Back"))
                                                                .action(DialogAction.customClick(
                                                                                (v, pl) -> {
                                                                                        if (pl instanceof Player player)
                                                                                                show(order, player);
                                                                                },
                                                                                ClickCallback.Options.builder()
                                                                                                .build()))
                                                                .build())));
                PlayerUtils.openDialog(p, dropDialog);
        }
}
