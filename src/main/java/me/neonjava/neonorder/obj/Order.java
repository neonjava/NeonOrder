package me.neonjava.neonorder.obj;

import lombok.Getter;
import lombok.val;
import me.neonjava.neonorder.api.events.PlayerCancelOrderEvent;
import me.neonjava.neonorder.api.events.PlayerCollectItemsEvent;
import me.neonjava.neonorder.api.events.PlayerCreateOrderEvent;
import me.neonjava.neonorder.api.events.PlayerDeliverOrderEvent;
import me.neonjava.neonorder.data.ConfigCache;
import me.neonjava.neonorder.gui.YourOrderGUI;
import me.neonjava.neonorder.load.NeonOrder;
import me.neonjava.neonorder.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.block.ShulkerBox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import static me.neonjava.neonorder.load.NeonOrder.plugin;

@Getter
public class Order implements me.neonjava.neonorder.api.Order {
    public final int id;
    public final UUID owner;
    public final ItemStack item;
    public double moneyPer;
    public int amount;
    public int delivered;
    public int inStorage;
    public long expiresAt;

    // private static DBManager db;
    private static ConfigCache cache;

    public static void init(NeonOrder plugin) {
        // db = plugin.getDbManager();
        cache = plugin.getConfigs();
    }

    public Order(int id, UUID owner, ItemStack item, double moneyPer, int amount, int delivered, int inStorage,
            long expiresAt) {
        this.id = id;
        this.owner = owner;
        this.item = item;
        this.moneyPer = moneyPer;
        this.amount = amount;
        this.delivered = delivered;
        this.inStorage = inStorage;
        this.expiresAt = expiresAt;
    }

    public boolean isActive() {
        return delivered < amount && expiresAt > System.currentTimeMillis();
    }

    private static class MatchSource {
        final ItemStack sourceStack;
        final int shulkerSlot;
        final ItemStack matchedStack;

        MatchSource(ItemStack sourceStack, int shulkerSlot, ItemStack matchedStack) {
            this.sourceStack = sourceStack;
            this.shulkerSlot = shulkerSlot;
            this.matchedStack = matchedStack;
        }
    }

    /// Must be called in the player region
    public void deliver(Player p, Collection<ItemStack> items) {
        val matchedSources = new ArrayList<MatchSource>();
        val nonMatchingItems = new ArrayList<ItemStack>();
        var totalMatchingAmount = 0;

        for (ItemStack item : items) {
            if (item == null || item.isEmpty()) continue;

            if (AlgoUtils.isSimilar(item, getItem())) {
                matchedSources.add(new MatchSource(item, -1, item));
                totalMatchingAmount += item.getAmount();
            } else if (item.getItemMeta() instanceof BlockStateMeta bsm &&
                       bsm.getBlockState() instanceof ShulkerBox shulkerBox) {
                val contents = shulkerBox.getInventory().getContents();
                var hasMatches = false;
                for (int slot = 0; slot < contents.length; slot++) {
                    ItemStack shulkerItem = contents[slot];
                    if (shulkerItem != null && !shulkerItem.isEmpty() && AlgoUtils.isSimilar(shulkerItem, getItem())) {
                        matchedSources.add(new MatchSource(item, slot, shulkerItem));
                        totalMatchingAmount += shulkerItem.getAmount();
                        hasMatches = true;
                    }
                }
                if (!hasMatches) {
                    nonMatchingItems.add(item);
                }
            } else {
                nonMatchingItems.add(item);
            }
        }

        for (ItemStack item : nonMatchingItems) {
            PlayerUtils.give(p, item, false);
        }

        if (totalMatchingAmount == 0)
            return;

        val event = new PlayerDeliverOrderEvent(p, this, totalMatchingAmount);
        event.callEvent();

        final int finalMatchingAmount = totalMatchingAmount;
        deliver(p, totalMatchingAmount).thenAccept(exceeded -> {
            p.getScheduler().run(plugin, task -> {
                val toConsume = finalMatchingAmount - exceeded;
                val modifiedShulkers = new HashMap<ItemStack, ShulkerBox>();
                val sourceStacksToReturn = new HashSet<ItemStack>();

                var consumed = 0;
                for (MatchSource source : matchedSources) {
                    val matched = source.matchedStack;
                    val matchAmt = matched.getAmount();

                    if (consumed >= toConsume) {
                        sourceStacksToReturn.add(source.sourceStack);
                        continue;
                    }

                    val remainingToConsume = toConsume - consumed;
                    if (matchAmt <= remainingToConsume) {
                        consumed += matchAmt;
                        if (source.shulkerSlot != -1) {
                            val shulker = modifiedShulkers.computeIfAbsent(source.sourceStack, k -> 
                                (ShulkerBox) ((BlockStateMeta) k.getItemMeta()).getBlockState()
                            );
                            shulker.getInventory().setItem(source.shulkerSlot, null);
                            sourceStacksToReturn.add(source.sourceStack);
                        }
                    } else {
                        matched.setAmount(matchAmt - remainingToConsume);
                        consumed = toConsume;
                        if (source.shulkerSlot != -1) {
                            val shulker = modifiedShulkers.computeIfAbsent(source.sourceStack, k -> 
                                (ShulkerBox) ((BlockStateMeta) k.getItemMeta()).getBlockState()
                            );
                            shulker.getInventory().setItem(source.shulkerSlot, matched);
                        }
                        sourceStacksToReturn.add(source.sourceStack);
                    }
                }

                for (Map.Entry<ItemStack, ShulkerBox> entry : modifiedShulkers.entrySet()) {
                    val shulkerItem = entry.getKey();
                    val shulker = entry.getValue();
                    shulkerItem.editMeta(BlockStateMeta.class, bsm -> {
                        bsm.setBlockState(shulker);
                    });
                }

                PlayerUtils.give(p, new ArrayList<>(sourceStacksToReturn), true);
            }, null);
        });
    }

    public Response collect(String rawAmount) {
        final Player p = Bukkit.getPlayer(getOwnerUniqueId());
        if (p == null || !p.isOnline() || rawAmount == null)
            return Response.INVALID;
        final double dAmount = ConvertUtils.formatNumber(rawAmount);
        final int amount = (int) dAmount;
        if (dAmount == -1 || dAmount != amount) {
            p.sendRichMessage(cache.getInvalidInput());
            return Response.INVALID;
        }
        return collect(amount);
    }

    /// Must be called in the player region
    public Response collect(int amount) {
        final Player p = Bukkit.getPlayer(this.getOwnerUniqueId());
        if (p == null || !p.isOnline())
            return Response.INVALID;
        if (amount > cache.getMaxCollect() && !p.hasPermission("neonorder.bypass.max-collect")) {
            p.sendRichMessage(cache.getExceedMaxCollect());
            return Response.FAIL;
        }

        final int collectedInMinute = PDCUtils.getCollected(p);
        if (collectedInMinute > cache.getMaxCollectPerMinute()
                && !p.hasPermission("neonorder.bypass.max-collect-per-minute")) {
            p.sendRichMessage(cache.getCollectingTooFast());
            return Response.FAIL;
        }

        val event = new PlayerCollectItemsEvent(p, this, amount);
        if (!event.callEvent())
            return Response.FAIL;

        plugin.getStorage().collectItems(this, amount).thenAccept(success -> {
            p.getScheduler().run(plugin, task -> {
                if (success.equals(Boolean.FALSE)) {
                    p.sendRichMessage(cache.getInvalidInput());
                    return;
                }

                PDCUtils.setCollected(p, collectedInMinute + amount);

                // FIXME Player loses the items if they disconnect
                PlayerUtils.give(p, getItem().clone(), amount, true);
            }, null);
        });

        return Response.SCHEDULED;
    }

    public void cancel(Player p) {
        val event = new PlayerCancelOrderEvent(p, this);
        if (!event.callEvent())
            return;

        plugin.getStorage().cancelOrder(this).thenAccept(payBack -> {
            if (payBack.equals(-1.0)) {
                return;
            }
            this.expiresAt = System.currentTimeMillis() - 1;
            p.getScheduler().run(plugin, task -> {
                EconUtils.addMoney(Bukkit.getOfflinePlayer(getOwnerUniqueId()), payBack);
                p.sendRichMessage("<green>Order cancelled! Refunded <gold>$" + ConvertUtils.formatNumber(payBack) + "</gold>.");
                YourOrderGUI.open(p);
            }, null);
        });
    }

    private CompletableFuture<Integer> deliver(Player deliverer, int a) {
        final Function<Integer, Integer> func = exceeded -> {
            deliverer.getScheduler().run(plugin, task -> {
                final double earning = moneyPer * (a - exceeded);
                EconUtils.addMoney(deliverer, earning);
                deliverer.sendRichMessage(cache.getDelivered(),
                        Placeholder.unparsed("money", ConvertUtils.formatNumber(earning)));
                PlayerUtils.playSound(deliverer, cache.getDeliverSound());

                final Player ownerPlayer = Bukkit.getPlayer(owner);
                if (ownerPlayer != null && ownerPlayer.isOnline()) {
                    final ItemMeta meta = item.getItemMeta();
                    final Component displayName = meta == null ? null : meta.displayName();
                    assert item.getType().getItemTranslationKey() != null;
                    ownerPlayer.sendRichMessage(
                            cache.getReceiveDelivery(),
                            Placeholder.unparsed("deliverer", deliverer.getName()),
                            Placeholder.unparsed("amount", ConvertUtils.formatNumber(a)),
                            Placeholder.component("item",
                                    (displayName == null ? Component.translatable(item.getType().getItemTranslationKey())
                                            : displayName)));
                }
            }, null);
            return exceeded;
        };
        return plugin.getStorage().deliverOrder(this, a).thenApply(func);
    }

    public OrderStatus getStatus() {
        if (delivered >= amount)
            return OrderStatus.COMPLETED;
        if (expiresAt < System.currentTimeMillis())
            return OrderStatus.EXPIRED;
        return OrderStatus.AVAILABLE;
    }

    public boolean shouldBeDeleted() {
        return !isActive() && inStorage == 0;
    }

    public double getPaid() {
        return moneyPer * delivered;
    }

    @Override
    public UUID getOwnerUniqueId() {
        return owner;
    }

    @Override
    public void setDelivered(int delivered) {
        this.delivered = delivered;
        update("delivered", delivered);
    }

    @Override
    public void setInStorage(int inStorage) {
        this.inStorage = inStorage;
        update("in_storage", inStorage);
    }

    @Override
    public void setAmount(int amount) {
        this.amount = amount;
        update("amount", amount);
    }

    @Override
    public void setMoneyPer(double moneyPer) {
        this.moneyPer = moneyPer;
        update("money_per", moneyPer);
    }

    private void update(String var, Object value) {
        if (shouldBeDeleted()) {
            plugin.getStorage().deleteOrder(this);
            return;
        }
        plugin.getStorage().updateOrder(this, var, value);
    }

    public static Response create(Player p, ItemStack item, String rawMoneyPer, String rawAmount) {
        if (rawAmount == null || rawMoneyPer == null)
            return Response.INVALID;
        final double dAmount = ConvertUtils.formatNumber(rawAmount);
        final int amount = (int) dAmount;
        final double moneyPer = ConvertUtils.formatNumber(rawMoneyPer);
        if (dAmount == -1 || moneyPer == -1 || dAmount != amount)
            return Response.INVALID;

        return create(p, item, moneyPer, amount);
    }

    public static Response create(Player owner, ItemStack item, double moneyPer, int amount) {
        if (me.neonjava.neonorder.gui.YourOrderGUI.isAtLimit(owner)) {
            return Response.LIMIT_REACHED;
        }

        val event = new PlayerCreateOrderEvent(owner, item, moneyPer, amount);
        if (!event.callEvent())
            return Response.CANCELLED;

        if (!EconUtils.removeMoney(owner, moneyPer * amount)) {
            return Response.FAIL;
        }

        plugin.getStorage().createOrder(owner.getUniqueId(), item, amount, moneyPer)
                .exceptionally(err -> {
                    // DB insertion failed after money was already taken — refund the player
                    EconUtils.addMoney(owner, moneyPer * amount);
                    owner.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                            .deserialize("<red>Your order could not be placed due to a server error. Your money has been refunded."));
                    plugin.getServer().getLogger().severe("[NeonOrder] Failed to save order for " + owner.getName() + ": " + err.getMessage());
                    return null;
                });

        // Broadcast new order creation
        owner.getServer().getLogger().info("[Order] Broadcasting new order from " + owner.getName());
        net.kyori.adventure.text.Component broadcastMsg = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                "<gray>Player <white>" + owner.getName() + " <gray>has set an order for <white>" + amount
                        + "x <item><gray>!",
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component("item",
                        (item.getItemMeta() != null && item.getItemMeta().hasDisplayName()
                                ? item.getItemMeta().displayName()
                                : net.kyori.adventure.text.Component
                                        .translatable(item.getType().getItemTranslationKey()))));
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendActionBar(broadcastMsg);
        }

        return Response.SUCCESS;
    }

    public enum Response {
        INVALID,
        SUCCESS,
        FAIL,
        CANCELLED,
        SCHEDULED,
        LIMIT_REACHED
    }
}
