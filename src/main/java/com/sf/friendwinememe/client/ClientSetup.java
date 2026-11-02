package com.sf.friendwinememe.client;

import com.sf.friendwinememe.FriendWineMemeMod;
import com.sf.friendwinememe.init.FriendWineMemeModEntity;
import com.sf.friendwinememe.renderer.BlockDisguiseRenderer;
import com.sf.friendwinememe.renderer.FloatingTransparencyRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = FriendWineMemeMod.MODID, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(FriendWineMemeModEntity.BLOCK_DISGUISE.get(),
                BlockDisguiseRenderer::new);
        event.registerEntityRenderer(FriendWineMemeModEntity.FLOATING_TRANSPARENCY.get(),
                FloatingTransparencyRenderer::new);
    }
}