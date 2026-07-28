package me.neonjava.neonorder.utils;

import me.neonjava.neonorder.data.ConfigCache;
import me.neonjava.neonorder.load.NeonOrder;
import me.neonjava.neonorder.obj.MoneyTransaction;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import static me.neonjava.neonorder.load.NeonOrder.plugin;

public class EconUtils {
    private static final MoneyTransaction currentTransaction = new MoneyTransaction();
//    private static DBManager db;
    private static ConfigCache cache;

    private static Economy getEco() {
        org.bukkit.plugin.ServicesManager sm = plugin.getServer().getServicesManager();
        for (org.bukkit.plugin.RegisteredServiceProvider<Economy> rsp : sm.getRegistrations(Economy.class)) {
            if (rsp.getProvider().getName().equalsIgnoreCase("NeonEconomy")) {
                return rsp.getProvider();
            }
        }
        org.bukkit.plugin.RegisteredServiceProvider<Economy> rsp = sm.getRegistration(Economy.class);
        return rsp != null ? rsp.getProvider() : null;
    }

    public static void init(NeonOrder plugin) {
//        db = plugin.getDbManager();
        cache = plugin.getConfigs();
    }

    public static void addMoney(OfflinePlayer p, double amount) {
        logTransactionBefore(p, amount);
        Economy eco = getEco();
        if (eco != null) eco.depositPlayer(p, amount);
        logTransactionAfter(p);
    }

    public static boolean removeMoney(Player p, double amount) {
        Economy eco = getEco();
        if (eco == null) return false;
        double bal = eco.getBalance(p);
        plugin.getLogger().info("Checking balance for " + p.getName() + ": has " + bal + ", needs " + amount);
        if (bal < amount) return false;
        logTransactionBefore(p, amount);
        eco.withdrawPlayer(p, amount);
        logTransactionAfter(p);
        return true;
    }

    private static void logTransactionBefore(OfflinePlayer p, double amount) {
        if (!cache.isLogTransactions()) return;
        Economy eco = getEco();
        if (eco == null) return;
        currentTransaction.player = p.getUniqueId();
        currentTransaction.before = eco.getBalance(p);
        currentTransaction.amount = amount;
    }

    private static void logTransactionAfter(OfflinePlayer p) {
        if (!cache.isLogTransactions()) return;
        Economy eco = getEco();
        if (eco == null) return;
        currentTransaction.after = eco.getBalance(p);
        plugin.getStorage().logTransaction(currentTransaction.player, currentTransaction.before, currentTransaction.amount, currentTransaction.after);
    }
}