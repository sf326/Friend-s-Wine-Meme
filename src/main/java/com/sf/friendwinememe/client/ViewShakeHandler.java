package com.sf.friendwinememe.client;

import com.mojang.brigadier.CommandDispatcher;
import com.sf.friendwinememe.Config;
import com.sf.friendwinememe.FriendWineMemeMod;
import com.sf.friendwinememe.init.FriendWineMemeModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FriendWineMemeMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ViewShakeHandler {

    private static boolean localShakeDisabled = false;

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("fwmplayershake")
                .executes(ctx -> {
                    localShakeDisabled = !localShakeDisabled;
                    ctx.getSource().sendSuccess(
                            () -> Component.translatable(localShakeDisabled
                                    ? "command.friendwinememe.playershake.on"
                                    : "command.friendwinememe.playershake.off"),
                            false);
                    return 1;
                }));
    }

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        if (localShakeDisabled) return;
        if (!Config.isViewShake()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float[] anim = FriendWineMemeModNetwork.getAnimProgress(mc.player.getId());
        if (anim == null) return;

        long elapsed = (long) anim[0];
        long cycle = FriendWineMemeModNetwork.cycleMs();
        if (cycle <= 0) return;

        float shake = getShake(elapsed, cycle);

        event.setPitch(event.getPitch() + shake);
    }

    private static float getShake(long elapsed, long cycle) {
        float t = (elapsed % cycle) / (float) cycle;

        float maxH = (float) Config.getMaxHeight();
        float minH = (float) Config.getMinHeight();
        float ratio = (maxH - minH) / Math.max(maxH, 0.01F);
        if (ratio < 0) ratio = 0;
        if (ratio > 1) ratio = 1;

        float maxAngle = (float) Config.getViewShakeMax();

        return (float) Math.sin(t * Math.PI * 2.0) * maxAngle * ratio;
    }
}