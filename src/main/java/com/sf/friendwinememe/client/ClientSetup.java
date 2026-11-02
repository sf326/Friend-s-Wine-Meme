package com.sf.friendwinememe.client;

import com.sf.friendwinememe.FriendWineMemeMod;
import com.sf.friendwinememe.init.FriendWineMemeModEntity;
import com.sf.friendwinememe.renderer.BlockDisguiseRenderer;
import com.sf.friendwinememe.renderer.FloatingTransparencyRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FriendWineMemeMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(FriendWineMemeModEntity.BLOCK_DISGUISE.get(),
                BlockDisguiseRenderer::new);
        event.registerEntityRenderer(FriendWineMemeModEntity.FLOATING_TRANSPARENCY.get(),
                FloatingTransparencyRenderer::new);
    }
}