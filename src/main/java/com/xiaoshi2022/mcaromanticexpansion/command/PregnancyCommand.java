package com.xiaoshi2022.mcaromanticexpansion.command;

import com.mojang.brigadier.CommandDispatcher;
import com.xiaoshi2022.mcaromanticexpansion.event.PregnancyAttemptHandler;
import com.xiaoshi2022.mcaromanticexpansion.util.PregnancyManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class PregnancyCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pregnancy")
                .requires(source -> source.hasPermission(2))

                // 查看备孕期状态
                .then(Commands.literal("status")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayer();
                            if (player == null) return 0;

                            PregnancyManager.PregnancyData data = PregnancyManager.getPregnancyData(player.getUUID());
                            if (data != null && data.isActive()) {
                                long worldTime = player.serverLevel().getGameTime();
                                int elapsed = (int) (worldTime - data.getStartTime());
                                int remaining = data.getDurationTicks() - elapsed;

                                context.getSource().sendSuccess(() ->
                                        Component.translatable("mcaromanticexpansion.command.pregnancy.status.active",
                                                elapsed, data.getDurationTicks(), remaining), false);
                            } else {
                                context.getSource().sendSuccess(() ->
                                        Component.translatable("mcaromanticexpansion.command.pregnancy.status.inactive"), false);
                            }
                            return 1;
                        })
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                    PregnancyManager.PregnancyData data = PregnancyManager.getPregnancyData(target.getUUID());

                                    if (data != null && data.isActive()) {
                                        long worldTime = target.serverLevel().getGameTime();
                                        int elapsed = (int) (worldTime - data.getStartTime());
                                        int remaining = data.getDurationTicks() - elapsed;

                                        context.getSource().sendSuccess(() ->
                                                Component.translatable("mcaromanticexpansion.command.pregnancy.status.active.other",
                                                        target.getName().getString(), elapsed, data.getDurationTicks(), remaining), false);
                                    } else {
                                        context.getSource().sendSuccess(() ->
                                                Component.translatable("mcaromanticexpansion.command.pregnancy.status.inactive.other",
                                                        target.getName().getString()), false);
                                    }
                                    return 1;
                                })
                        )
                )

                // 手动完成备孕期
                .then(Commands.literal("complete")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayer();
                            if (player == null) return 0;

                            PregnancyManager.PregnancyData data = PregnancyManager.getPregnancyData(player.getUUID());
                            if (data != null && data.isActive()) {
                                PregnancyAttemptHandler.attemptPregnancy(player, data);
                                PregnancyManager.removePregnancyPeriod(player.getUUID());
                                context.getSource().sendSuccess(() ->
                                        Component.translatable("mcaromanticexpansion.command.pregnancy.complete.success"), true);
                            } else {
                                context.getSource().sendFailure(Component.translatable("mcaromanticexpansion.command.pregnancy.complete.failure"));
                            }
                            return 1;
                        })
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                    PregnancyManager.PregnancyData data = PregnancyManager.getPregnancyData(target.getUUID());

                                    if (data != null && data.isActive()) {
                                        PregnancyAttemptHandler.attemptPregnancy(target, data);
                                        PregnancyManager.removePregnancyPeriod(target.getUUID());
                                        context.getSource().sendSuccess(() ->
                                                Component.translatable("mcaromanticexpansion.command.pregnancy.complete.success.other",
                                                        target.getName().getString()), true);
                                    } else {
                                        context.getSource().sendFailure(Component.translatable("mcaromanticexpansion.command.pregnancy.complete.failure.other",
                                                target.getName().getString()));
                                    }
                                    return 1;
                                })
                        )
                )

                // 取消备孕期
                .then(Commands.literal("cancel")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayer();
                            if (player == null) return 0;

                            if (PregnancyManager.isPlayerInPregnancyPeriod(player.getUUID())) {
                                PregnancyManager.removePregnancyPeriod(player.getUUID());
                                context.getSource().sendSuccess(() ->
                                        Component.translatable("mcaromanticexpansion.command.pregnancy.cancel.success"), true);
                            } else {
                                context.getSource().sendFailure(Component.translatable("mcaromanticexpansion.command.pregnancy.cancel.failure"));
                            }
                            return 1;
                        })
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "target");

                                    if (PregnancyManager.isPlayerInPregnancyPeriod(target.getUUID())) {
                                        PregnancyManager.removePregnancyPeriod(target.getUUID());
                                        context.getSource().sendSuccess(() ->
                                                Component.translatable("mcaromanticexpansion.command.pregnancy.cancel.success.other",
                                                        target.getName().getString()), true);
                                    } else {
                                        context.getSource().sendFailure(Component.translatable("mcaromanticexpansion.command.pregnancy.cancel.failure.other",
                                                target.getName().getString()));
                                    }
                                    return 1;
                                })
                        )
                )
        );
    }
}
