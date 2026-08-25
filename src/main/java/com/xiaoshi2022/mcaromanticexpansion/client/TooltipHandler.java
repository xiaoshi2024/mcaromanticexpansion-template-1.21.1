package com.xiaoshi2022.mcaromanticexpansion.client;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.util.RingNBTUtil;
import net.conczin.mca.item.EngagementRingItem;
import net.conczin.mca.item.WeddingRingItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = MCARomanticExpansion.MODID, value = Dist.CLIENT)
public class TooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        boolean isEngagementRing = stack.getItem() instanceof EngagementRingItem;
        boolean isWeddingRing = stack.getItem() instanceof WeddingRingItem;

        String partnerName = RingNBTUtil.getPartnerName(stack);

        if (partnerName != null && !partnerName.isEmpty()) {
            if (isEngagementRing) {
                event.getToolTip().add(1, Component.translatable("mcaromanticexpansion.ring.tooltip.engaged_to", partnerName)
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                // 删除这行调试日志
                // MCARomanticExpansion.LOGGER.debug("Added engagement ring tooltip for: {}", partnerName);
            } else if (isWeddingRing) {
                event.getToolTip().add(1, Component.translatable("mcaromanticexpansion.ring.tooltip.married_to", partnerName)
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
                event.getToolTip().add(2, Component.translatable("mcaromanticexpansion.ring.tooltip.blessing")
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
                // 删除这行调试日志
                // MCARomanticExpansion.LOGGER.debug("Added wedding ring tooltip for: {}", partnerName);
            }
        }
    }
}