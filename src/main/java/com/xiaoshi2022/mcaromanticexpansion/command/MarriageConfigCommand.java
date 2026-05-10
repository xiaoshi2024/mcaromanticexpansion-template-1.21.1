// 文件路径: src/main/java/com/xiaoshi2022/mcaromanticexpansion/command/MarriageConfigCommand.java

package com.xiaoshi2022.mcaromanticexpansion.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.xiaoshi2022.mcaromanticexpansion.util.MarriageConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class MarriageConfigCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("marriageconfig")
                .requires(source -> source.hasPermission(2)) // 需要 OP 权限（2级）
                .then(Commands.literal("allowSameGender")
                        .then(Commands.argument("allow", BoolArgumentType.bool())
                                .executes(context -> {
                                    boolean allow = BoolArgumentType.getBool(context, "allow");
                                    MarriageConfig.setGlobalAllowSameGenderMarriage(allow);

                                    String message = allow ? "§a已启用同性结婚（全局）" : "§c已禁用同性结婚（全局）";
                                    context.getSource().sendSuccess(() -> Component.literal(message), true);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("player")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.literal("allowSameGender")
                                        .then(Commands.argument("allow", BoolArgumentType.bool())
                                                .executes(context -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                                    boolean allow = BoolArgumentType.getBool(context, "allow");
                                                    MarriageConfig.setPlayerAllowSameGenderMarriage(target.getName().getString(), allow);

                                                    String message = allow ? "§a已允许 " + target.getName().getString() + " 同性结婚（覆盖全局）"
                                                            : "§c已禁止 " + target.getName().getString() + " 同性结婚（覆盖全局）";
                                                    context.getSource().sendSuccess(() -> Component.literal(message), true);
                                                    return 1;
                                                })
                                        )
                                )
                                .then(Commands.literal("reset")
                                        .executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                            MarriageConfig.setPlayerAllowSameGenderMarriage(target.getName().getString(), null);

                                            context.getSource().sendSuccess(() ->
                                                    Component.literal("§a已重置 " + target.getName().getString() + " 的同性结婚权限，现在遵循全局设置"), true);
                                            return 1;
                                        })
                                )
                        )
                )
                .then(Commands.literal("status")
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();

                            source.sendSuccess(() -> Component.literal("§6=== 同性结婚配置状态 ==="), false);

                            boolean global = MarriageConfig.isGlobalAllowSameGenderMarriage();
                            source.sendSuccess(() -> Component.literal("§e全局设置: " + (global ? "§a启用" : "§c禁用")), false);

                            var overrides = MarriageConfig.getAllPlayerOverrides();
                            if (!overrides.isEmpty()) {
                                source.sendSuccess(() -> Component.literal("§e玩家覆盖设置:"), false);
                                for (var entry : overrides.entrySet()) {
                                    source.sendSuccess(() -> Component.literal("  §7- " + entry.getKey() + ": " +
                                            (entry.getValue() ? "§a允许" : "§c禁止")), false);
                                }
                            } else {
                                source.sendSuccess(() -> Component.literal("§7暂无玩家覆盖设置"), false);
                            }
                            return 1;
                        })
                )
                .then(Commands.literal("help")
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            source.sendSuccess(() -> Component.literal("§6=== 同性结婚配置指令帮助 ==="), false);
                            source.sendSuccess(() -> Component.literal("§7/marriageconfig allowSameGender <true/false> - 设置全局同性结婚权限"), false);
                            source.sendSuccess(() -> Component.literal("§7/marriageconfig player <玩家> allowSameGender <true/false> - 设置玩家覆盖"), false);
                            source.sendSuccess(() -> Component.literal("§7/marriageconfig player <玩家> reset - 重置玩家覆盖（使用全局设置）"), false);
                            source.sendSuccess(() -> Component.literal("§7/marriageconfig status - 查看当前配置状态"), false);
                            return 1;
                        })
                )
        );
    }
}