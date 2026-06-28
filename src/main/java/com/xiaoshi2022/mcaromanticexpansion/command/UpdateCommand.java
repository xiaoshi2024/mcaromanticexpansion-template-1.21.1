package com.xiaoshi2022.mcaromanticexpansion.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.util.ModrinthUpdateChecker;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class UpdateCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mcaupdate")
                .requires(source -> source.hasPermission(0)) // 所有玩家可用
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    if (source.getEntity() instanceof ServerPlayer player) {
                        ModrinthUpdateChecker.checkNow(player);
                    } else {
                        source.sendSuccess(() ->
                                Component.literal("§c该命令只能由玩家执行！"), false);
                    }
                    return 1;
                })
                .then(Commands.literal("check")
                        .executes(context -> {
                            if (context.getSource().getEntity() instanceof ServerPlayer player) {
                                ModrinthUpdateChecker.checkNow(player);
                            }
                            return 1;
                        })
                )
                .then(Commands.literal("status")
                        .executes(context -> {
                            if (context.getSource().getEntity() instanceof ServerPlayer player) {
                                String latest = ModrinthUpdateChecker.getLatestVersion();
                                if (latest != null && !latest.equals(MCARomanticExpansion.MOD_VERSION)) {
                                    player.sendSystemMessage(Component.literal(
                                            "§a有新版本可用！当前: §c" + MCARomanticExpansion.MOD_VERSION +
                                                    " §a→ §e" + latest
                                    ));
                                } else if (latest != null) {
                                    player.sendSystemMessage(Component.literal(
                                            "§a你已经是最新版本！(v" + latest + ")"
                                    ));
                                } else {
                                    player.sendSystemMessage(Component.literal(
                                            "§e尚未检查更新，请使用 §6/mcaupdate check §e检查"
                                    ));
                                }
                            }
                            return 1;
                        })
                )
        );
    }
}