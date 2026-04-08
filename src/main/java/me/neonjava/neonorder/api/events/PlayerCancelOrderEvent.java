package me.neonjava.neonorder.api.events;

import me.neonjava.neonorder.api.Order;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlayerCancelOrderEvent extends NeonOrderPlayerEvent {

    public PlayerCancelOrderEvent(@NotNull Player player, @NotNull Order order) {
        super(player, order);
    }
}
