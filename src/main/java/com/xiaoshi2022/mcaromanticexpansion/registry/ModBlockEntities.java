package com.xiaoshi2022.mcaromanticexpansion.registry;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.content.block.UmbrellaStandBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    // 使用 Forge 的 DeferredRegister
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MCARomanticExpansion.MODID);

    // 注册雨伞架 BlockEntity - 使用 RegistryObject
    public static final RegistryObject<BlockEntityType<UmbrellaStandBlockEntity>> UMBRELLA_STAND_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("umbrella_stand",
                    () -> BlockEntityType.Builder.of(
                            UmbrellaStandBlockEntity::new,
                            ModBlocks.UMBRELLA_STAND.get()
                    ).build(null));
}