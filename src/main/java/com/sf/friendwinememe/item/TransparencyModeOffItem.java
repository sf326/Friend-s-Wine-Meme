package com.sf.friendwinememe.item;

import com.sf.friendwinememe.client.RangeModeManager;
import com.sf.friendwinememe.entity.BlockDisguiseEntity;
import com.sf.friendwinememe.entity.FloatingTransparencyEntity;
import com.sf.friendwinememe.init.FriendWineMemeModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

public class TransparencyModeOffItem extends Item {

    public TransparencyModeOffItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        var selfData = player.getPersistentData();
        if (selfData.contains("FriendWineMemeStart")) {
            selfData.remove("FriendWineMemeDuration");
            selfData.remove("FriendWineMemeStart");
            FriendWineMemeModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
                    new FriendWineMemeModNetwork.StopEffectPacket(player.getId()));
            if (!player.isCreative()) stack.shrink(1);
            return InteractionResultHolder.success(stack);
        }

        if (RangeModeManager.isEntityAffected(player.getId())) {
            RangeModeManager.stopEntityEffect(player.getId());
            if (!player.isCreative()) stack.shrink(1);
            return InteractionResultHolder.success(stack);
        }

        double reach = 3.0D;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.x * reach, look.y * reach, look.z * reach);

        EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                level, player, eye, end,
                player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0D),
                e -> e != player && e.isAlive());

        if (entityHit != null) {
            Entity target = entityHit.getEntity();

            if (target instanceof FloatingTransparencyEntity fte) {
                RangeModeManager.stop(fte.getId());
                if (!player.isCreative()) stack.shrink(1);
                return InteractionResultHolder.success(stack);
            }

            if (stopTarget(target)) {
                RangeModeManager.stopEntityEffect(target.getId());
                if (!player.isCreative()) stack.shrink(1);
                return InteractionResultHolder.success(stack);
            }
            return InteractionResultHolder.pass(stack);
        }

        //方块
        HitResult hit = player.pick(reach, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)) return InteractionResultHolder.pass(stack);
        BlockPos pos = blockHit.getBlockPos();

        if (level instanceof ServerLevel sl) {
            var entities = sl.getEntitiesOfClass(BlockDisguiseEntity.class,
                    new net.minecraft.world.phys.AABB(pos).inflate(0D));
            if (!entities.isEmpty()) {
                BlockDisguiseEntity bde = entities.get(0);
                bde.restore();
                FriendWineMemeModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
                        new FriendWineMemeModNetwork.StopEffectPacket(bde.getId()));
                RangeModeManager.stopEntityEffect(bde.getId());
                if (!player.isCreative()) stack.shrink(1);
                return InteractionResultHolder.success(stack);
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    private static boolean stopTarget(Entity target) {
        if (target instanceof BlockDisguiseEntity bde) {
            bde.restore();
            FriendWineMemeModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
                    new FriendWineMemeModNetwork.StopEffectPacket(bde.getId()));
            return true;
        }

        var data = target.getPersistentData();
        if (!data.contains("FriendWineMemeStart")) return false;

        data.remove("FriendWineMemeDuration");
        data.remove("FriendWineMemeStart");

        FriendWineMemeModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
                new FriendWineMemeModNetwork.StopEffectPacket(target.getId()));
        return true;
    }
}