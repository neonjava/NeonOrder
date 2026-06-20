package me.neonjava.neonorder.utils;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import io.papermc.paper.dialog.Dialog;
import lombok.val;
import me.neonjava.neonorder.data.ConfigCache;
import me.neonjava.neonorder.load.NeonOrder;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Level;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PlayerUtils {
    private static NeonOrder plugin;
    private static ConfigCache cache;

    public static void init(NeonOrder plugin) {
        PlayerUtils.plugin = plugin;
        PlayerUtils.cache = plugin.getConfigs();
    }

    /**
     *
     * @param p     Player to give item to
     * @param items the items
     * @param safe  whether to schedule this with the player's scheduler or not
     */
    public static void give(Player p, Collection<ItemStack> items, boolean safe) {
        if (!safe) {
            p.give(items, true);
            return;
        }
        val location = p.getLocation();
        p.getScheduler().run(plugin, t -> p.give(items, true), () -> {
            Bukkit.getRegionScheduler().run(plugin, location, task -> {
                items.forEach(item -> location.getWorld().dropItemNaturally(location, item));
            });
        });
    }

    public static void drop(Player p, Collection<ItemStack> items) {
        val location = p.getEyeLocation();
        val direction = location.getDirection().multiply(0.3); // Shoot forward
        Bukkit.getRegionScheduler().run(plugin, location, task -> {
            items.forEach(item -> {
                val dropped = location.getWorld().dropItem(location, item);
                dropped.setVelocity(direction);
            });
        });
    }

    public static void give(Player p, ItemStack item, boolean safe) {
        PlayerUtils.give(p, Collections.singleton(item), safe);
    }

    public static void give(Player p, ItemStack item, int amount, boolean safe) {
        PlayerUtils.give(p, createStacks(item, amount), safe);
    }

    public static List<ItemStack> createStacks(ItemStack item, int amount) {
        final int maxStackSize = item.getMaxStackSize();
        final ItemStack copy = item.clone();
        copy.setAmount(maxStackSize);
        final int fullStackAmount = amount / maxStackSize;
        final List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < fullStackAmount; i++) {
            items.add(copy.clone());
        }
        final int rem = amount % maxStackSize;
        if (rem > 0) {
            copy.setAmount(rem);
            items.add(copy.clone());
        }
        return items;
    }

    public static void playSound(Player p, Sound s) {
        p.playSound(s);
    }

    public static void openGui(Player p, ChestGui gui) {
        p.getScheduler().run(plugin, t -> gui.show(p), null);
    }

    public static void openDialog(Player p, Dialog dialog) {
        p.getScheduler().run(plugin, t -> p.showDialog(dialog), null);
    }

    public static void closeInv(Player p) {
        p.getScheduler().run(plugin, t -> p.closeInventory(), null);
    }

    public static void clickNext(InventoryClickEvent e, ChestGui nextPage) {
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p))
            return;
        PlayerUtils.openGui(p, nextPage);
        PlayerUtils.playSound(p, cache.getNextPageSound());
    }

    public static void clickBack(InventoryClickEvent e, ChestGui previousPage) {
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p))
            return;
        PlayerUtils.openGui(p, previousPage);
        PlayerUtils.playSound(p, cache.getPreviousPageSound());
    }

    public static int getPlayerProtocolVersion(Player player) {
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("ViaVersion")) {
                Object viaApi = Class.forName("com.viaversion.viaversion.api.Via").getMethod("getAPI").invoke(null);
                if (viaApi != null) {
                    Class<?> viaApiClass = Class.forName("com.viaversion.viaversion.api.ViaAPI");
                    Object versionObj = viaApiClass.getMethod("getPlayerVersion", java.util.UUID.class).invoke(viaApi, player.getUniqueId());
                    if (versionObj instanceof Number) {
                        int ver = ((Number) versionObj).intValue();
                        NeonOrder.plugin.getLogger().info("[PlayerUtils] ViaVersion detected version: " + ver);
                        return ver;
                    }
                }
            }
        } catch (Throwable t) {
            NeonOrder.plugin.getLogger().log(Level.WARNING, "[PlayerUtils] ViaVersion version check failed", t);
        }
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
                Object protocolManager = Class.forName("com.comphenix.protocol.ProtocolLibrary").getMethod("getProtocolManager").invoke(null);
                if (protocolManager != null) {
                    Object versionObj = protocolManager.getClass().getMethod("getProtocolVersion", Player.class).invoke(protocolManager, player);
                    if (versionObj instanceof Number) {
                        int ver = ((Number) versionObj).intValue();
                        NeonOrder.plugin.getLogger().info("[PlayerUtils] ProtocolLib detected version: " + ver);
                        return ver;
                    }
                }
            }
        } catch (Throwable t) {
            NeonOrder.plugin.getLogger().log(Level.WARNING, "[PlayerUtils] ProtocolLib version check failed", t);
        }
        try {
            if (player instanceof com.destroystokyo.paper.network.NetworkClient) {
                int ver = ((com.destroystokyo.paper.network.NetworkClient) player).getProtocolVersion();
                NeonOrder.plugin.getLogger().info("[PlayerUtils] NetworkClient detected version: " + ver);
                return ver;
            }
        } catch (Throwable ignored) {}
        return -1;
    }
}
