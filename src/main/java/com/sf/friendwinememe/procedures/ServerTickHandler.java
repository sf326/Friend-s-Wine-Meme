package com.sf.friendwinememe.procedures;

import com.sf.friendwinememe.FriendWineMemeMod;
import com.sf.friendwinememe.client.RangeModeManager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FriendWineMemeMod.MODID)
public class ServerTickHandler {
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            RangeModeManager.tick();
        }
    }
}