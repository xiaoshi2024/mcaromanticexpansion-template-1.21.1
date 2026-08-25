//package com.xiaoshi2022.mcaromanticexpansion.command;
//
//import com.mojang.brigadier.CommandDispatcher;
//import com.mojang.brigadier.arguments.BoolArgumentType;
//import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
//import com.xiaoshi2022.mcaromanticexpansion.util.ModrinthUpdateChecker;
//import com.xiaoshi2022.mcaromanticexpansion.util.UpdateConfig;
//import net.minecraft.commands.CommandSourceStack;
//import net.minecraft.commands.Commands;
//import net.minecraft.network.chat.Component;
//import net.minecraft.server.level.ServerPlayer;
//
//public class UpdateCommand {
//    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
//        dispatcher.register(Commands.literal("mcaupdate")
//                .requires(source -> source.hasPermission(0)) // 所有玩家可用
//                .executes(context -> {
//                    CommandSourceStack source = context.getSource();
//                    if (source.getEntity() instanceof ServerPlayer player) {
//                        ModrinthUpdateChecker.checkNow(player);
//                    } else {
//                        source.sendSuccess(() ->
//                                Component.translatable("message.mcaromanticexpansion.update.player_only"), false);
//                    }
//                    return 1;
//                })
//                .then(Commands.literal("check")
//                        .executes(context -> {
//                            if (context.getSource().getEntity() instanceof ServerPlayer player) {
//                                ModrinthUpdateChecker.checkNow(player);
//                            }
//                            return 1;
//                        })
//                )
//                .then(Commands.literal("status")
//                        .executes(context -> {
//                            if (context.getSource().getEntity() instanceof ServerPlayer player) {
//                                String latest = ModrinthUpdateChecker.getLatestVersion();
//                                if (latest != null && !latest.equals(MCARomanticExpansion.MOD_VERSION)) {
//                                    player.sendSystemMessage(Component.translatable(
//                                            "message.mcaromanticexpansion.update.available",
//                                            MCARomanticExpansion.MOD_VERSION, latest));
//                                } else if (latest != null) {
//                                    player.sendSystemMessage(Component.translatable(
//                                            "message.mcaromanticexpansion.update.latest", latest));
//                                } else {
//                                    player.sendSystemMessage(Component.translatable(
//                                            "message.mcaromanticexpansion.update.not_checked"));
//                                }
//                            }
//                            return 1;
//                        })
//                )
//                .then(Commands.literal("notify")
//                        .executes(context -> {
//                            if (context.getSource().getEntity() instanceof ServerPlayer player) {
//                                boolean enabled = UpdateConfig.isNotificationEnabled(player.getName().getString());
//                                String key = enabled
//                                        ? "message.mcaromanticexpansion.update.notify.status.enabled"
//                                        : "message.mcaromanticexpansion.update.notify.status.disabled";
//                                player.sendSystemMessage(Component.translatable(key));
//                            } else {
//                                context.getSource().sendSuccess(() ->
//                                        Component.translatable("message.mcaromanticexpansion.update.player_only"), false);
//                            }
//                            return 1;
//                        })
//                        .then(Commands.argument("enabled", BoolArgumentType.bool())
//                                .executes(context -> {
//                                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
//                                        boolean enabled = BoolArgumentType.getBool(context, "enabled");
//                                        UpdateConfig.setNotificationEnabled(player.getName().getString(), enabled);
//                                        String key = enabled
//                                                ? "message.mcaromanticexpansion.update.notify.enabled"
//                                                : "message.mcaromanticexpansion.update.notify.disabled";
//                                        player.sendSystemMessage(Component.translatable(key));
//                                    } else {
//                                        context.getSource().sendSuccess(() ->
//                                                Component.translatable("message.mcaromanticexpansion.update.player_only"), false);
//                                    }
//                                    return 1;
//                                })
//                        )
//                )
//        );
//    }
//}