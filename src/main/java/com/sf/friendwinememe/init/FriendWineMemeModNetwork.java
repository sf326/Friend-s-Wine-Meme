package com.sf.friendwinememe.init;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sf.friendwinememe.Config;
import com.sf.friendwinememe.ConfigSync;
import com.sf.friendwinememe.FriendWineMemeMod;
import com.sf.friendwinememe.client.ClientRangeMode;
import com.sf.friendwinememe.client.MusicManager;
import com.sf.friendwinememe.client.OrbitHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class FriendWineMemeModNetwork {
    private static final String PROTOCOL = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FriendWineMemeMod.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private record Anim(long start, long durationMs) {}
    private static final Map<Integer, Anim> ANIMS = new ConcurrentHashMap<>();
    private static final Set<Integer> PUSHED = ConcurrentHashMap.newKeySet();

    private static final long CYCLE_MS = 600L;
    private static final float T1_RATIO = 0.35F;

    public static void init() {
        CHANNEL.registerMessage(0, TransparencyEffectPacket.class,
                TransparencyEffectPacket::encode, TransparencyEffectPacket::decode,
                TransparencyEffectPacket::handle);
        CHANNEL.registerMessage(1, PlayMusicPacket.class,
                PlayMusicPacket::encode, PlayMusicPacket::decode, PlayMusicPacket::handle);
        CHANNEL.registerMessage(2, StopEffectPacket.class,
                StopEffectPacket::encode, StopEffectPacket::decode, StopEffectPacket::handle);
        CHANNEL.registerMessage(4, StopAllEffectPacket.class,
                StopAllEffectPacket::encode, StopAllEffectPacket::decode, StopAllEffectPacket::handle);
        CHANNEL.registerMessage(5, RangeModeStartPacket.class,
                RangeModeStartPacket::encode, RangeModeStartPacket::decode, RangeModeStartPacket::handle);
        CHANNEL.registerMessage(6, RangeModeStopPacket.class,
                RangeModeStopPacket::encode, RangeModeStopPacket::decode, RangeModeStopPacket::handle);
        ConfigSync.init();
    }

    public record TransparencyEffectPacket(int entityId, int durationSeconds, long elapsedMs) {
        public static void encode(TransparencyEffectPacket m, FriendlyByteBuf b) {
            b.writeVarInt(m.entityId);
            b.writeVarInt(m.durationSeconds);
            b.writeLong(m.elapsedMs);
        }
        public static TransparencyEffectPacket decode(FriendlyByteBuf b) {
            return new TransparencyEffectPacket(b.readVarInt(), b.readVarInt(), b.readLong());
        }
        public static void handle(TransparencyEffectPacket m, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> {
                long duration = m.durationSeconds > 0 ? (long) m.durationSeconds * 1000L : -1L;
                long start = System.currentTimeMillis() - m.elapsedMs;
                ANIMS.put(m.entityId, new Anim(start, duration));
            });
            c.get().setPacketHandled(true);
        }
    }

    public record StopEffectPacket(int entityId) {
        public static void encode(StopEffectPacket m, FriendlyByteBuf b) { b.writeVarInt(m.entityId); }
        public static StopEffectPacket decode(FriendlyByteBuf b) { return new StopEffectPacket(b.readVarInt()); }
        public static void handle(StopEffectPacket m, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> {
                ANIMS.remove(m.entityId);
                MusicManager.INSTANCE.stopFor(m.entityId);
                com.sf.friendwinememe.client.DisguiseShadowCache.clear(m.entityId);
                ClientRangeMode.INSTANCE.stop(m.entityId);
            });
            c.get().setPacketHandled(true);
        }
    }

    public record StopAllEffectPacket() {
        public static void encode(StopAllEffectPacket m, FriendlyByteBuf b) {}
        public static StopAllEffectPacket decode(FriendlyByteBuf b) { return new StopAllEffectPacket(); }
        public static void handle(StopAllEffectPacket m, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> {
                ANIMS.clear();
                MusicManager.INSTANCE.stopAll();
                com.sf.friendwinememe.client.DisguiseShadowCache.clearAll();
            });
            c.get().setPacketHandled(true);
        }
    }

    public record PlayMusicPacket(BlockPos pos, int entityId, long startTimeMs, ResourceLocation music) {
        public static void encode(PlayMusicPacket m, FriendlyByteBuf b) {
            b.writeBlockPos(m.pos);
            b.writeVarInt(m.entityId);
            b.writeLong(m.startTimeMs);
            b.writeResourceLocation(m.music);
        }
        public static PlayMusicPacket decode(FriendlyByteBuf b) {
            return new PlayMusicPacket(b.readBlockPos(), b.readVarInt(), b.readLong(),
                    b.readResourceLocation());
        }
        public static void handle(PlayMusicPacket m, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() ->
                    MusicManager.INSTANCE.onPlayPacket(m.entityId, m.startTimeMs, m.music));
            c.get().setPacketHandled(true);
        }
    }

    public record RangeModeStartPacket(int centerId, long startMs, long durationMs,
                                       int radius, boolean orbit, double orbitSpeed,
                                       boolean affectBlocks, boolean blockOrbit,
                                       int layerY, double cx, double cy, double cz) {
        public static void encode(RangeModeStartPacket m, FriendlyByteBuf b) {
            b.writeVarInt(m.centerId);
            b.writeLong(m.startMs);
            b.writeLong(m.durationMs);
            b.writeVarInt(m.radius);
            b.writeBoolean(m.orbit);
            b.writeDouble(m.orbitSpeed);
            b.writeBoolean(m.affectBlocks);
            b.writeBoolean(m.blockOrbit);
            b.writeVarInt(m.layerY);
            b.writeDouble(m.cx);
            b.writeDouble(m.cy);
            b.writeDouble(m.cz);
        }
        public static RangeModeStartPacket decode(FriendlyByteBuf b) {
            return new RangeModeStartPacket(b.readVarInt(), b.readLong(), b.readLong(),
                    b.readVarInt(), b.readBoolean(), b.readDouble(),
                    b.readBoolean(), b.readBoolean(), b.readVarInt(),
                    b.readDouble(), b.readDouble(), b.readDouble());
        }
        public static void handle(RangeModeStartPacket m, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> ClientRangeMode.INSTANCE.start(
                    m.centerId, m.startMs, m.durationMs, m.radius, m.orbit, m.orbitSpeed,
                    m.affectBlocks, m.blockOrbit, m.layerY,
                    new Vec3(m.cx, m.cy, m.cz)));
            c.get().setPacketHandled(true);
        }
    }

    public record RangeModeStopPacket(int centerId) {
        public static void encode(RangeModeStopPacket m, FriendlyByteBuf b) { b.writeVarInt(m.centerId); }
        public static RangeModeStopPacket decode(FriendlyByteBuf b) { return new RangeModeStopPacket(b.readVarInt()); }
        public static void handle(RangeModeStopPacket m, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> ClientRangeMode.INSTANCE.stop(m.centerId));
            c.get().setPacketHandled(true);
        }
    }

    public static float[] getAnimProgress(int entityId) {
        Anim anim = ANIMS.get(entityId);
        if (anim == null) return null;
        long elapsed = System.currentTimeMillis() - anim.start();
        if (anim.durationMs() > 0 && elapsed >= anim.durationMs()) return null;
        return new float[]{ elapsed, anim.durationMs() };
    }

    public static float scaleY(float t, float max, float min) {
        if (t < T1_RATIO) {
            float x = t / T1_RATIO;
            float ease = 1.0F - (float) Math.pow(1.0F - x, 3);
            return max - (max - min) * ease;
        } else {
            float x = (t - T1_RATIO) / (1.0F - T1_RATIO);
            return min + (max - min) * (x * x * x);
        }
    }

    public static long cycleMs() {
        long c = (long) (CYCLE_MS / (float) Config.getBounceSpeed());
        return c <= 0 ? 1 : c;
    }

    @Mod.EventBusSubscriber(modid = FriendWineMemeMod.MODID,
            bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientHandler {

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            if (ANIMS.isEmpty()) return;

            long now = System.currentTimeMillis();
            Iterator<Map.Entry<Integer, Anim>> it = ANIMS.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, Anim> entry = it.next();
                Anim anim = entry.getValue();
                if (anim.durationMs() > 0 && now - anim.start() >= anim.durationMs()) {
                    it.remove();
                    MusicManager.INSTANCE.stopFor(entry.getKey());
                    com.sf.friendwinememe.client.DisguiseShadowCache.clear(entry.getKey());
                }
            }
        }

        @SubscribeEvent
        public static void onRenderPre(RenderLivingEvent.Pre<?, ?> event) {
            int id = event.getEntity().getId();
            float[] anim = getAnimProgress(id);
            if (anim == null) return;

            long elapsed = (long) anim[0];
            float maxH = (float) Config.getMaxHeight();
            float minH = (float) Config.getMinHeight();
            long cycle = cycleMs();
            float t = (elapsed % cycle) / (float) cycle;

            float yScale = scaleY(t, maxH, minH);
            float xzScale = 1.0F + (maxH - yScale) * 0.5F;
            float yOffset = (maxH - yScale) * 0.15F;

            PoseStack pose = event.getPoseStack();
            pose.pushPose();

            OrbitHelper.applyOrbit(pose, event.getEntity());

            pose.translate(0.0D, yOffset, 0.0D);
            pose.scale(xzScale, yScale, xzScale);

            if (Config.isRotate()) {
                float deg = (float) (elapsed / 1000.0 * Config.getRotateSpeed());
                pose.mulPose(Axis.YP.rotationDegrees(deg));
            }
            PUSHED.add(id);
        }

        @SubscribeEvent
        public static void onRenderPost(RenderLivingEvent.Post<?, ?> event) {
            int id = event.getEntity().getId();
            if (PUSHED.remove(id)) {
                event.getPoseStack().popPose();
            }
        }
    }
}