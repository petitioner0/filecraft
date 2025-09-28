package com.petitioner0.filecraft.content;

import com.petitioner0.filecraft.Filecraft;
import com.petitioner0.filecraft.item.PathBinderItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.bus.api.IEventBus;

public class FilecraftItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Filecraft.MODID);

    public static final DeferredItem<Item> PATH_BINDER =
        ITEMS.registerItem("path_binder", props -> new PathBinderItem(props.stacksTo(1)));

    public static void init(IEventBus bus) { ITEMS.register(bus); }
}
