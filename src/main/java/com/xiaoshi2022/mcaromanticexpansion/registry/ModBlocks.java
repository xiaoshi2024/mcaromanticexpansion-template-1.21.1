package com.xiaoshi2022.mcaromanticexpansion.registry;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.content.block.UmbrellaStandBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    // 关键：使用 DeferredRegister.Blocks
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(MCARomanticExpansion.MODID);

    // 关键：使用 DeferredBlock + registerBlock
    public static final DeferredBlock<UmbrellaStandBlock> UMBRELLA_STAND =
            BLOCKS.registerBlock("umbrella_stand",
                    UmbrellaStandBlock::new
            );

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}