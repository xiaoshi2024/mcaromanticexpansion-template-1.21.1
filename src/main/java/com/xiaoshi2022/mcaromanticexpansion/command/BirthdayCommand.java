// BirthdayCommand.java
package com.xiaoshi2022.mcaromanticexpansion.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.xiaoshi2022.mcaromanticexpansion.util.PlayerBirthdayData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class BirthdayCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("birthday")
                .requires(source -> source.hasPermission(0))

                // ========== 设置自己的生日 ==========
                .then(Commands.literal("set")
                        .then(Commands.argument("month", IntegerArgumentType.integer(1, 12))
                                .then(Commands.argument("day", IntegerArgumentType.integer(1, 31))
                                        .executes(context -> {
                                            int month = IntegerArgumentType.getInteger(context, "month");
                                            int day = IntegerArgumentType.getInteger(context, "day");
                                            PlayerBirthdayData.setBirthday(context.getSource().getPlayer(), month, day);
                                            context.getSource().sendSuccess(() ->
                                                    Component.translatable("mcaromanticexpansion.command.birthday.set.success", month, day), true);
                                            return 1;
                                        })
                                )
                        )
                )  // ← set 命令结束

                // ========== 查看生日（自己或他人） ==========
                .then(Commands.literal("check")
                        .executes(context -> {
                            // 无参数：查看自己的生日
                            var player = context.getSource().getPlayer();
                            if (player == null) return 0;
                            var birthday = PlayerBirthdayData.getBirthday(player);
                            if (birthday.isPresent()) {
                                context.getSource().sendSuccess(() ->
                                        Component.translatable("mcaromanticexpansion.command.birthday.check.success",
                                                birthday.get().getMonthValue(), birthday.get().getDayOfMonth()), false);
                            } else {
                                context.getSource().sendSuccess(() ->
                                        Component.translatable("mcaromanticexpansion.command.birthday.check.not_set"), false);
                            }
                            return 1;
                        })
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> {
                                    // 有参数：查看对方的生日
                                    ServerPlayer source = context.getSource().getPlayer();
                                    if (source == null) return 0;
                                    ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                    var birthday = PlayerBirthdayData.getBirthday(target);

                                    if (birthday.isPresent()) {
                                        source.sendSystemMessage(Component.translatable("mcaromanticexpansion.command.birthday.check.other.success",
                                                target.getName().getString(),
                                                birthday.get().getMonthValue(),
                                                birthday.get().getDayOfMonth()));
                                    } else {
                                        source.sendSystemMessage(Component.translatable("mcaromanticexpansion.command.birthday.check.other.not_set",
                                                target.getName().getString()));
                                    }
                                    return 1;
                                })
                        )
                )  // ← check 命令结束

                // ========== 检查今天是否是自己的生日 ==========
                .then(Commands.literal("today")
                        .executes(context -> {
                            var player = context.getSource().getPlayer();
                            if (player == null) return 0;
                            if (PlayerBirthdayData.isBirthdayToday(player)) {
                                context.getSource().sendSuccess(() ->
                                        Component.translatable("mcaromanticexpansion.command.birthday.today.success"), false);
                            } else {
                                var birthday = PlayerBirthdayData.getBirthday(player);
                                if (birthday.isPresent()) {
                                    context.getSource().sendSuccess(() ->
                                            Component.translatable("mcaromanticexpansion.command.birthday.today.not_birthday",
                                                    birthday.get().getMonthValue(), birthday.get().getDayOfMonth()), false);
                                } else {
                                    context.getSource().sendSuccess(() ->
                                            Component.translatable("mcaromanticexpansion.command.birthday.today.not_set"), false);
                                }
                            }
                            return 1;
                        })
                )  // ← today 命令结束

                // ========== 给手中的礼盒设定为生日礼盒 ==========
                .then(Commands.literal("setgift")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayer();
                            if (player == null) return 0;

                            ItemStack handItem = player.getMainHandItem();
                            if (handItem.getItem() instanceof com.xiaoshi2022.mcaromanticexpansion.item.GiftBoxItem) {
                                com.xiaoshi2022.mcaromanticexpansion.item.GiftBoxItem.setBirthdayGift(handItem);
                                context.getSource().sendSuccess(() ->
                                        Component.translatable("mcaromanticexpansion.command.birthday.setgift.success"), true);
                                return 1;
                            } else {
                                context.getSource().sendFailure(Component.translatable("mcaromanticexpansion.command.birthday.setgift.need_gift_box"));
                                return 0;
                            }
                        })
                )  // ← setgift 命令结束
        );
    }
}