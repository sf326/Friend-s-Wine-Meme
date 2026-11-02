package com.sf.friendwinememe.client.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.lwjgl.openal.AL10;

import java.util.ArrayList;
import java.util.List;

public class AlMusicPlayer {
    private static final int AL_SEC_OFFSET = 0x1024;

    private int sourceId = 0;
    private final List<Integer> buffers = new ArrayList<>();

    private double cursorSec = 0.0;
    private long lastNano = 0;
    private boolean playing = false;

    private MusicData data;
    private float musicVolume = 1.0F;

    public double getCursorSec() { return cursorSec; }
    public double getTotalSec() { return data == null ? 0.0 : data.totalSec; }
    public boolean isPlaying() { return playing; }

    public boolean load(ResourceLocation ogg) {
        this.data = MusicDataCache.get(ogg);
        return this.data != null;
    }

    public void play() {
        if (data == null) return;
        playing = true;
        lastNano = 0;
        if (sourceId != 0) {
            AL10.alSourcePlay(sourceId);
        } else {
            ensureSource();
        }
    }

    public void pause() {
        if (!playing) return;
        updateCursor();
        playing = false;
        if (sourceId != 0) AL10.alSourcePause(sourceId);
    }

    public void stop() {
        updateCursor();
        playing = false;
        releaseSource();
        cursorSec = 0.0;
    }

    public void seek(double sec) {
        if (data == null) return;
        this.cursorSec = Math.max(0, Math.min(sec, data.totalSec));
        if (sourceId != 0) {
            AL10.alSourcef(sourceId, AL_SEC_OFFSET, (float) cursorSec);
        }
    }

    public void tick() {
        if (playing) updateCursor();
        if (sourceId != 0) {
            int state = AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE);
            if (state == AL10.AL_STOPPED) releaseSource();
        }
    }

    public void applyVolume() {
        var options = Minecraft.getInstance().options;
        float master = options.getSoundSourceVolume(SoundSource.MASTER);
        float music = options.getSoundSourceVolume(SoundSource.MUSIC);
        float v = master * music;
        this.musicVolume = v;
        if (sourceId != 0) AL10.alSourcef(sourceId, AL10.AL_GAIN, v);
    }

    private void updateCursor() {
        if (data == null) return;
        long now = System.nanoTime();
        if (lastNano == 0) { lastNano = now; return; }
        double delta = (now - lastNano) / 1_000_000_000.0;
        lastNano = now;
        cursorSec += delta;
        if (cursorSec >= data.totalSec) {
            cursorSec = data.totalSec;
            playing = false;
            releaseSource();
        }
    }

    private void ensureSource() {
        if (sourceId != 0 || data == null) return;
        int src = AL10.alGenSources();
        if (src == 0) return;
        sourceId = src;

        int buf = AL10.alGenBuffers();
        AL10.alBufferData(buf,
                data.channels == 2 ? AL10.AL_FORMAT_STEREO16 : AL10.AL_FORMAT_MONO16,
                data.pcm, data.sampleRate);
        buffers.add(buf);
        AL10.alSourcei(sourceId, AL10.AL_BUFFER, buf);

        AL10.alSourcef(sourceId, AL_SEC_OFFSET, (float) cursorSec);
        AL10.alSourcef(sourceId, AL10.AL_GAIN, musicVolume);
        AL10.alSourcei(sourceId, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
        AL10.alSource3f(sourceId, AL10.AL_POSITION, 0, 0, 0);
        AL10.alSourcei(sourceId, AL10.AL_LOOPING, AL10.AL_FALSE);

        AL10.alSourcePlay(sourceId);
        lastNano = 0;
    }

    private void releaseSource() {
        if (sourceId == 0) return;
        AL10.alSourceStop(sourceId);
        AL10.alDeleteSources(sourceId);
        sourceId = 0;
    }

    public void dispose() {
        releaseSource();
        for (int b : buffers) AL10.alDeleteBuffers(b);
        buffers.clear();
    }
}