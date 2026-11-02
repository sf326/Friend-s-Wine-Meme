package com.sf.friendwinememe.client;

import com.sf.friendwinememe.FriendWineMemeMod;
import com.sf.friendwinememe.client.audio.MusicDataCache;
import com.sf.friendwinememe.procedures.MusicPreloader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

@EventBusSubscriber(modid = FriendWineMemeMod.MODID, value = Dist.CLIENT)
public class ClientCleanup {

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        MusicManager.INSTANCE.stopAll();
        DisguiseShadowCache.clearAll();
        MusicDataCache.clear();
        MusicPreloader.clear();
    }

    @SubscribeEvent
    public static void onShutdown(GameShuttingDownEvent event) {
        MusicManager.INSTANCE.stopAll();
        DisguiseShadowCache.clearAll();
        MusicDataCache.clear();
        MusicPreloader.clear();
    }
}