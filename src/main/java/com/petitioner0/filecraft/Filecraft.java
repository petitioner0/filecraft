package com.petitioner0.filecraft;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.petitioner0.filecraft.content.FilecraftBlockEntities;
import com.petitioner0.filecraft.content.FilecraftBlocks;
import com.petitioner0.filecraft.content.FilecraftItems;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Filecraft.MODID)
public class Filecraft {
    public static final String MODID = "filecraft";
    public static final Logger LOGGER = LogUtils.getLogger();
    public Filecraft(IEventBus modEventBus) {
        
        FilecraftBlocks.BLOCKS.register(modEventBus);
        FilecraftBlocks.ITEMS.register(modEventBus);
        FilecraftItems.ITEMS.register(modEventBus);
        FilecraftBlockEntities.register(modEventBus);
        ModCreativeTabs.init(modEventBus);

    }

}
