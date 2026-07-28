package me.neonjava.neonorder.data;

import lombok.Getter;
import lombok.val;
import me.neonjava.neonorder.obj.Order;
import me.neonjava.neonorder.obj.Pair;
import me.neonjava.neonorder.obj.SortTypes;
import me.neonjava.neonorder.utils.AlgoUtils;
import me.neonjava.neonorder.utils.Log;
import me.neonjava.neonorder.utils.PDCUtils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;
import org.bukkit.Registry;
import org.bukkit.block.BlockType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public final class DataCache {

    private static final Registry<BlockType> BLOCK_REGISTRY = Registry.BLOCK;
    private final List<ItemStack> itemsAZ = new ArrayList<>();
    private final List<ItemStack> itemsZA = new ArrayList<>();

    @Getter
    private final HashSet<Pair<ItemStack, String>> customItems = new HashSet<>();
    @Getter
    private final HashSet<ItemStack> blacklist = new HashSet<>();

    private final TreeSet<Order> mostMoneyPerItem = new TreeSet<>(
            Comparator.comparingDouble(Order::getMoneyPer).reversed().thenComparing(Order::getId));
    private final TreeSet<Order> recentlyListed = new TreeSet<>(
            Comparator.comparingLong(Order::getExpiresAt).reversed().thenComparing(Order::getId));
    private final TreeSet<Order> mostDelivered = new TreeSet<>(
            Comparator.comparingInt(Order::getDelivered).reversed().thenComparing(Order::getId));
    private final TreeSet<Order> mostPaid = new TreeSet<>(
            Comparator.comparingDouble(Order::getPaid).reversed().thenComparing(Order::getId));

    private void setBlacklistAndCustomItems(Collection<ItemStack> blacklist,
            Collection<Pair<ItemStack, String>> customItems) {
        this.blacklist.clear();
        this.customItems.clear();
        this.blacklist.addAll(blacklist);
        this.customItems.addAll(customItems);
    }

    public void setItems(Collection<ItemStack> bukkitItems, Collection<ItemStack> blacklistedItems,
            Collection<Pair<ItemStack, String>> customItems) {
        itemsAZ.clear();
        itemsZA.clear();

        List<ItemStack> all = new ArrayList<>(bukkitItems);
        final List<ItemStack> parsedCustomItems = customItems.stream().map(item -> {
            final ItemStack stack = item.first.clone();
            stack.editMeta(meta -> PDCUtils.setSearch(meta, item.second));
            return stack;
        }).toList();
        all.addAll(parsedCustomItems);

        for (ItemStack item : all) {
            if (item == null || me.neonjava.neonorder.storage.Storage.isRestrictedItem(item.getType())) continue;
            boolean isBlacklisted = false;
            for (ItemStack black : blacklistedItems) {
                if (AlgoUtils.isSimilar(item, black)) {
                    isBlacklisted = true;
                    break;
                }
            }
            if (!isBlacklisted) {
                itemsAZ.add(item);
                itemsZA.add(item);
            }
        }

        itemsAZ.sort(AlgoUtils.getComparator(SortTypes.A_Z));
        itemsZA.sort(AlgoUtils.getComparator(SortTypes.Z_A));

        Log.info("Loaded " + itemsAZ.size() + " items.");
        setBlacklistAndCustomItems(blacklistedItems, customItems);
    }

    public void setOrders(Collection<Order> orders) {
        mostMoneyPerItem.clear();
        recentlyListed.clear();
        mostDelivered.clear();
        mostPaid.clear();
        mostMoneyPerItem.addAll(orders);
        recentlyListed.addAll(orders);
        mostDelivered.addAll(orders);
        mostPaid.addAll(orders);
    }

    public void updateOrder(Order order, double moneyPer, int amount, int delivered, int inStorage) {
        order.moneyPer = moneyPer;
        order.amount = amount;
        order.delivered = delivered;
        order.inStorage = inStorage;
        mostMoneyPerItem.remove(order);
        recentlyListed.remove(order);
        mostDelivered.remove(order);
        mostPaid.remove(order);
        mostMoneyPerItem.add(order);
        recentlyListed.add(order);
        mostDelivered.add(order);
        mostPaid.add(order);
    }

    public void deleteOrder(Order order) {
        mostMoneyPerItem.remove(order);
        recentlyListed.remove(order);
        mostDelivered.remove(order);
        mostPaid.remove(order);
    }

    public void addOrder(Order order) {
        mostMoneyPerItem.add(order);
        recentlyListed.add(order);
        mostDelivered.add(order);
        mostPaid.add(order);
    }

    public List<Order> getOrders(UUID ownerId) {
        val toDel = new ArrayList<Order>();
        val ownerOrders = mostMoneyPerItem.stream().filter(order -> {
            if (!order.getOwnerUniqueId().equals(ownerId))
                return false;
            if (order.shouldBeDeleted()) {
                toDel.add(order);
                return false;
            }
            return true;
        }).toList();
        toDel.forEach(this::deleteOrder);
        return ownerOrders;
    }

    public TreeSet<Order> getSortedOrders(SortTypes sortType) {
        switch (sortType) {
            case MOST_MONEY_PER_ITEM -> {
                return mostMoneyPerItem;
            }
            case RECENTLY_LISTED -> {
                return recentlyListed;
            }
            case MOST_DELIVERED -> {
                return mostDelivered;
            }
            case MOST_PAID -> {
                return mostPaid;
            }
            default -> {
                return mostMoneyPerItem;
            }
        }
    }

    public List<ItemStack> getItems(SortTypes sortType) {
        switch (sortType) {
            case A_Z -> {
                return itemsAZ;
            }
            case Z_A -> {
                return itemsZA;
            }
            default -> {
                return itemsAZ;
            }
        }
    }

    public BlockType getBlockType(@KeyPattern String identifier) {
        return BLOCK_REGISTRY.get(Key.key(identifier));
    }
}
