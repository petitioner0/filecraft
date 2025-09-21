package com.petitioner0.filecraft.content;

import com.petitioner0.filecraft.Filecraft;
import com.petitioner0.filecraft.fsblock.FileNodeBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class FilecraftBlocks {
    
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Filecraft.MODID);
    public static final DeferredRegister.Items  ITEMS  = DeferredRegister.createItems(Filecraft.MODID);

    
    public static final DeferredBlock<FileNodeBlock> FILE_NODE =
        BLOCKS.registerBlock(
            "file_node",
            FileNodeBlock::new, 
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(-1.0f, 3_600_000.0f)
                .noOcclusion()
                .noLootTable() 
        );

    static {
        
        ITEMS.registerSimpleBlockItem("file_node", FILE_NODE, new Item.Properties());
    }
}