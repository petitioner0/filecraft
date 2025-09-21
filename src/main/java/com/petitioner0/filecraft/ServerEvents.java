package com.petitioner0.filecraft;


import com.petitioner0.filecraft.util.PlacementScheduler;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;

@EventBusSubscriber(modid = Filecraft.MODID)
public class ServerEvents {
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        if (e.getServer() != null) {
            PlacementScheduler.tick(e.getServer());
        }
    }
}