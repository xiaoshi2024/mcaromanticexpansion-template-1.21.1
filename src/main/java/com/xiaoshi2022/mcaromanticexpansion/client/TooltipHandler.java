package com.xiaoshi2022.mcaromanticexpansion.client;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.util.RingNBTUtil;
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
        String partnerName = RingNBTUtil.getPartnerName(stack);

        if (partnerName != null && !partnerName.isEmpty()) {
            String itemId = stack.getItem().toString();

            if (itemId.contains("engagement")) {
                event.getToolTip().add(Component.translatable("mcaromanticexpansion.ring.tooltip.engaged_to", partnerName)
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            } else if (itemId.contains("wedding")) {
                event.getToolTip().add(Component.translatable("mcaromanticexpansion.ring.tooltip.married_to", partnerName)
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
                event.getToolTip().add(Component.translatable("mcaromanticexpansion.ring.tooltip.blessing")
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
            }
        }
    }
}