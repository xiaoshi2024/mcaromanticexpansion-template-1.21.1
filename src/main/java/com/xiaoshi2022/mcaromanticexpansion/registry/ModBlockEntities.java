package com.xiaoshi2022.mcaromanticexpansion.registry;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MCARomanticExpansion.MODID);

    // 注释掉伞架方块实体
    // public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<UmbrellaStandBlockEntity>> UMBRELLA_STAND_BLOCK_ENTITY =
    //         BLOCK_ENTITY_TYPES.register("umbrella_stand",
    //                 () -> new BlockEntityType<>(
    //                         UmbrellaStandBlockEntity::new,
    //                         Set.of(ModBlocks.UMBRELLA_STAND.get())
    //                 )
    //         );

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}