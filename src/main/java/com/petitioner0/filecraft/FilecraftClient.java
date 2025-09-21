package com.petitioner0.filecraft;

import com.petitioner0.filecraft.content.FilecraftBlockEntities;
import com.petitioner0.filecraft.fsblock.FileNodeRenderer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Filecraft.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = Filecraft.MODID, value = Dist.CLIENT)
public final class FilecraftClient {
    public FilecraftClient(ModContainer container) {
        // 官方推荐：在客户端入口里注册配置界面扩展点
        container.registerExtensionPoint(
            IConfigScreenFactory.class, ConfigurationScreen::new
        );
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
            FilecraftBlockEntities.FILE_NODE.get(),
            FileNodeRenderer::new
        );
    }
}
