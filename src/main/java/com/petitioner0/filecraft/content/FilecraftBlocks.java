package com.petitioner0.filecraft.content;

import com.petitioner0.filecraft.Filecraft;
import com.petitioner0.filecraft.fsblock.FileNodeBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class FilecraftBlocks {
    // 使用专用的 Helpers（会自动为 Properties 调用 setId）
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Filecraft.MODID);
    public static final DeferredRegister.Items  ITEMS  = DeferredRegister.createItems(Filecraft.MODID);

    // 自动 setId 的安全注册：registerBlock
    public static final DeferredBlock<FileNodeBlock> FILE_NODE =
        BLOCKS.registerBlock(
            "file_node",
            FileNodeBlock::new, // 构造器会接收下面这份 Properties
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(-1.0f, 3_600_000.0f)
                .noOcclusion()
                .noLootTable() // 技术方块不掉落，OK
        );

    static {
        // BlockItem 也用 Items 的 helper，一行到位
        ITEMS.registerSimpleBlockItem("file_node", FILE_NODE, new Item.Properties());
    }
}