package com.sf.friendwinememe;

import com.sf.friendwinememe.init.FriendWineMemeModNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ConfigSync {
    public static double sBounceSpeed, sMaxHeight, sMinHeight, sRotateSpeed, sOrbitSpeed, sViewShakeMax;
    public static int sMusicDistance, sRangeRadius;
    public static boolean sPlayMusic, sRotate, sShowLyrics, sMusicLoop;
    public static boolean sOrbit, sAffectBlocks, sBlockOrbit, sViewShake;
    public static String sMusicName;

    public static void readServerValues() {
        sBounceSpeed = Config.BOUNCE_SPEED.get();
        sMaxHeight = Config.MAX_HEIGHT.get();
        sMinHeight = Config.MIN_HEIGHT.get();
        sPlayMusic = Config.PLAY_MUSIC.get();
        sMusicDistance = Config.MUSIC_DISTANCE.get();
        sRotate = Config.ROTATE.get();
        sRotateSpeed = Config.ROTATE_SPEED.get();
        sShowLyrics = Config.SHOW_LYRICS.get();
        sMusicName = Config.MUSIC_NAME.get();
        sMusicLoop = Config.MUSIC_LOOP.get();
        sRangeRadius = Config.RANGE_RADIUS.get();
        sOrbit = Config.ORBIT.get();
        sOrbitSpeed = Config.ORBIT_SPEED.get();
        sAffectBlocks = Config.AFFECT_BLOCKS.get();
        sBlockOrbit = Config.BLOCK_ORBIT.get();
        sViewShake = Config.VIEW_SHAKE.get();
        sViewShakeMax = Config.VIEW_SHAKE_MAX.get();
    }

    public static void syncToAll(MinecraftServer server) {
        SyncConfigPacket pkt = build();
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(sp, pkt);
        }
    }

    private static SyncConfigPacket build() {
        return new SyncConfigPacket(sBounceSpeed, sMaxHeight, sMinHeight,
                sPlayMusic, sMusicDistance, sRotate, sRotateSpeed, sShowLyrics, sMusicName,
                sMusicLoop, sRangeRadius, sOrbit, sOrbitSpeed, sAffectBlocks, sBlockOrbit,
                sViewShake, sViewShakeMax);
    }

    public static void init() {
    }

    @EventBusSubscriber(modid = FriendWineMemeMod.MODID)
    public static class ServerHandler {
        @SubscribeEvent
        public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (!(event.getEntity() instanceof ServerPlayer sp)) return;
            MinecraftServer server = sp.getServer();
            if (server == null || !server.isDedicatedServer()) return;
            readServerValues();
            PacketDistributor.sendToPlayer(sp, build());
        }
    }

    @EventBusSubscriber(modid = FriendWineMemeMod.MODID, value = Dist.CLIENT)
    public static class ClientHandler {
        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            Config.clearRemote();
        }
    }

    public record SyncConfigPacket(double bounceSpeed, double maxHeight, double minHeight,
                                   boolean playMusic, int musicDistance,
                                   boolean rotate, double rotateSpeed, boolean showLyrics,
                                   String musicName, boolean musicLoop,
                                   int rangeRadius, boolean orbit, double orbitSpeed,
                                   boolean affectBlocks, boolean blockOrbit,
                                   boolean viewShake, double viewShakeMax)
            implements CustomPacketPayload {

        public static final Type<SyncConfigPacket> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(
                        FriendWineMemeMod.MODID, "sync_config"));

        public static final StreamCodec<FriendlyByteBuf, SyncConfigPacket> STREAM_CODEC =
                StreamCodec.of(
                        (b, m) -> {
                            b.writeDouble(m.bounceSpeed);
                            b.writeDouble(m.maxHeight);
                            b.writeDouble(m.minHeight);
                            b.writeBoolean(m.playMusic);
                            b.writeVarInt(m.musicDistance);
                            b.writeBoolean(m.rotate);
                            b.writeDouble(m.rotateSpeed);
                            b.writeBoolean(m.showLyrics);
                            b.writeUtf(m.musicName);
                            b.writeBoolean(m.musicLoop);
                            b.writeVarInt(m.rangeRadius);
                            b.writeBoolean(m.orbit);
                            b.writeDouble(m.orbitSpeed);
                            b.writeBoolean(m.affectBlocks);
                            b.writeBoolean(m.blockOrbit);
                            b.writeBoolean(m.viewShake);
                            b.writeDouble(m.viewShakeMax);
                        },
                        b -> new SyncConfigPacket(
                                b.readDouble(), b.readDouble(), b.readDouble(),
                                b.readBoolean(), b.readVarInt(), b.readBoolean(), b.readDouble(),
                                b.readBoolean(), b.readUtf(), b.readBoolean(),
                                b.readVarInt(), b.readBoolean(), b.readDouble(),
                                b.readBoolean(), b.readBoolean(),
                                b.readBoolean(), b.readDouble())
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(SyncConfigPacket m, IPayloadContext context) {
            context.enqueueWork(() ->
                    Config.applyRemote(m.bounceSpeed, m.maxHeight, m.minHeight,
                            m.playMusic, m.musicDistance, m.rotate, m.rotateSpeed,
                            m.showLyrics, m.musicName, m.musicLoop,
                            m.rangeRadius, m.orbit, m.orbitSpeed, m.affectBlocks, m.blockOrbit,
                            m.viewShake, m.viewShakeMax));
        }
    }
}