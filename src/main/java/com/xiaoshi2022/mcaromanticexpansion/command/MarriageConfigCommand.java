// 文件路径: src/main/java/com/xiaoshi2022/mcaromanticexpansion/command/MarriageConfigCommand.java

package com.xiaoshi2022.mcaromanticexpansion.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.xiaoshi2022.mcaromanticexpansion.util.MarriageConfig;
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

                                    String key = allow
                                            ? "command.mcaromanticexpansion.marriageconfig.enabled"
                                            : "command.mcaromanticexpansion.marriageconfig.disabled";
                                    context.getSource().sendSuccess(() -> Component.translatable(key), true);
                                    return 1;
                                })
                        )
                )
                // 备孕是否必须先结婚
                .then(Commands.literal("pregnancyRequiresMarriage")
                        .then(Commands.argument("require", BoolArgumentType.bool())
                                .executes(context -> {
                                    boolean require = BoolArgumentType.getBool(context, "require");
                                    MarriageConfig.setPregnancyRequiresMarriage(require);

                                    String key = require
                                            ? "command.mcaromanticexpansion.marriageconfig.pregnancy_requires_marriage.enabled"
                                            : "command.mcaromanticexpansion.marriageconfig.pregnancy_requires_marriage.disabled";
                                    context.getSource().sendSuccess(() -> Component.translatable(key), true);
                                    return 1;
                                })
                        )
                )
                // 在 MarriageConfigCommand.java 中添加
                .then(Commands.literal("reload")
                        .executes(context -> {
                            MarriageConfig.reload();
                            context.getSource().sendSuccess(() ->
                                    Component.translatable("command.mcaromanticexpansion.marriageconfig.reloaded"), true);
                            return 1;
                        })
                )
                .then(Commands.literal("player")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.literal("allowSameGender")
                                        .then(Commands.argument("allow", BoolArgumentType.bool())
                                                .executes(context -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                                    boolean allow = BoolArgumentType.getBool(context, "allow");
                                                    MarriageConfig.setPlayerAllowSameGenderMarriage(target.getName().getString(), allow);

                                                    String key = allow
                                                            ? "command.mcaromanticexpansion.marriageconfig.player.allowed"
                                                            : "command.mcaromanticexpansion.marriageconfig.player.denied";
                                                    context.getSource().sendSuccess(() ->
                                                            Component.translatable(key, target.getName().getString()), true);
                                                    return 1;
                                                })
                                        )
                                )
                                .then(Commands.literal("reset")
                                        .executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                            MarriageConfig.setPlayerAllowSameGenderMarriage(target.getName().getString(), null);

                                            context.getSource().sendSuccess(() ->
                                                    Component.translatable("command.mcaromanticexpansion.marriageconfig.player.reset",
                                                            target.getName().getString()), true);
                                            return 1;
                                        })
                                )
                                // per-player 备孕需结婚覆盖
                                .then(Commands.literal("pregnancyRequiresMarriage")
                                        .then(Commands.argument("require", BoolArgumentType.bool())
                                                .executes(context -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                                    boolean require = BoolArgumentType.getBool(context, "require");
                                                    MarriageConfig.setPlayerPregnancyRequiresMarriage(target.getName().getString(), require);

                                                    String key = require
                                                            ? "command.mcaromanticexpansion.marriageconfig.player.pregnancy_requires_marriage.required"
                                                            : "command.mcaromanticexpansion.marriageconfig.player.pregnancy_requires_marriage.exempt";
                                                    context.getSource().sendSuccess(() ->
                                                            Component.translatable(key, target.getName().getString()), true);
                                                    return 1;
                                                })
                                        )
                                )
                                .then(Commands.literal("resetPregnancy")
                                        .executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                            MarriageConfig.setPlayerPregnancyRequiresMarriage(target.getName().getString(), null);

                                            context.getSource().sendSuccess(() ->
                                                    Component.translatable("command.mcaromanticexpansion.marriageconfig.player.reset_pregnancy",
                                                            target.getName().getString()), true);
                                            return 1;
                                        })
                                )
                        )
                )
                .then(Commands.literal("status")
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();

                            source.sendSuccess(() -> Component.translatable("command.mcaromanticexpansion.marriageconfig.status.title"), false);

                            boolean global = MarriageConfig.isGlobalAllowSameGenderMarriage();
                            String globalKey = global
                                    ? "command.mcaromanticexpansion.marriageconfig.status.global.enabled"
                                    : "command.mcaromanticexpansion.marriageconfig.status.global.disabled";
                            source.sendSuccess(() -> Component.translatable(globalKey), false);

                            boolean pregRequire = MarriageConfig.isPregnancyRequiresMarriage();
                            String pregKey = pregRequire
                                    ? "command.mcaromanticexpansion.marriageconfig.status.pregnancy_requires_marriage.enabled"
                                    : "command.mcaromanticexpansion.marriageconfig.status.pregnancy_requires_marriage.disabled";
                            source.sendSuccess(() -> Component.translatable(pregKey), false);

                            var overrides = MarriageConfig.getAllPlayerOverrides();
                            if (!overrides.isEmpty()) {
                                source.sendSuccess(() -> Component.translatable("command.mcaromanticexpansion.marriageconfig.status.overrides"), false);
                                for (var entry : overrides.entrySet()) {
                                    String entryKey = entry.getValue()
                                            ? "command.mcaromanticexpansion.marriageconfig.status.override_entry.allowed"
                                            : "command.mcaromanticexpansion.marriageconfig.status.override_entry.denied";
                                    source.sendSuccess(() -> Component.translatable(entryKey, entry.getKey()), false);
                                }
                            } else {
                                source.sendSuccess(() -> Component.translatable("command.mcaromanticexpansion.marriageconfig.status.no_overrides"), false);
                            }

                            var pregOverrides = MarriageConfig.getAllPlayerPregnancyOverrides();
                            if (!pregOverrides.isEmpty()) {
                                source.sendSuccess(() -> Component.translatable("command.mcaromanticexpansion.marriageconfig.status.pregnancy_overrides"), false);
                                for (var entry : pregOverrides.entrySet()) {
                                    String entryKey = entry.getValue()
                                            ? "command.mcaromanticexpansion.marriageconfig.status.pregnancy_override_entry.required"
                                            : "command.mcaromanticexpansion.marriageconfig.status.pregnancy_override_entry.exempt";
                                    source.sendSuccess(() -> Component.translatable(entryKey, entry.getKey()), false);
                                }
                            } else {
                                source.sendSuccess(() -> Component.translatable("command.mcaromanticexpansion.marriageconfig.status.no_pregnancy_overrides"), false);
                            }
                            return 1;
                        })
                )
                .then(Commands.literal("help")
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            source.sendSuccess(() -> Component.translatable("command.mcaromanticexpansion.marriageconfig.help.title"), false);
                            source.sendSuccess(() -> Component.translatable("command.mcaromanticexpansion.marriageconfig.help.allow_same_gender"), false);
                            source.sendSuccess(() -> Component.translatable("command.mcaromanticexpansion.marriageconfig.help.pregnancy_requires_marriage"), false);
                            source.sendSuccess(() -> Component.translatable("command.mcaromanticexpansion.marriageconfig.help.player_allow"), false);
                            source.sendSuccess(() -> Component.translatable("command.mcaromanticexpansion.marriageconfig.help.player_reset"), false);
                            source.sendSuccess(() -> Component.translatable("command.mcaromanticexpansion.marriageconfig.help.player_pregnancy_requires_marriage"), false);
                            source.sendSuccess(() -> Component.translatable("command.mcaromanticexpansion.marriageconfig.help.player_reset_pregnancy"), false);
                            source.sendSuccess(() -> Component.translatable("command.mcaromanticexpansion.marriageconfig.help.status"), false);
                            return 1;
                        })
                )
        );
    }
}