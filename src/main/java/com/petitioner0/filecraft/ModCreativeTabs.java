package com.petitioner0.filecraft;

import com.petitioner0.filecraft.content.FilecraftBlocks;
import com.petitioner0.filecraft.content.FilecraftItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Filecraft.MODID);


    public static final Supplier<CreativeModeTab> FILECRAFT_TAB = TABS.register("filecraft",
            () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.filecraft.filecraft")) 
                .icon(() -> new net.minecraft.world.item.ItemStack(FilecraftBlocks.FILE_NODE.get())) 
                .displayItems((params, output) -> {
                    output.accept(FilecraftItems.PATH_BINDER.get());
                    output.accept(FilecraftItems.FILE_MOVER.get());
                    output.accept(FilecraftBlocks.FILE_NODE.get()); 
                })
                .build()
    );

    public static void init(IEventBus bus) { TABS.register(bus); }
}