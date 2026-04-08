package me.neonjava.neonorder.gui;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.neonjava.neonorder.data.ConfigCache;
import me.neonjava.neonorder.listener.DialogListener;
import me.neonjava.neonorder.load.NeonOrder;
import me.neonjava.neonorder.obj.Order;
import me.neonjava.neonorder.utils.ConvertUtils;
import me.neonjava.neonorder.utils.PlayerUtils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class DeliveryConfirmDialog {
    private static MiniMessage mm;
    private static ConfigCache cache;

    public static void init(NeonOrder plugin) {
        mm = plugin.mm;
        cache = plugin.getConfigs();
    }

    public static void show(Player p, Order order, int amount, List<ItemStack> items) {
        final Dialog dialog = Dialog.create(builder -> {
            final String amountText = ConvertUtils.formatNumber(amount);
            final int amountWidth = amountText.length() * 10;
            builder.empty()
                    .base(DialogBase.builder(mm.deserialize(cache.getConfirmDeliveryTitle()))
                            .body(List.of(
                                    DialogBody.plainMessage(mm.deserialize(cache.getConfirmDeliveryBody())),
                                    DialogBody.item(ConvertUtils.parseOrder(order, cache.getOrderLore())).description(DialogBody.plainMessage(Component.text(amountText), amountWidth)).build(),
                                    DialogBody.plainMessage(mm.deserialize(cache.getConfirmDeliveryTransactionMessage(), Placeholder.unparsed("money", ConvertUtils.formatNumber(amount * order.getMoneyPer()))))
                            ))
                            .build())
                    .type(DialogType.confirmation(
                            ActionButton.builder(mm.deserialize(cache.getConfirmDeliveryConfirmLabel()))
                                    .tooltip(mm.deserialize(cache.getConfirmDeliveryConfirmHover()))
                                    .action(DialogAction.customClick(
                                            (view, player) -> {
                                                DialogListener.removeItems(p);
                                                order.deliver(p, items);
                                            },
                                            ClickCallback.Options.builder().build()
                                    ))
                                    .build(),
                            ActionButton.builder(mm.deserialize(cache.getConfirmDeliveryCancelLabel()))
                                    .tooltip(mm.deserialize(cache.getConfirmDeliveryCancelHover()))
                                    .action(DialogAction.customClick(Key.key("neonorder:confirm_delivery/cancel"), null))
                                    .build()
                    ));
        });
        DialogListener.addItems(p, items);

        PlayerUtils.openDialog(p, dialog);
    }
}
