package com.sf.friendwinememe.client;

import com.sf.friendwinememe.FriendWineMemeMod;
import com.sf.friendwinememe.client.audio.MusicDataCache;
import com.sf.friendwinememe.procedures.MusicPreloader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FriendWineMemeMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
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