package com.sf.friendwinememe.client.audio;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MusicDataCache {
    private static final Map<ResourceLocation, MusicData> CACHE = new ConcurrentHashMap<>();

    public static MusicData get(ResourceLocation ogg) {
        return CACHE.computeIfAbsent(ogg, MusicData::load);
    }

    public static void clear() {
        CACHE.clear();
    }
}