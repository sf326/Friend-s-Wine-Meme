package com.sf.friendwinememe.procedures;

import com.sf.friendwinememe.FriendWineMemeMod;
import com.sf.friendwinememe.client.audio.LrcParser;
import com.sf.friendwinememe.client.audio.MusicDataCache;
import com.sf.friendwinememe.init.FriendWineMemeModMainMusicRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = FriendWineMemeMod.MODID, value = Dist.CLIENT)
public class MusicPreloader {

    private static final Map<ResourceLocation, List<LrcParser.Line>> LRC_CACHE = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        for (ResourceLocation music : FriendWineMemeModMainMusicRegistry.MUSIC.values()) {
            ResourceLocation ogg = ResourceLocation.fromNamespaceAndPath(
                    music.getNamespace(), "sounds/" + music.getPath() + ".ogg");
            MusicDataCache.get(ogg);

            ResourceLocation lrc = ResourceLocation.fromNamespaceAndPath(
                    music.getNamespace(), "sounds/" + music.getPath() + ".lrc");
            loadLrc(music, lrc);
        }
    }

    public static List<LrcParser.Line> getLyrics(ResourceLocation music) {
        List<LrcParser.Line> lines = LRC_CACHE.get(music);
        if (lines != null) return lines;

        ResourceLocation lrc = ResourceLocation.fromNamespaceAndPath(
                music.getNamespace(), "sounds/" + music.getPath() + ".lrc");
        return loadLrc(music, lrc);
    }

    private static List<LrcParser.Line> loadLrc(ResourceLocation music, ResourceLocation lrc) {
        try (InputStream in = Minecraft.getInstance().getResourceManager()
                .getResource(lrc).orElseThrow().open()) {
            List<LrcParser.Line> parsed = LrcParser.parse(in);
            LRC_CACHE.put(music, parsed);
            return parsed;
        } catch (Exception e) {
            List<LrcParser.Line> empty = List.of();
            LRC_CACHE.put(music, empty);
            return empty;
        }
    }

    public static void clear() {
        LRC_CACHE.clear();
    }
}