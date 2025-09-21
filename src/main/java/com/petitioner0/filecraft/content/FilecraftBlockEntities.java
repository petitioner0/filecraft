package com.petitioner0.filecraft.content;

import java.util.function.Supplier;

import com.petitioner0.filecraft.Filecraft;
import com.petitioner0.filecraft.fsblock.FileNodeBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class FilecraftBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Filecraft.MODID);

    // 推荐：用 Supplier，而不是显式的 DeferredHolder 泛型
    public static final Supplier<BlockEntityType<FileNodeBlockEntity>> FILE_NODE =
            BLOCK_ENTITY_TYPES.register("file_node",
                    () -> new BlockEntityType<>(
                            FileNodeBlockEntity::new, // 工厂
                            false,                    // 仅 OP 才能从 NBT 放置，通常为 false
                            FilecraftBlocks.FILE_NODE.get() // 关联的方块
                    )
            );

    public static void register(IEventBus bus) {
        BLOCK_ENTITY_TYPES.register(bus);
    }
}
