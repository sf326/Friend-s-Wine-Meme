package com.sf.friendwinememe.client;

import com.sf.friendwinememe.Config;
import com.sf.friendwinememe.entity.BlockDisguiseEntity;
import com.sf.friendwinememe.entity.FloatingTransparencyEntity;
import com.sf.friendwinememe.init.FriendWineMemeModMainMusicRegistry;
import com.sf.friendwinememe.init.FriendWineMemeModNetwork;
import com.sf.friendwinememe.procedures.BlockTransformHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RangeModeManager {

    public static class RangeState {
        public final int centerId;
        public final Vec3 centerPos;
        public final int layerY;
        public final long startMs;
        public final long durationMs;
        public final int radius;
        public final boolean orbit;
        public final double orbitSpeed;
        public final boolean affectBlocks;
        public final boolean blockOrbit;
        public final String musicName;

        public final Set<Integer> affectedEntities = ConcurrentHashMap.newKeySet();
        public final Set<Integer> affectedBlocks = ConcurrentHashMap.newKeySet();
        public final Map<Integer, Vec3> entityOriginalPos = new ConcurrentHashMap<>();
        public final Map<Integer, Float> entityOriginalYaw = new ConcurrentHashMap<>();
        public final Map<Integer, Long> entityJoinMs = new ConcurrentHashMap<>();

        public RangeState(int centerId, Vec3 centerPos, int layerY,
                          long startMs, long durationMs, int radius,
                          boolean orbit, double orbitSpeed,
                          boolean affectBlocks, boolean blockOrbit, String musicName) {
            this.centerId = centerId;
            this.centerPos = centerPos;
            this.layerY = layerY;
            this.startMs = startMs;
            this.durationMs = durationMs;
            this.radius = radius;
            this.orbit = orbit;
            this.orbitSpeed = orbitSpeed;
            this.affectBlocks = affectBlocks;
            this.blockOrbit = blockOrbit;
            this.musicName = musicName;
        }
    }

    private static final Map<Integer, RangeState> ACTIVE = new ConcurrentHashMap<>();

    public static void start(RangeState state) {
        ACTIVE.put(state.centerId, state);
        PacketDistributor.sendToAllPlayers(
                new FriendWineMemeModNetwork.RangeModeStartPacket(
                        state.centerId, state.startMs, state.durationMs,
                        state.radius, state.orbit, state.orbitSpeed,
                        state.affectBlocks, state.blockOrbit,
                        state.layerY,
                        state.centerPos.x, state.centerPos.y, state.centerPos.z));
    }

    public static void stop(int centerId) {
        RangeState state = ACTIVE.remove(centerId);
        if (state == null) return;

        // 还原方块
        for (int id : state.affectedBlocks) {
            Entity e = findEntity(id);
            if (e instanceof BlockDisguiseEntity bde) bde.restoreSilent();
            PacketDistributor.sendToAllPlayers(
                    new FriendWineMemeModNetwork.StopEffectPacket(id));
        }

        // 还原实体
        for (int id : state.affectedEntities) {
            Entity e = findEntity(id);
            if (e != null && !(e instanceof Player)) {
                Vec3 pos = state.entityOriginalPos.get(id);
                if (pos != null) {
                    e.teleportTo(pos.x, pos.y, pos.z);
                    Float yaw = state.entityOriginalYaw.get(id);
                    if (yaw != null) {
                        e.setYRot(yaw);
                        e.setYHeadRot(yaw);
                    }
                    e.setInvulnerable(false);
                }
            }
            PacketDistributor.sendToAllPlayers(
                    new FriendWineMemeModNetwork.StopEffectPacket(id));
        }

        Entity center = findEntity(centerId);
        if (center instanceof FloatingTransparencyEntity fte) fte.discard();

        PacketDistributor.sendToAllPlayers(
                new FriendWineMemeModNetwork.RangeModeStopPacket(centerId));
        PacketDistributor.sendToAllPlayers(
                new FriendWineMemeModNetwork.StopEffectPacket(centerId));
    }

    public static void stopAll() {
        for (Integer id : new ArrayList<>(ACTIVE.keySet())) stop(id);
    }

    public static boolean isEntityAffected(int entityId) {
        for (RangeState state : ACTIVE.values()) {
            if (state.affectedEntities.contains(entityId)) return true;
        }
        return false;
    }

    private static Entity findEntity(int id) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e != null) return e;
        }
        return null;
    }

    public static void tick() {
        if (ACTIVE.isEmpty()) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        List<Integer> toStop = new ArrayList<>();
        for (RangeState state : ACTIVE.values()) {
            Entity center = findEntity(state.centerId);
            if (!(center instanceof FloatingTransparencyEntity fte) || center.isRemoved()) {
                toStop.add(state.centerId);
                continue;
            }
            if (fte.isExpired()) {
                toStop.add(state.centerId);
                continue;
            }
            for (ServerLevel level : server.getAllLevels()) {
                tickState(level, state, fte);
            }
        }
        for (int id : toStop) stop(id);
    }

    private static void tickState(ServerLevel level, RangeState state, FloatingTransparencyEntity center) {
        Vec3 c = state.centerPos;
        double radiusSqr = (double) state.radius * state.radius;

        AABB box = new AABB(
                c.x - state.radius, state.layerY - 1, c.z - state.radius,
                c.x + state.radius, state.layerY + 2, c.z + state.radius);

        for (Entity e : level.getEntities((Entity) null, box, ent -> isValidTarget(ent, state))) {
            if (!state.affectedEntities.contains(e.getId())) {
                state.affectedEntities.add(e.getId());
                state.entityOriginalPos.put(e.getId(), e.position());
                state.entityOriginalYaw.put(e.getId(), e.getYRot());
                state.entityJoinMs.put(e.getId(), System.currentTimeMillis());

                PacketDistributor.sendToAllPlayers(
                        new FriendWineMemeModNetwork.TransparencyEffectPacket(
                                e.getId(),
                                state.durationMs > 0 ? (int) (state.durationMs / 1000) : -1,
                                0L));
                if (Config.isPlayMusic()) {
                    var musicRl = FriendWineMemeModMainMusicRegistry.get(Config.getMusicName());
                    PacketDistributor.sendToAllPlayers(
                            new FriendWineMemeModNetwork.PlayMusicPacket(
                                    e.blockPosition(), e.getId(),
                                    System.currentTimeMillis(), musicRl));
                }
            }

            if (state.orbit && state.orbitSpeed > 0 && !(e instanceof Player)) {
                Vec3 orig = state.entityOriginalPos.get(e.getId());
                Long joinMs = state.entityJoinMs.get(e.getId());
                if (orig != null && joinMs != null) {
                    double dx = orig.x - c.x;
                    double dz = orig.z - c.z;
                    double radius = Math.sqrt(dx * dx + dz * dz);
                    double initialAngle = Math.atan2(dz, dx);

                    double myElapsed = (System.currentTimeMillis() - joinMs) / 1000.0;
                    double speedOffset = 0.8 + (e.getId() % 50) / 100.0;
                    double myAngle = state.orbitSpeed * myElapsed * speedOffset;

                    double a = Math.toRadians(myAngle);
                    double nx = c.x + radius * Math.cos(initialAngle + a);
                    double nz = c.z + radius * Math.sin(initialAngle + a);
                    Vec3 newPos = new Vec3(nx, orig.y, nz);
                    e.teleportTo(newPos.x, newPos.y, newPos.z);

                    float tangent = (float) (Math.toDegrees(initialAngle + a) + 90.0);
                    e.setYRot(tangent);
                    e.setYHeadRot(tangent);
                    if (e instanceof LivingEntity le) le.yBodyRot = tangent;
                }
            }
        }

        if (state.affectBlocks) {
            for (int dx = -state.radius; dx <= state.radius; dx++) {
                for (int dz = -state.radius; dz <= state.radius; dz++) {
                    if (dx * dx + dz * dz > radiusSqr) continue;
                    BlockPos pos = new BlockPos(
                            (int) Math.floor(c.x) + dx,
                            state.layerY,
                            (int) Math.floor(c.z) + dz);
                    BlockState bs = level.getBlockState(pos);
                    if (bs.isAir()) continue;
                    if (bs.getDestroySpeed(level, pos) < 0) {
                        continue;
                    }

                    BlockPos finalPos = pos;
                    boolean exists = !level.getEntitiesOfClass(
                            BlockDisguiseEntity.class,
                            new AABB(pos),
                            e -> BlockPos.containing(e.getX(), e.getY(), e.getZ()).equals(finalPos)
                    ).isEmpty();
                    if (exists) {
                        continue;
                    }

                    BlockTransformHelper.TransformResult result =
                            BlockTransformHelper.tryTransform(level, pos, state.durationMs);
                    if (result.isSuccess()) {
                        BlockDisguiseEntity bde = result.entity;
                        bde.setEffectStartMs(System.currentTimeMillis());
                        state.affectedBlocks.add(bde.getId());

                        PacketDistributor.sendToAllPlayers(
                                new FriendWineMemeModNetwork.TransparencyEffectPacket(
                                        bde.getId(),
                                        state.durationMs > 0 ? (int) (state.durationMs / 1000) : -1,
                                        0L));
                        if (Config.isPlayMusic()) {
                            var musicRl = FriendWineMemeModMainMusicRegistry.get(Config.getMusicName());
                            PacketDistributor.sendToAllPlayers(
                                    new FriendWineMemeModNetwork.PlayMusicPacket(
                                            pos, bde.getId(),
                                            System.currentTimeMillis(), musicRl));
                        }
                    }
                }
            }
        }
    }

    private static boolean isValidTarget(Entity e, RangeState state) {
        if (e instanceof FloatingTransparencyEntity) return false;
        if (e instanceof BlockDisguiseEntity) return false;
        if (e instanceof net.minecraft.world.entity.item.ItemEntity) return false;
        if (e instanceof net.minecraft.world.entity.ExperienceOrb) return false;
        if (e.isRemoved()) return false;
        e.setInvulnerable(true);
        return true;
    }

    public static void stopEntityEffect(int entityId) {
        for (RangeState state : ACTIVE.values()) {
            if (state.affectedEntities.remove(entityId)) {
                Vec3 orig = state.entityOriginalPos.remove(entityId);
                state.entityJoinMs.remove(entityId);
                Entity e = findEntity(entityId);
                if (e != null && orig != null && !(e instanceof Player)) {
                    e.teleportTo(orig.x, orig.y, orig.z);
                    e.setInvulnerable(false);
                }
                PacketDistributor.sendToAllPlayers(
                        new FriendWineMemeModNetwork.StopEffectPacket(entityId));
            }
            if (state.affectedBlocks.remove(entityId)) {
                Entity e = findEntity(entityId);
                if (e instanceof BlockDisguiseEntity bde) bde.restoreSilent();
                PacketDistributor.sendToAllPlayers(
                        new FriendWineMemeModNetwork.StopEffectPacket(entityId));
            }
        }
    }
}