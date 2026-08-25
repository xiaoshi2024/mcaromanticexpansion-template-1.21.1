package com.xiaoshi2022.mcaromanticexpansion.registry;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, MCARomanticExpansion.MODID);

    // 注释掉伞架方块
    // public static final DeferredHolder<Block, UmbrellaStandBlock> UMBRELLA_STAND =
    //         BLOCKS.register("umbrella_stand",
    //                 () -> new UmbrellaStandBlock(
    //                         BlockBehaviour.Properties.of()
    //                                 .strength(2.0F)
    //                                 .noOcclusion()
    //                                 .pushReaction(PushReaction.DESTROY)
    //                                 .isRedstoneConductor((state, level, pos) -> false)
    //                                 .isSuffocating((state, level, pos) -> false)
    //                                 .isViewBlocking((state, level, pos) -> false)
    //                 )
    //         );

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}