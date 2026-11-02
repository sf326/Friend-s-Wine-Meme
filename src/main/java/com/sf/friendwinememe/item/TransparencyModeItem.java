package com.sf.friendwinememe.item;

import com.sf.friendwinememe.Config;
import com.sf.friendwinememe.entity.BlockDisguiseEntity;
import com.sf.friendwinememe.init.*;
import com.sf.friendwinememe.procedures.BlockTransformHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

public class TransparencyModeItem extends Item {

    public static final String NBT_DURATION_SEC = "FriendWineMemeDurationSeconds";

    public TransparencyModeItem(Properties properties) {
        super(properties);
    }

    private static int getDurationSeconds(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_DURATION_SEC)) return -1;
        return tag.getInt(NBT_DURATION_SEC);
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(NBT_DURATION_SEC)) {
            int sec = tag.getInt(NBT_DURATION_SEC);
            return Component.translatable(
                    "item.friendwinememe.transparency_mode.seconds", sec);
        }
        return Component.translatable("item.friendwinememe.transparency_mode.infinite");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        int durationSeconds = getDurationSeconds(stack);
        long durationMs = durationSeconds > 0 ? (long) durationSeconds * 1000L : -1L;
        long now = System.currentTimeMillis();
        ResourceLocation musicRl = FriendWineMemeModMainMusicRegistry.get(Config.getMusicName());

        double reach = 3.0D;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.x * reach, look.y * reach, look.z * reach);

        //实体检测
        var entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                level, player, eye, end,
                player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0D),
                e -> e != player && e.isAlive() && !(e instanceof BlockDisguiseEntity));

        if (entityHit != null) {
            Entity target = entityHit.getEntity();
            target.getPersistentData().putLong("FriendWineMemeDuration", durationMs);
            target.getPersistentData().putLong("FriendWineMemeStart", now);

            FriendWineMemeModNetwork.CHANNEL.send(
                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target),
                    new FriendWineMemeModNetwork.TransparencyEffectPacket(target.getId(), durationSeconds, 0L));
            if (Config.isPlayMusic()) {
                FriendWineMemeModNetwork.CHANNEL.send(
                        PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target),
                        new FriendWineMemeModNetwork.PlayMusicPacket(target.blockPosition(), target.getId(), now, musicRl));
            }
            if (!player.isCreative()) stack.shrink(1);
            return InteractionResultHolder.success(stack);
        }

        //方块检测
        HitResult hit = player.pick(reach, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)) {
            return InteractionResultHolder.pass(stack);
        }
        BlockPos hitPos = blockHit.getBlockPos();
        BlockState hitState = level.getBlockState(hitPos);

        if (hitState.isAir()) {
            return InteractionResultHolder.pass(stack);
        }

        if (hitState.getDestroySpeed(level, hitPos) < 0) {
            return InteractionResultHolder.pass(stack);
        }

        if (level instanceof ServerLevel sl) {
            BlockTransformHelper.TransformResult result =
                    BlockTransformHelper.tryTransform(sl, hitPos, durationMs);

            if (!result.isSuccess()) {
                return InteractionResultHolder.fail(stack);
            }

            BlockDisguiseEntity disguise = result.entity;
            disguise.setEffectStartMs(now);
            if (Config.isPlayMusic()) {
                disguise.setMusicStartMs(now);
            }

            FriendWineMemeModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
                    new FriendWineMemeModNetwork.TransparencyEffectPacket(disguise.getId(), durationSeconds, 0L));
            if (Config.isPlayMusic()) {
                FriendWineMemeModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
                        new FriendWineMemeModNetwork.PlayMusicPacket(hitPos, disguise.getId(), now, musicRl));
            }
            if (!player.isCreative()) stack.shrink(1);
            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.pass(stack);
    }
}