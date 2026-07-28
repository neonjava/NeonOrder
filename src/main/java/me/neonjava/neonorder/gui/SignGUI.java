package me.neonjava.neonorder.gui;

import io.papermc.paper.math.Position;
import io.papermc.paper.event.packet.UncheckedSignChangeEvent;
import io.papermc.paper.math.BlockPosition;
import lombok.val;
import me.neonjava.neonorder.load.NeonOrder;
import me.neonjava.neonorder.obj.SignInfo;
import me.neonjava.neonorder.utils.Log;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockType;
import org.bukkit.block.Sign;
import org.bukkit.block.TileState;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("UnstableApiUsage")
public class SignGUI implements Listener {
    private static final HashMap<Player, SignInfo> sessionsList = new HashMap<>();
    private static MiniMessage mm;
    private static NeonOrder plugin;

    public static void newSession(Player p, Consumer<String> action, List<String> lines, BlockType blockType) {
        if (blockType == null) {
            Log.warn("Failed to show sign GUI because of invalid sign block");
            action.accept("");
            return;
        }
        if (sessionsList.containsKey(p)) return;

        final int x = (int) Math.floor(p.getX());
        final int y = (int) Math.floor(p.getEyeLocation().getY());
        final int z = (int) Math.floor(p.getZ());

        // PAPER PACKET SENDING IMPLEMENTATION
        Sign signState = (Sign) blockType.createBlockData().createBlockState();
        val frontSide = signState.getSide(Side.FRONT);
        for (int i = 0; i < 4; i++) frontSide.line(i, mm.deserialize(lines.get(i)));
        val loc = new Location(p.getWorld(), x, y, z);
        p.sendBlockChange(loc, signState.getBlockData());
        p.sendBlockUpdate(loc, signState);

        p.getScheduler().runDelayed(plugin, task -> {
            if (p.isOnline()) {
                p.openVirtualSign(Position.block(loc), Side.FRONT);
            }
        }, null, 2L);
        // END

        sessionsList.put(p, new SignInfo(action, blockType, Position.block(loc)));
    }

    public SignGUI(NeonOrder plugin) {
        mm = plugin.mm;
        SignGUI.plugin = plugin;
    }

    public static void shutdown() {
        sessionsList.clear();
        mm = null;
        plugin = null;
    }

    @EventHandler
    public void onSignChange(@NonNull UncheckedSignChangeEvent e) {
        final Player player = e.getPlayer();
        final SignInfo info = sessionsList.get(player);
        if (info == null) return;
        final BlockPosition pos = info.pos();
        if (!e.getEditedBlockPosition().equals(pos)) return;
        StringBuilder inputBuilder = new StringBuilder();
        for (net.kyori.adventure.text.Component lineComp : e.lines()) {
            String text = PlainTextComponentSerializer.plainText().serialize(lineComp).trim();
            if (text.contains("↑") || text.contains("^") || text.equalsIgnoreCase("Search")) continue;
            if (!text.isEmpty()) {
                if (inputBuilder.length() > 0) inputBuilder.append(" ");
                inputBuilder.append(text);
            }
        }
        final String fullInput = inputBuilder.toString().trim();
        info.action().accept(fullInput);

        sessionsList.remove(player);

        val world = player.getWorld();
        val loc = new Location(world, pos.blockX(), pos.blockY(), pos.blockZ());

        // PAPER
        Bukkit.getRegionScheduler().run(plugin, loc, t -> {
            player.sendBlockChange(loc, world.getBlockData(loc));
            if (world.getBlockState(loc) instanceof TileState tile) player.sendBlockUpdate(loc, tile);
        });
        // END
        e.setCancelled(true);
    }
}
