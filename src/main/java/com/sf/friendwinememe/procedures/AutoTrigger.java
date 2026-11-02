package com.sf.friendwinememe.procedures;

import com.sf.friendwinememe.Config;
import com.sf.friendwinememe.FriendWineMemeMod;
import com.sf.friendwinememe.client.RangeModeManager;
import com.sf.friendwinememe.entity.FloatingTransparencyEntity;
import com.sf.friendwinememe.init.*;
import com.sf.friendwinememe.item.TransparencyModeItem;
import com.sf.friendwinememe.item.TransparencyModeOffItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;

@Mod.EventBusSubscriber(modid = FriendWineMemeMod.MODID)
public class AutoTrigger {

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getLevel() instanceof ServerLevel)) return;

        Entity e = event.getEntity();
        if (e instanceof ItemEntity) return;

        CompoundTag data = e.getPersistentData();
        if (!data.contains("FriendWineMemeDuration")) return;
        if (data.contains("FriendWineMemeStart")) return;

        long durationMs = data.getLong("FriendWineMemeDuration");
        long now = System.currentTimeMillis();
        data.putLong("FriendWineMemeStart", now);

        int durationSeconds = durationMs > 0 ? (int) (durationMs / 1000) : -1;
        var musicRl = FriendWineMemeModMainMusicRegistry.get(Config.getMusicName());

        FriendWineMemeModNetwork.CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> e),
                new FriendWineMemeModNetwork.TransparencyEffectPacket(
                        e.getId(), durationSeconds, 0L));
        if (Config.isPlayMusic()) {
            FriendWineMemeModNetwork.CHANNEL.send(
                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> e),
                    new FriendWineMemeModNetwork.PlayMusicPacket(
                            e.blockPosition(), e.getId(), now, musicRl));
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (ServerLevel level : server.getAllLevels()) {
            for (Entity e : level.getAllEntities()) {
                if (!(e instanceof ItemEntity itemEntity)) continue;
                if (!itemEntity.onGround()) continue;
                if (itemEntity.isRemoved()) continue;

                ItemStack stack = itemEntity.getItem();

                if (stack.getItem() instanceof TransparencyModeItem) {
                    tryTriggerRangeMode(itemEntity, level);
                }

                else if (stack.getItem() instanceof TransparencyModeOffItem) {
                    tryStopRangeMode(itemEntity, level);
                }
            }
        }
    }

    private static void tryTriggerRangeMode(ItemEntity itemEntity, ServerLevel sl) {
        AABB box = itemEntity.getBoundingBox().inflate(0.5);

        List<ItemEntity> sugars = sl.getEntitiesOfClass(ItemEntity.class, box,
                e -> e.getItem().is(Items.SUGAR));
        if (sugars.size() < 2) return;

        sugars.get(0).discard();
        sugars.get(1).discard();

        ItemStack stack = itemEntity.getItem();
        int durationSeconds = -1;
        if (stack.getTag() != null && stack.getTag().contains(TransparencyModeItem.NBT_DURATION_SEC)) {
            durationSeconds = stack.getTag().getInt(TransparencyModeItem.NBT_DURATION_SEC);
        }
        long durationMs = durationSeconds > 0 ? (long) durationSeconds * 1000L : -1L;

        double px = itemEntity.getX();
        double py = itemEntity.getY();
        double pz = itemEntity.getZ();
        int layerY = (int) Math.floor(py);

        itemEntity.discard();

        FloatingTransparencyEntity fte = FriendWineMemeModEntity.FLOATING_TRANSPARENCY.get().create(sl);
        if (fte == null) return;

        fte.moveTo(px, py + 2.0, pz, 0, 0);
        fte.setRenderStack(stack.copyWithCount(1));
        fte.setStartMs(System.currentTimeMillis());
        fte.setDurationMs(durationMs);
        fte.rangeRadius = Config.getRangeRadius();
        fte.orbit = Config.isOrbit();
        fte.orbitSpeed = Config.getOrbitSpeed();
        fte.affectBlocks = Config.isAffectBlocks();
        fte.blockOrbit = Config.isBlockOrbit();
        fte.layerY = layerY;
        fte.musicRl = FriendWineMemeModMainMusicRegistry.get(Config.getMusicName());
        sl.addFreshEntity(fte);

        var lightning = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(sl);
        if (lightning != null) {
            lightning.moveTo(px, py, pz);
            lightning.setVisualOnly(true);
            sl.addFreshEntity(lightning);
        }

        RangeModeManager.start(new RangeModeManager.RangeState(
                fte.getId(), fte.position(), layerY,
                fte.getStartMs(), durationMs,
                fte.rangeRadius, fte.orbit, fte.orbitSpeed,
                fte.affectBlocks, fte.blockOrbit,
                Config.getMusicName()));
    }

    private static void tryStopRangeMode(ItemEntity itemEntity, ServerLevel sl) {
        double px = itemEntity.getX();
        double py = itemEntity.getY();
        double pz = itemEntity.getZ();
        int itemLayerY = (int) Math.floor(py);

        AABB area = new AABB(
                px - 2, py - 3, pz - 2,
                px + 2, py + 3, pz + 2);
        List<FloatingTransparencyEntity> floatings = sl.getEntitiesOfClass(
                FloatingTransparencyEntity.class, area);

        for (FloatingTransparencyEntity fte : floatings) {
            if (itemLayerY == fte.layerY) {
                RangeModeManager.stop(fte.getId());
                itemEntity.discard();
                return;
            }
        }
    }
}