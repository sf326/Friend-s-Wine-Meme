package com.sf.friendwinememe.client.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.sounds.JOrbisAudioStream;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MusicData {
    public final ByteBuffer pcm;
    public final int sampleRate;
    public final int channels;
    public final int bitsPerSample;
    public final double totalSec;

    public MusicData(ByteBuffer pcm, int sampleRate, int channels, int bitsPerSample, double totalSec) {
        this.pcm = pcm;
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.bitsPerSample = bitsPerSample;
        this.totalSec = totalSec;
    }

    public static MusicData load(ResourceLocation ogg) {
        try {
            var res = Minecraft.getInstance().getResourceManager().getResource(ogg);
            if (res.isEmpty()) return null;

            byte[] fileBytes;
            try (InputStream in = res.get().open()) {
                fileBytes = in.readAllBytes();
            }

            int parsedRate = parseOggSampleRate(fileBytes);
            int sampleRate = parsedRate > 0 ? parsedRate : 48000;

            double parsedDur = parseOggDuration(fileBytes, sampleRate);
            double totalSec = parsedDur > 0 ? parsedDur : 0.0;

            ByteBuffer pcm;
            try (InputStream in = new ByteArrayInputStream(fileBytes)) {
                JOrbisAudioStream oggStream = new JOrbisAudioStream(in);
                int channels = 2;
                int bitsPerSample = 16;

                List<ByteBuffer> chunks = new ArrayList<>();
                int totalBytes = 0;
                ByteBuffer read;
                while ((read = oggStream.read(4096)) != null && read.remaining() > 0) {
                    ByteBuffer copy = ByteBuffer.allocateDirect(read.remaining());
                    copy.put(read);
                    copy.flip();
                    chunks.add(copy);
                    totalBytes += copy.capacity();
                }
                oggStream.close();

                ByteBuffer all = ByteBuffer.allocateDirect(totalBytes);
                for (ByteBuffer c : chunks) all.put(c);
                all.flip();
                pcm = all;

                if (totalSec <= 0) {
                    int bytesPerSec = sampleRate * channels * (bitsPerSample / 8);
                    totalSec = totalBytes / (double) bytesPerSec;
                }
            }

            return new MusicData(pcm, sampleRate, 2, 16, totalSec);
        } catch (Exception e) {
            return null;
        }
    }

    private static int parseOggSampleRate(byte[] data) {
        try {
            int identStart = 28;
            if (data[identStart] != 0x01
                    || data[identStart + 1] != 'v' || data[identStart + 2] != 'o'
                    || data[identStart + 3] != 'r' || data[identStart + 4] != 'b'
                    || data[identStart + 5] != 'i' || data[identStart + 6] != 's') {
                return -1;
            }
            int srOffset = identStart + 1 + 6 + 4 + 1;
            return (data[srOffset] & 0xFF)
                    | ((data[srOffset + 1] & 0xFF) << 8)
                    | ((data[srOffset + 2] & 0xFF) << 16)
                    | ((data[srOffset + 3] & 0xFF) << 24);
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static double parseOggDuration(byte[] data, int sampleRate) {
        try {
            long totalSamples = 0;
            for (int i = data.length - 27; i >= 0; i--) {
                if (data[i] == 'O' && data[i + 1] == 'g' && data[i + 2] == 'g'
                        && data[i + 3] == 'S') {
                    long granule = 0;
                    for (int j = 0; j < 8; j++) {
                        granule |= ((long) (data[i + 6 + j] & 0xFF)) << (8 * j);
                    }
                    totalSamples = granule;
                    break;
                }
            }
            if (sampleRate <= 0) return -1;
            return totalSamples / (double) sampleRate;
        } catch (Exception ignored) {
            return -1;
        }
    }
}