package com.sf.friendwinememe.procedures;

import com.sf.friendwinememe.FriendWineMemeMod;
import com.sf.friendwinememe.entity.BlockDisguiseEntity;
import com.sf.friendwinememe.init.FriendWineMemeModEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public class BlockTransformHelper {

    public static class TransformResult {
        public final BlockDisguiseEntity entity;
        public final String failReason;
        public final boolean silent;

        private TransformResult(BlockDisguiseEntity e, String r, boolean silent) {
            this.entity = e;
            this.failReason = r;
            this.silent = silent;
        }
        public static TransformResult ok(BlockDisguiseEntity e) {
            return new TransformResult(e, null, false);
        }
        public static TransformResult fail(String r) {
            return new TransformResult(null, r, false);
        }
        public static TransformResult silent() {
            return new TransformResult(null, null, true);
        }
        public boolean isSuccess() { return entity != null; }
        public boolean isSilent() { return silent; }
    }

    public static TransformResult tryTransform(ServerLevel level, BlockPos pos, long durationMs) {
        BlockState state = level.getBlockState(pos);

        if (state.isAir()) return TransformResult.silent();

        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (blockId == null) {
            return fail(pos, "方块没有注册 ID: " + state.getBlock());
        }

        if (!hasBlockModel(state)) {
            return fail(pos, "方块没有可渲染模型: " + blockId);
        }

        BlockEntity be = level.getBlockEntity(pos);
        CompoundTag nbt = new CompoundTag();
        if (be != null) {
            try {
                nbt = be.saveWithoutMetadata();
                ResourceLocation typeId = ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(be.getType());
                if (typeId == null) {
                    return fail(pos, "BlockEntityType 没有注册 ID: " + be.getType());
                }
                nbt.putString("id", typeId.toString());
            } catch (Exception ex) {
                FriendWineMemeMod.LOGGER.error("[FriendWineMeme] BlockEntity NBT 保存失败: {}",
                        be.getType(), ex);
                return fail(pos, "BlockEntity NBT 保存失败: " + ex.getMessage());
            }
        }

        BlockDisguiseEntity entity;
        try {
            entity = FriendWineMemeModEntity.BLOCK_DISGUISE.get().create(level);
        } catch (Exception ex) {
            FriendWineMemeMod.LOGGER.error("[FriendWineMeme] 伪装实体创建异常", ex);
            return fail(pos, "伪装实体创建异常: " + ex.getMessage());
        }
        if (entity == null) {
            return fail(pos, "伪装实体创建返回 null");
        }

        try {
            entity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
            entity.setBlockState(state);
            entity.setBlockNbt(nbt);
            entity.setDurationMs(durationMs);
            entity.setOriginalBlockId(blockId);
            entity.setOriginalBlockEntityNbt(nbt);

            level.removeBlock(pos, false);
            if (!level.addFreshEntity(entity)) {
                level.setBlock(pos, state, 3);
                if (be != null) {
                    var restored = level.getBlockEntity(pos);
                    if (restored != null) restored.load(nbt);
                }
                return fail(pos, "伪装实体加入世界失败，已还原方块 " + blockId);
            }

            level.setBlock(pos, Blocks.BARRIER.defaultBlockState(), 3);
            entity.setBarrierPos(pos);
        } catch (Exception ex) {
            FriendWineMemeMod.LOGGER.error("[FriendWineMeme] 转化异常，尝试还原", ex);
            try {
                level.setBlock(pos, state, 3);
                if (be != null) {
                    var restored = level.getBlockEntity(pos);
                    if (restored != null) restored.load(nbt);
                }
            } catch (Exception ignored) {}
            return fail(pos, "转化过程异常: " + ex.getMessage());
        }

        return TransformResult.ok(entity);
    }

    private static TransformResult fail(BlockPos pos, String reason) {
        FriendWineMemeMod.LOGGER.warn("[FriendWineMeme] 转化失败 pos={}: {}", pos, reason);
        return TransformResult.fail(reason);
    }

    private static boolean hasBlockModel(BlockState state) {
        if (state.isAir()) return false;
        var block = state.getBlock();
        if (block == Blocks.BARRIER) return false;
        if (block == Blocks.STRUCTURE_VOID) return false;
        if (block == Blocks.LIGHT) return false;
        return true;
    }
}