package me.neonjava.neonorder.utils;

import org.bukkit.entity.Player;

public class BedrockUtils {
    public static boolean isBedrock(Player p) {
        // Simple prefix check as requested for the initial-commit style
        String name = p.getName();
        return name.startsWith(".") || name.startsWith("*");
    }
}
