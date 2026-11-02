package com.sf.friendwinememe.client;

import com.sf.friendwinememe.Config;
import com.sf.friendwinememe.client.audio.AlMusicPlayer;
import com.sf.friendwinememe.client.audio.LrcParser;
import com.sf.friendwinememe.procedures.MusicPreloader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MusicManager {
    public static final MusicManager INSTANCE = new MusicManager();

    public static class State {
        AlMusicPlayer player;
        final long startMs;
        final ResourceLocation music;
        boolean playing;
        List<LrcParser.Line> lyrics = List.of();

        State(long startMs, ResourceLocation music) {
            this.startMs = startMs;
            this.music = music;
        }
    }

    private final Map<Integer, State> STATES = new ConcurrentHashMap<>();

    public void onPlayPacket(int entityId, long startTimeMs, ResourceLocation music) {
        stopFor(entityId);

        State s = new State(startTimeMs, music);
        AlMusicPlayer p = new AlMusicPlayer();
        ResourceLocation ogg = new ResourceLocation(
                music.getNamespace(), "sounds/" + music.getPath() + ".ogg");
        if (!p.load(ogg)) return;

        s.lyrics = MusicPreloader.getLyrics(music);

        s.player = p;
        STATES.put(entityId, s);
    }

    public void stopFor(int entityId) {
        State s = STATES.remove(entityId);
        if (s == null) return;
        if (s.player != null) s.player.dispose();
    }

    public void stopAll() {
        for (Integer id : new java.util.ArrayList<>(STATES.keySet())) {
            stopFor(id);
        }
    }

    public int getMainEntityId() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return -1;

        int maxDist = Config.getMusicDistance();
        double maxSqr = (double) maxDist * maxDist;
        int bestId = -1;
        double bestElapsed = -1;

        for (Map.Entry<Integer, State> e : STATES.entrySet()) {
            Entity entity = mc.level.getEntity(e.getKey());
            if (entity == null) continue;
            if (mc.player.distanceToSqr(entity) > maxSqr) continue;

            double elapsed = (System.currentTimeMillis() - e.getValue().startMs) / 1000.0;
            if (elapsed > bestElapsed) {
                bestElapsed = elapsed;
                bestId = e.getKey();
            }
        }
        return bestId;
    }

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        int maxDist = Config.getMusicDistance();
        double maxSqr = (double) maxDist * maxDist;
        int mainId = getMainEntityId();

        for (Map.Entry<Integer, State> e : STATES.entrySet()) {
            int id = e.getKey();
            State s = e.getValue();
            if (s.player == null) continue;

            s.player.applyVolume();

            if (!Config.isMusicLoop()) {
                double elapsed = (System.currentTimeMillis() - s.startMs) / 1000.0;
                if (elapsed >= s.player.getTotalSec()) {
                    stopFor(id);
                    continue;
                }
            }

            Entity entity = mc.level.getEntity(id);
            boolean inRange = entity != null
                    && mc.player.distanceToSqr(entity) <= maxSqr;

            if (id == mainId && inRange) {
                if (!s.playing) {
                    s.player.seek(s.player.getCursorSec());
                    s.player.play();
                    s.playing = true;
                } else {
                    s.player.tick();
                }
            } else {
                if (s.playing) {
                    s.player.pause();
                    s.playing = false;
                }
            }
        }
    }

    public float getCurrentSec(int entityId) {
        State s = STATES.get(entityId);
        if (s == null || s.player == null) return -1.0F;
        return (float) s.player.getCursorSec();
    }

    public List<LrcParser.Line> getLyrics(int entityId) {
        State s = STATES.get(entityId);
        return s == null ? List.of() : s.lyrics;
    }

    public boolean hasMusic(int entityId) {
        return STATES.containsKey(entityId);
    }
}