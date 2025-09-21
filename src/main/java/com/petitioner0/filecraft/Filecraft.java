package com.petitioner0.filecraft;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.petitioner0.filecraft.content.FilecraftBlockEntities;
import com.petitioner0.filecraft.content.FilecraftBlocks;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Filecraft.MODID)
public class Filecraft {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "filecraft";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "filecraft" namespace
    public Filecraft(IEventBus modEventBus) {
        
        FilecraftBlocks.BLOCKS.register(modEventBus);
        FilecraftBlocks.ITEMS.register(modEventBus);
        FilecraftBlockEntities.register(modEventBus);

    }

}
