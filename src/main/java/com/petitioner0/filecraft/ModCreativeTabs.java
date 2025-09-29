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
    // 注册表：CREATIVE_MODE_TAB
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Filecraft.MODID);

    // 你的自定义标签：filecraft
    public static final Supplier<CreativeModeTab> FILECRAFT_TAB = TABS.register("filecraft",
            () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.filecraft.filecraft")) // 本地化键
                .icon(() -> new net.minecraft.world.item.ItemStack(FilecraftBlocks.FILE_NODE.get())) // 标签图标
                .displayItems((params, output) -> {
                    // 往标签里塞东西（ItemLike / ItemStack 都可以）
                    output.accept(FilecraftItems.PATH_BINDER.get());
                    output.accept(FilecraftBlocks.FILE_NODE.get()); // 方块会自动显示其物品形式
                })
                .build()
    );

    public static void init(IEventBus bus) { TABS.register(bus); }
}