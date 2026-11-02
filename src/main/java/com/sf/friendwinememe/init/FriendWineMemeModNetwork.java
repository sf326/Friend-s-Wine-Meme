package com.sf.friendwinememe.init;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sf.friendwinememe.Config;
import com.sf.friendwinememe.ConfigSync;
import com.sf.friendwinememe.FriendWineMemeMod;
import com.sf.friendwinememe.client.ClientRangeMode;
import com.sf.friendwinememe.client.DisguiseShadowCache;
import com.sf.friendwinememe.client.MusicManager;
import com.sf.friendwinememe.client.OrbitHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FriendWineMemeModNetwork {

    private record Anim(long start, long durationMs) {}
    private static final Map<Integer, Anim> ANIMS = new ConcurrentHashMap<>();
    private static final Set<Integer> PUSHED = ConcurrentHashMap.newKeySet();

    private static final long CYCLE_MS = 600L;
    private static final float T1_RATIO = 0.35F;

    public static void init() {
    }

    public static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("2");

        registrar.playToClient(
                TransparencyEffectPacket.TYPE,
                TransparencyEffectPacket.STREAM_CODEC,
                TransparencyEffectPacket::handle
        );
        registrar.playToClient(
                PlayMusicPacket.TYPE,
                PlayMusicPacket.STREAM_CODEC,
                PlayMusicPacket::handle
        );
        registrar.playToClient(
                StopEffectPacket.TYPE,
                StopEffectPacket.STREAM_CODEC,
                StopEffectPacket::handle
        );
        registrar.playToClient(
                StopAllEffectPacket.TYPE,
                StopAllEffectPacket.STREAM_CODEC,
                StopAllEffectPacket::handle
        );
        registrar.playToClient(
                RangeModeStartPacket.TYPE,
                RangeModeStartPacket.STREAM_CODEC,
                RangeModeStartPacket::handle
        );
        registrar.playToClient(
                RangeModeStopPacket.TYPE,
                RangeModeStopPacket.STREAM_CODEC,
                RangeModeStopPacket::handle
        );

        ConfigSync.init();
    }

    public record TransparencyEffectPacket(int entityId, int durationSeconds, long elapsedMs)
            implements CustomPacketPayload {
        public static final Type<TransparencyEffectPacket> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(FriendWineMemeMod.MODID, "transparency_effect"));

        public static final StreamCodec<FriendlyByteBuf, TransparencyEffectPacket> STREAM_CODEC =
                StreamCodec.of(
                        (buf, m) -> {
                            buf.writeVarInt(m.entityId);
                            buf.writeVarInt(m.durationSeconds);
                            buf.writeLong(m.elapsedMs);
                        },
                        buf -> new TransparencyEffectPacket(buf.readVarInt(), buf.readVarInt(), buf.readLong())
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(TransparencyEffectPacket m, IPayloadContext context) {
            context.enqueueWork(() -> {
                long duration = m.durationSeconds > 0 ? (long) m.durationSeconds * 1000L : -1L;
                long start = System.currentTimeMillis() - m.elapsedMs;
                ANIMS.put(m.entityId, new Anim(start, duration));
            });
        }
    }

    public record StopEffectPacket(int entityId) implements CustomPacketPayload {
        public static final Type<StopEffectPacket> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(FriendWineMemeMod.MODID, "stop_effect"));

        public static final StreamCodec<FriendlyByteBuf, StopEffectPacket> STREAM_CODEC =
                StreamCodec.of(
                        (buf, m) -> buf.writeVarInt(m.entityId),
                        buf -> new StopEffectPacket(buf.readVarInt())
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(StopEffectPacket m, IPayloadContext context) {
            context.enqueueWork(() -> {
                ANIMS.remove(m.entityId);
                MusicManager.INSTANCE.stopFor(m.entityId);
                DisguiseShadowCache.clear(m.entityId);
                ClientRangeMode.INSTANCE.stop(m.entityId);
            });
        }
    }

    public record StopAllEffectPacket() implements CustomPacketPayload {
        public static final Type<StopAllEffectPacket> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(FriendWineMemeMod.MODID, "stop_all_effect"));

        public static final StreamCodec<FriendlyByteBuf, StopAllEffectPacket> STREAM_CODEC =
                StreamCodec.unit(new StopAllEffectPacket());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(StopAllEffectPacket m, IPayloadContext context) {
            context.enqueueWork(() -> {
                ANIMS.clear();
                MusicManager.INSTANCE.stopAll();
                DisguiseShadowCache.clearAll();
            });
        }
    }

    public record PlayMusicPacket(BlockPos pos, int entityId, long startTimeMs, ResourceLocation music)
            implements CustomPacketPayload {
        public static final Type<PlayMusicPacket> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(FriendWineMemeMod.MODID, "play_music"));

        public static final StreamCodec<FriendlyByteBuf, PlayMusicPacket> STREAM_CODEC =
                StreamCodec.of(
                        (buf, m) -> {
                            buf.writeBlockPos(m.pos);
                            buf.writeVarInt(m.entityId);
                            buf.writeLong(m.startTimeMs);
                            buf.writeResourceLocation(m.music);
                        },
                        buf -> new PlayMusicPacket(buf.readBlockPos(), buf.readVarInt(),
                                buf.readLong(), buf.readResourceLocation())
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(PlayMusicPacket m, IPayloadContext context) {
            context.enqueueWork(() ->
                    MusicManager.INSTANCE.onPlayPacket(m.entityId, m.startTimeMs, m.music));
        }
    }

    public record RangeModeStartPacket(int centerId, long startMs, long durationMs,
                                       int radius, boolean orbit, double orbitSpeed,
                                       boolean affectBlocks, boolean blockOrbit,
                                       int layerY, double cx, double cy, double cz)
            implements CustomPacketPayload {
        public static final Type<RangeModeStartPacket> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(FriendWineMemeMod.MODID, "range_mode_start"));

        public static final StreamCodec<FriendlyByteBuf, RangeModeStartPacket> STREAM_CODEC =
                StreamCodec.of(
                        (buf, m) -> {
                            buf.writeVarInt(m.centerId);
                            buf.writeLong(m.startMs);
                            buf.writeLong(m.durationMs);
                            buf.writeVarInt(m.radius);
                            buf.writeBoolean(m.orbit);
                            buf.writeDouble(m.orbitSpeed);
                            buf.writeBoolean(m.affectBlocks);
                            buf.writeBoolean(m.blockOrbit);
                            buf.writeVarInt(m.layerY);
                            buf.writeDouble(m.cx);
                            buf.writeDouble(m.cy);
                            buf.writeDouble(m.cz);
                        },
                        buf -> new RangeModeStartPacket(
                                buf.readVarInt(), buf.readLong(), buf.readLong(),
                                buf.readVarInt(), buf.readBoolean(), buf.readDouble(),
                                buf.readBoolean(), buf.readBoolean(), buf.readVarInt(),
                                buf.readDouble(), buf.readDouble(), buf.readDouble())
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(RangeModeStartPacket m, IPayloadContext context) {
            context.enqueueWork(() -> ClientRangeMode.INSTANCE.start(
                    m.centerId, m.startMs, m.durationMs, m.radius, m.orbit, m.orbitSpeed,
                    m.affectBlocks, m.blockOrbit, m.layerY,
                    new Vec3(m.cx, m.cy, m.cz)));
        }
    }

    public record RangeModeStopPacket(int centerId) implements CustomPacketPayload {
        public static final Type<RangeModeStopPacket> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(FriendWineMemeMod.MODID, "range_mode_stop"));

        public static final StreamCodec<FriendlyByteBuf, RangeModeStopPacket> STREAM_CODEC =
                StreamCodec.of(
                        (buf, m) -> buf.writeVarInt(m.centerId),
                        buf -> new RangeModeStopPacket(buf.readVarInt())
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(RangeModeStopPacket m, IPayloadContext context) {
            context.enqueueWork(() -> ClientRangeMode.INSTANCE.stop(m.centerId));
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

    @EventBusSubscriber(modid = FriendWineMemeMod.MODID, value = Dist.CLIENT)
    public static class ClientHandler {

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            if (ANIMS.isEmpty()) return;

            long now = System.currentTimeMillis();
            Iterator<Map.Entry<Integer, Anim>> it = ANIMS.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, Anim> entry = it.next();
                Anim anim = entry.getValue();
                if (anim.durationMs() > 0 && now - anim.start() >= anim.durationMs()) {
                    it.remove();
                    MusicManager.INSTANCE.stopFor(entry.getKey());
                    DisguiseShadowCache.clear(entry.getKey());
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