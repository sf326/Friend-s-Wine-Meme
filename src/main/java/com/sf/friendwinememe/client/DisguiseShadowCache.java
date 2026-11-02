package com.sf.friendwinememe.client;

import com.sf.friendwinememe.FriendWineMemeMod;
import com.sf.friendwinememe.entity.BlockDisguiseEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class DisguiseShadowCache {

    private static final Map<Integer, BlockEntity> SHADOW = new ConcurrentHashMap<>();
    private static final Map<Integer, String> FAILED = new ConcurrentHashMap<>();

    public static BlockEntity get(BlockDisguiseEntity entity) {
        BlockEntity cached = SHADOW.get(entity.getId());
        if (cached != null) return cached;
        if (FAILED.containsKey(entity.getId())) return null;

        BlockEntity created = create(entity);
        if (created == null) {
            if (entity.getOriginalBlockEntityNbt() == null
                    || entity.getOriginalBlockEntityNbt().isEmpty()) {
                return null;
            }
            FAILED.put(entity.getId(), "create returned null");
            return null;
        }
        SHADOW.put(entity.getId(), created);
        return created;
    }

    public static void clear(int entityId) {
        SHADOW.remove(entityId);
        FAILED.remove(entityId);
    }

    public static void clearAll() {
        SHADOW.clear();
        FAILED.clear();
    }

    private static BlockEntity create(BlockDisguiseEntity entity) {
        CompoundTag nbt = entity.getOriginalBlockEntityNbt();
        if (nbt == null || nbt.isEmpty()) return null;

        String typeId = nbt.getString("id");
        if (typeId.isEmpty()) {
            logFail(entity, "NBT缺少字段");
            return null;
        }

        ResourceLocation typeRl = ResourceLocation.tryParse(typeId);
        if (typeRl == null) {
            logFail(entity, "ID错误: " + typeId);
            return null;
        }

        BlockEntityType<?> type = ForgeRegistries.BLOCK_ENTITY_TYPES.getValue(typeRl);
        if (type == null) {
            logFail(entity, "实体方块未注册: " + typeRl);
            return null;
        }

        BlockPos pos = entity.blockPosition();
        BlockState state = entity.getBlockState();

        BlockEntity be;
        try {
            be = type.create(pos, state);
        } catch (Exception ex) {
            logFail(entity, "null (" + typeRl + "): " + ex);
            return null;
        }
        if (be == null) {
            logFail(entity, "null: " + typeRl);
            return null;
        }

        try {
            be.load(nbt);
        } catch (Exception ex) {
            logFail(entity, "实体方块 NBT 加载异常 (" + typeRl + "): " + ex);
            return null;
        }

        try {
            if (Minecraft.getInstance().level != null) {
                be.setLevel(Minecraft.getInstance().level);
            }
        } catch (Exception ex) {
            logFail(entity, "null (" + typeRl + "): " + ex);
            return null;
        }

        var dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        if (dispatcher.getRenderer(be) == null) {
            logFail(entity, "找不到实体方块的原始渲染器: " + typeRl);
            return null;
        }

        return be;
    }

    private static void logFail(BlockDisguiseEntity entity, String reason) {
        FriendWineMemeMod.LOGGER.error(
                "[FriendWineMeme] 重建失败 (entityId={}, block={}): {}",
                entity.getId(), entity.getOriginalBlockId(), reason);
    }
}