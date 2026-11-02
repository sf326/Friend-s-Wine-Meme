package com.sf.friendwinememe.init;

import com.sf.friendwinememe.FriendWineMemeMod;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

public class FriendWineMemeModMainMusicRegistry {

    public static final Map<String, ResourceLocation> MUSIC = new LinkedHashMap<>();

    static {
        MUSIC.put("transparency_mode_music",
                ResourceLocation.fromNamespaceAndPath(FriendWineMemeMod.MODID, "transparency_mode_music"));
        MUSIC.put("ba_qian_li_lu_ming_yue_zhao",
                ResourceLocation.fromNamespaceAndPath(FriendWineMemeMod.MODID, "ba_qian_li_lu_ming_yue_zhao"));
    }

    public static ResourceLocation get(String name) {
        ResourceLocation rl = MUSIC.get(name);
        if (rl != null) return rl;
        return getDefault();
    }

    public static ResourceLocation getDefault() {
        return MUSIC.values().iterator().next();
    }

    public static Iterable<String> names() {
        return MUSIC.keySet();
    }
}