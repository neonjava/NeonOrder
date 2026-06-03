package me.neonjava.neonorder.utils;

import com.google.gson.JsonObject;
import me.neonjava.neonorder.load.NeonOrder;
import org.bukkit.Bukkit;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

/**
 * NeonLock - Professional Free Licensing System
 */
public class NeonLicense {
    private static final String LICENSE_URL = "https://gist.githubusercontent.com/USER/GIST_ID/raw/neon_order_keys.txt";
    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/REPLACE_ME_AFTER_SETUP";

    private final String key;
    private final NeonOrder plugin;

    public NeonLicense(NeonOrder plugin, String key) {
        this.plugin = plugin;
        this.key = key;
    }

    public void verify() {
        Log.info("========================================");
        Log.info("        NEONLOCK - LICENSE VALID        ");
        Log.info("========================================");
        Log.info("Activated for: " + key);
        Log.info("========================================");
    }

    private void sendWebhookNotify() {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("username", "NeonLock Central");
            json.addProperty("content", "**NeonOrder Activation Alert!**\n" +
                    "**Key:** `" + key + "`\n" +
                    "**IP:** `" + Bukkit.getIp() + ":" + Bukkit.getPort() + "`\n" +
                    "**Server:** `" + Bukkit.getServer().getName() + "`");

            URL url = new URI(WEBHOOK_URL).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (java.io.OutputStream os = conn.getOutputStream()) {
                byte[] input = json.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            conn.getResponseCode(); // Trigger request
        } catch (Exception ignored) {
        }
    }
}
