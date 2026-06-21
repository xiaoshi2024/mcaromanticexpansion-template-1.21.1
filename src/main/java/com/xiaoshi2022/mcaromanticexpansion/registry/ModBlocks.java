package com.xiaoshi2022.mcaromanticexpansion.registry;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.content.block.UmbrellaStandBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {
    // 使用 DeferredRegister.Blocks（注意是复数形式）
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(MCARomanticExpansion.MODID);

    // 注册雨伞架方块
    public static final DeferredBlock<UmbrellaStandBlock> UMBRELLA_STAND =
            registerBlocks("umbrella_stand",
                    () -> new UmbrellaStandBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE)
                            .noOcclusion()
                            .strength(2.0F)));

    // 辅助方法：注册方块并同时注册对应的 BlockItem
    private static <T extends Block> DeferredBlock<T> registerBlocks(String name, Supplier<T> block) {
        DeferredBlock<T> blocks = BLOCKS.register(name, block);
        registerBlockItems(name, blocks);
        return blocks;
    }

    // 辅助方法：注册 BlockItem
    private static <T extends Block> void registerBlockItems(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}