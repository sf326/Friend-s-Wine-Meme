package com.sf.friendwinememe.client;

import com.sf.friendwinememe.Config;
import com.sf.friendwinememe.FriendWineMemeMod;
import com.sf.friendwinememe.client.audio.LrcParser;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = FriendWineMemeMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientMusicTick {
    private static boolean lyricsDisabled = false;
    private static LrcParser.Line lastLine = null;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END) {
            MusicManager.INSTANCE.tick();
        }
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("fwmplayerlyrics")
                .executes(ctx -> {
                    lyricsDisabled = !lyricsDisabled;
                    ctx.getSource().sendSuccess(
                            () -> Component.translatable(lyricsDisabled
                                    ? "command.friendwinememe.playerlyrics.off"
                                    : "command.friendwinememe.playerlyrics.on"),
                            false);
                    return 1;
                }));
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (lyricsDisabled) return;
        if (!Config.isShowLyrics()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        int mainId = MusicManager.INSTANCE.getMainEntityId();
        if (mainId < 0) {
            lastLine = null;
            return;
        }

        float sec = MusicManager.INSTANCE.getCurrentSec(mainId);
        List<LrcParser.Line> lines = MusicManager.INSTANCE.getLyrics(mainId);
        LrcParser.Line line = LrcParser.lineAt(lines, sec);

        if (line == lastLine) return;
        lastLine = line;

        if (line == null || line.text.isEmpty()) return;

        if (line.isTitle) {
            mc.gui.setTimes(0, 20, 10);
            mc.gui.setTitle(Component.literal(line.text));
            mc.gui.setSubtitle(Component.empty());
        } else {
            mc.gui.setOverlayMessage(Component.literal(line.text), false);
        }
    }
}