package me.neonjava.neonorder.load;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.val;
import me.neonjava.neonorder.gui.AdminToolGUI;
import me.neonjava.neonorder.gui.MainGUI;
import me.neonjava.neonorder.obj.Order;
import me.neonjava.neonorder.obj.SortTypes;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;

import static me.neonjava.neonorder.load.NeonOrder.plugin;

@SuppressWarnings("UnstableApiUsage")
public class Bootstrapper implements PluginBootstrap {

    private static LiteralCommandNode<CommandSourceStack> getOrderCmd(String alias) {
        return Commands.literal(alias)
                .requires(predicate -> predicate.getExecutor() instanceof Player)
                .executes(ctx -> {
                    new MainGUI((Player) ctx.getSource().getExecutor(), 0);

                    return 1;
                })
                .then(Commands.argument("search", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            final String search = StringArgumentType.getString(ctx, "search");
                            new MainGUI((Player) ctx.getSource().getExecutor(), 0, search);
                            return 1;
                        })
                )
                .build();
    }

    private static LiteralCommandNode<CommandSourceStack> getNeonOrderCmd(String alias) {
        val builder = Commands.literal(alias);
        builder
                .requires(predicate -> (predicate.getSender().hasPermission("neonorder.admin")))
                .then(Commands.literal("reload")
                        .requires(predicate -> predicate.getSender().hasPermission("neonorder.admin.reload"))
                        .executes(ctx -> {
                            plugin.getConfigs().reload(() -> {

                                ctx.getSource().getSender().sendRichMessage("<green>NeonOrder reloaded");
                            });
                            return 1;
                        })
                )
                .then(Commands.literal("blacklist")
                        .requires(predicate ->
                                predicate.getExecutor() != null &&
                                predicate.getExecutor().hasPermission("neonorder.admin.blacklist") &&
                                predicate.getExecutor() instanceof Player)
                        .executes(ctx -> {
                            if (!(ctx.getSource().getExecutor() instanceof Player p)) return 0;

                            AdminToolGUI.openBlacklist(p);

                            return 1;
                        })
                )
                .then(Commands.literal("custom_items")
                        .requires(predicate ->
                                predicate.getExecutor() != null &&
                                        predicate.getExecutor().hasPermission("neonorder.admin.custom-items") &&
                                        predicate.getExecutor() instanceof Player)
                        .executes(ctx -> {
                            if (!(ctx.getSource().getExecutor() instanceof Player p)) return 0;

                            AdminToolGUI.openCustomItems(p);

                            return 1;
                        })
                );

        builder
                .then(Commands.literal("debug")
                        .executes(ctx -> {
                            val sender = ctx.getSource().getSender();
                            sender.sendMessage(Component.text("=== [NeonOrder Debug Info] ==="));
                            if (sender instanceof Player p) {
                                int protocol = me.neonjava.neonorder.utils.PlayerUtils.getPlayerProtocolVersion(p);
                                sender.sendMessage(Component.text("Player: " + p.getName()));
                                sender.sendMessage(Component.text("Protocol Version: " + protocol));
                                sender.sendMessage(Component.text("Is Bedrock (Geyser): " + me.neonjava.neonorder.utils.BedrockUtils.isBedrock(p)));
                                sender.sendMessage(Component.text("Will Use Paper Dialog: " + (plugin.getConfigs().isUseDialogAPI() && NeonOrder.hasDialogAPI && !me.neonjava.neonorder.utils.BedrockUtils.isBedrock(p) && protocol >= 772)));
                            }
                            var itemsAZ = plugin.getDataCache().getItems(SortTypes.A_Z);
                            sender.sendMessage(Component.text("Total Selectable Items: " + itemsAZ.size()));
                            if (!itemsAZ.isEmpty()) {
                                String first5 = itemsAZ.stream().limit(5).map(i -> i.getType().name()).reduce((a, b) -> a + ", " + b).orElse("");
                                sender.sendMessage(Component.text("Sample Items: " + first5));
                            }
                            return 1;
                        })
                )
                .then(Commands.literal("test")
                        .executes(ctx -> {
                            val sender = ctx.getSource().getExecutor();
                            if (sender == null) return 1;
                            sender.sendMessage(Component.text("Orders: " + plugin.getDataCache().getSortedOrders(SortTypes.MOST_MONEY_PER_ITEM).size()));
                            sender.sendMessage(Component.text("Blacklist: " + plugin.getDataCache().getBlacklist().size()));
                            sender.sendMessage(Component.text("Custom Items: " + plugin.getDataCache().getCustomItems().size()));
                            return 1;
                        })

                )
                .then(Commands.literal("orders")
                        .executes(ctx -> {
                            val sender = ctx.getSource().getExecutor();
                            if (!(sender instanceof Player player)) return 1;
                            for (Order order : plugin.getDataCache().getSortedOrders(SortTypes.MOST_MONEY_PER_ITEM)) {
                                sender.sendMessage(Component.text("#" + order.id + " amount=" + order.amount + " moneyPer=" + order.moneyPer + " delivered=" + order.delivered + " inStorage=" + order.inStorage + " expiresAt=" + order.expiresAt));
                                player.give(order.getItem());
                            }
                            return 1;
                        })
                );

        return builder.build();
    }

    @Override
    public void bootstrap(@NonNull BootstrapContext ctx) {
        ctx.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, e -> {
            e.registrar().register(getOrderCmd("orders"));
            e.registrar().register(getOrderCmd("order"));
            e.registrar().register(getNeonOrderCmd("neonorder"));
        });
    }

    @Override
    public @NonNull JavaPlugin createPlugin(@NonNull PluginProviderContext ctx) {
        return new NeonOrder();
    }

}
