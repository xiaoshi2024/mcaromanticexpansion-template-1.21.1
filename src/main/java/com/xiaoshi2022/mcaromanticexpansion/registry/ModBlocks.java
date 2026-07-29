package com.xiaoshi2022.mcaromanticexpansion.registry;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.content.block.UmbrellaStandBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    // 使用 Forge 的 DeferredRegister
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MCARomanticExpansion.MODID);

    // 注册雨伞架方块 - 使用 RegistryObject
    public static final RegistryObject<UmbrellaStandBlock> UMBRELLA_STAND =
            registerBlocks("umbrella_stand",
                    () -> new UmbrellaStandBlock(BlockBehaviour.Properties.copy(Blocks.OAK_FENCE)
                            .noOcclusion()
                            .strength(2.0F)));

    // 辅助方法：注册方块并同时注册对应的 BlockItem
    private static <T extends Block> RegistryObject<T> registerBlocks(String name, Supplier<T> block) {
        RegistryObject<T> blockObject = BLOCKS.register(name, block);
        registerBlockItems(name, blockObject);
        return blockObject;
    }

    // 辅助方法：注册 BlockItem
    private static <T extends Block> void registerBlockItems(String name, RegistryObject<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}