package com.xiaoshi2022.mcaromanticexpansion.registry;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.content.block.UmbrellaStandBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    // BlockEntityType 使用 DeferredRegister 注册
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MCARomanticExpansion.MODID);

    // 注册雨伞架 BlockEntity
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<UmbrellaStandBlockEntity>> UMBRELLA_STAND_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("umbrella_stand",
                    () -> BlockEntityType.Builder.of(
                            UmbrellaStandBlockEntity::new,
                            ModBlocks.UMBRELLA_STAND.value()
                    ).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}