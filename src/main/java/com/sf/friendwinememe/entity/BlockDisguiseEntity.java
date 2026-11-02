package com.sf.friendwinememe.entity;

import com.sf.friendwinememe.init.FriendWineMemeModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public class BlockDisguiseEntity extends Entity {

    private static final EntityDataAccessor<BlockState> DATA_BLOCK =
            SynchedEntityData.defineId(BlockDisguiseEntity.class, EntityDataSerializers.BLOCK_STATE);
    private static final EntityDataAccessor<String> DATA_BLOCK_ID =
            SynchedEntityData.defineId(BlockDisguiseEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<CompoundTag> DATA_BE_NBT =
            SynchedEntityData.defineId(BlockDisguiseEntity.class, EntityDataSerializers.COMPOUND_TAG);

    private CompoundTag blockNbt = new CompoundTag();
    private long durationMs = -1L;
    private long effectStartMs = 0L;
    private long musicStartMs = 0L;
    private BlockPos barrierPos = null;

    public BlockDisguiseEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.setSilent(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_BLOCK, Blocks.AIR.defaultBlockState());
        builder.define(DATA_BLOCK_ID, "");
        builder.define(DATA_BE_NBT, new CompoundTag());
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            if (barrierPos != null) {
                if (this.isRemoved() || !(this.level() instanceof ServerLevel sl)
                        || !sl.getBlockState(barrierPos).is(Blocks.BARRIER)) {
                    barrierPos = null;
                }
            }
            if (durationMs > 0 && System.currentTimeMillis() - effectStartMs >= durationMs) {
                restore();
            }
        }
    }

    @Override public void move(MoverType type, Vec3 pos) {}
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return true; }
    @Override public boolean fireImmune() { return true; }

    // 同步
    public void setBlockState(BlockState state) { this.entityData.set(DATA_BLOCK, state); }
    public BlockState getBlockState() { return this.entityData.get(DATA_BLOCK); }

    public void setOriginalBlockId(ResourceLocation id) {
        this.entityData.set(DATA_BLOCK_ID, id == null ? "" : id.toString());
    }
    public ResourceLocation getOriginalBlockId() {
        String s = this.entityData.get(DATA_BLOCK_ID);
        return s.isEmpty() ? null : ResourceLocation.tryParse(s);
    }

    public void setOriginalBlockEntityNbt(CompoundTag nbt) {
        this.entityData.set(DATA_BE_NBT, nbt == null ? new CompoundTag() : nbt);
    }
    public CompoundTag getOriginalBlockEntityNbt() {
        return this.entityData.get(DATA_BE_NBT);
    }

    public void setBlockNbt(CompoundTag tag) { this.blockNbt = tag; }
    public CompoundTag getBlockNbt() { return blockNbt; }
    public void setDurationMs(long ms) { this.durationMs = ms; }
    public void setEffectStartMs(long ms) { this.effectStartMs = ms; }
    public void setMusicStartMs(long ms) { this.musicStartMs = ms; }
    public void setBarrierPos(BlockPos pos) { this.barrierPos = pos; }
    public BlockPos getBarrierPos() { return barrierPos; }

    private void clearBarrier() {
        if (barrierPos != null && this.level() instanceof ServerLevel sl) {
            if (sl.getBlockState(barrierPos).is(Blocks.BARRIER)) {
                sl.removeBlock(barrierPos, false);
            }
        }
        barrierPos = null;
    }

    // 还原
    public void restore() {
        if (this.level().isClientSide) return;
        clearBarrier();

        BlockState state = getBlockState();
        if (state.isAir()) { this.discard(); return; }

        CompoundTag savedNbt = this.blockNbt == null ? new CompoundTag() : this.blockNbt.copy();

        this.level().setBlock(this.blockPosition(), state, 3);
        if (!savedNbt.isEmpty()) {
            var be = this.level().getBlockEntity(this.blockPosition());
            if (be != null) {
                HolderLookup.Provider registries = this.level().registryAccess();
                be.loadWithComponents(savedNbt, registries);
                be.setChanged();
                if (this.level() instanceof ServerLevel sl) {
                    sl.getChunkSource().blockChanged(this.blockPosition());
                }
            }
        }

        PacketDistributor.sendToAllPlayers(new FriendWineMemeModNetwork.StopEffectPacket(this.getId()));
        this.discard();
    }

    public void restoreSilent() {
        if (this.level().isClientSide) return;
        clearBarrier();

        BlockState state = getBlockState();
        if (state.isAir()) { this.discard(); return; }

        CompoundTag savedNbt = this.blockNbt == null ? new CompoundTag() : this.blockNbt.copy();

        this.level().setBlock(this.blockPosition(), state, 3);
        if (!savedNbt.isEmpty()) {
            var be = this.level().getBlockEntity(this.blockPosition());
            if (be != null) {
                HolderLookup.Provider registries = this.level().registryAccess();
                be.loadWithComponents(savedNbt, registries);
                be.setChanged();
                if (this.level() instanceof ServerLevel sl) {
                    sl.getChunkSource().blockChanged(this.blockPosition());
                }
            }
        }
        this.discard();
    }

    @Override
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
        clearBarrier();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("BlockState")) {
            var nbt = tag.getCompound("BlockState");
            this.setBlockState(NbtUtils.readBlockState(
                    this.level().holderLookup(Registries.BLOCK), nbt));
        }
        this.blockNbt = tag.getCompound("BlockNbt");
        this.durationMs = tag.getLong("DurationMs");
        this.effectStartMs = tag.getLong("EffectStartMs");
        this.musicStartMs = tag.getLong("MusicStartMs");
        if (tag.contains("BarrierPos")) {
            this.barrierPos = NbtUtils.readBlockPos(
                    tag, "BarrierPos").orElse(null);
        }
        if (tag.contains("OriginalBlockId")) {
            this.setOriginalBlockId(ResourceLocation.tryParse(tag.getString("OriginalBlockId")));
        }
        this.setOriginalBlockEntityNbt(tag.getCompound("OriginalBlockEntityNbt"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("BlockState", NbtUtils.writeBlockState(getBlockState()));
        tag.put("BlockNbt", blockNbt);
        tag.putLong("DurationMs", durationMs);
        tag.putLong("EffectStartMs", effectStartMs);
        tag.putLong("MusicStartMs", musicStartMs);
        if (barrierPos != null) {
            tag.put("BarrierPos", NbtUtils.writeBlockPos(barrierPos));
        }
        ResourceLocation blockId = getOriginalBlockId();
        if (blockId != null) {
            tag.putString("OriginalBlockId", blockId.toString());
        }
        tag.put("OriginalBlockEntityNbt", getOriginalBlockEntityNbt());
    }
}