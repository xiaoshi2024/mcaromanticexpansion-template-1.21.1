package com.xiaoshi2022.mcaromanticexpansion.client;

import com.xiaoshi2022.mcaromanticexpansion.client.gui.LoveLetterEditScreen;
import com.xiaoshi2022.mcaromanticexpansion.client.gui.LoveLetterReadScreen;
import com.xiaoshi2022.mcaromanticexpansion.item.LoveLetterItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 客户端辅助类，通过反射被 LoveLetterItem 调用以打开 GUI
 * 直接从 ItemStack 读取 NBT，避免传递过多参数
 */
@OnlyIn(Dist.CLIENT)
public class LoveLetterClient {

    @OnlyIn(Dist.CLIENT)
    public static void openScreen(InteractionHand hand, ItemStack stack, boolean editMode) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (editMode) {
                mc.setScreen(new LoveLetterEditScreen(hand,
                        LoveLetterItem.getRecipient(stack),
                        LoveLetterItem.getMessage(stack)));
            } else {
                mc.setScreen(new LoveLetterReadScreen(hand, stack));
            }
        });
    }
}