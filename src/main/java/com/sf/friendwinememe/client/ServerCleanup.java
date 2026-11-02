package com.sf.friendwinememe.client;

import com.sf.friendwinememe.FriendWineMemeMod;
import com.sf.friendwinememe.entity.BlockDisguiseEntity;
import com.sf.friendwinememe.entity.FloatingTransparencyEntity;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.ArrayList;

@EventBusSubscriber(modid = FriendWineMemeMod.MODID)
public class ServerCleanup {

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MinecraftServer server = event.getServer();

        RangeModeManager.stopAll();

        for (var lvl : server.getAllLevels()) {
            ArrayList<BlockDisguiseEntity> blocks = new ArrayList<>();
            for (var e : lvl.getEntities().getAll()) {
                if (e instanceof BlockDisguiseEntity bde) blocks.add(bde);
            }
            for (BlockDisguiseEntity bde : blocks) {
                bde.restoreSilent();
            }

            ArrayList<FloatingTransparencyEntity> floatings = new ArrayList<>();
            for (var e : lvl.getEntities().getAll()) {
                if (e instanceof FloatingTransparencyEntity fte) floatings.add(fte);
            }
            for (FloatingTransparencyEntity fte : floatings) {
                fte.discard();
            }
        }
    }
}