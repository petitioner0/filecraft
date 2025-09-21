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

    
    public static final Supplier<BlockEntityType<FileNodeBlockEntity>> FILE_NODE =
            BLOCK_ENTITY_TYPES.register("file_node",
                    () -> new BlockEntityType<>(
                            FileNodeBlockEntity::new, 
                            false,                    
                            FilecraftBlocks.FILE_NODE.get() 
                    )
            );

    public static void register(IEventBus bus) {
        BLOCK_ENTITY_TYPES.register(bus);
    }
}
