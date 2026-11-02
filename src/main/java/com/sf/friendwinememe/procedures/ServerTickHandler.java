package com.sf.friendwinememe.procedures;

import com.sf.friendwinememe.FriendWineMemeMod;
import com.sf.friendwinememe.client.RangeModeManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = FriendWineMemeMod.MODID)
public class ServerTickHandler {
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        RangeModeManager.tick();
    }
}