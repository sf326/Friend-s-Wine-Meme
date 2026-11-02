package com.sf.friendwinememe.entity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class FloatingTransparencyEntity extends Entity {
    private static final EntityDataAccessor<ItemStack> DATA_STACK =
            SynchedEntityData.defineId(FloatingTransparencyEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Long> DATA_START_MS =
            SynchedEntityData.defineId(FloatingTransparencyEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DATA_DURATION_MS =
            SynchedEntityData.defineId(FloatingTransparencyEntity.class, EntityDataSerializers.LONG);

    public int rangeRadius = 7;
    public boolean orbit = true;
    public double orbitSpeed = 25.0;
    public boolean affectBlocks = false;
    public boolean blockOrbit = false;
    public int layerY = 0;
    public ResourceLocation musicRl;

    public FloatingTransparencyEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.setSilent(true);
        this.setGlowingTag(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_STACK, ItemStack.EMPTY);
        builder.define(DATA_START_MS, 0L);
        builder.define(DATA_DURATION_MS, -1L);
    }

    public void setRenderStack(ItemStack stack) { this.entityData.set(DATA_STACK, stack); }
    public ItemStack getRenderStack() { return this.entityData.get(DATA_STACK); }
    public void setStartMs(long ms) { this.entityData.set(DATA_START_MS, ms); }
    public long getStartMs() { return this.entityData.get(DATA_START_MS); }
    public void setDurationMs(long ms) { this.entityData.set(DATA_DURATION_MS, ms); }
    public long getDurationMs() { return this.entityData.get(DATA_DURATION_MS); }

    public long getRemainingMs() {
        long dur = getDurationMs();
        if (dur < 0) return -1;
        return Math.max(0, dur - (System.currentTimeMillis() - getStartMs()));
    }

    public boolean isExpired() {
        long dur = getDurationMs();
        return dur >= 0 && System.currentTimeMillis() - getStartMs() >= dur;
    }

    @Override
    public boolean isPickable() { return true; }
    @Override
    public boolean isPushable() { return false; }
    @Override
    public boolean canBeCollidedWith() { return false; }
    @Override
    public boolean fireImmune() { return true; }
    @Override
    public boolean shouldShowName() { return true; }
    @Override
    public boolean isCurrentlyGlowing() { return true; }

    @Override
    public Component getName() {
        long remaining = getRemainingMs();
        if (remaining < 0) {
            return Component.translatable("item.friendwinememe.transparency_mode.infinite");
        }
        return Component.translatable("item.friendwinememe.transparency_mode.seconds",
                remaining / 1000);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setRenderStack(ItemStack.parseOptional(this.registryAccess(), tag.getCompound("Stack")));
        setStartMs(tag.getLong("StartMs"));
        setDurationMs(tag.getLong("DurationMs"));
        rangeRadius = tag.getInt("RangeRadius");
        orbit = tag.getBoolean("Orbit");
        orbitSpeed = tag.getDouble("OrbitSpeed");
        affectBlocks = tag.getBoolean("AffectBlocks");
        blockOrbit = tag.getBoolean("BlockOrbit");
        layerY = tag.getInt("LayerY");
        if (tag.contains("MusicRl")) musicRl = ResourceLocation.tryParse(tag.getString("MusicRl"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("Stack", getRenderStack().save(this.registryAccess(), new CompoundTag()));
        tag.putLong("StartMs", getStartMs());
        tag.putLong("DurationMs", getDurationMs());
        tag.putInt("RangeRadius", rangeRadius);
        tag.putBoolean("Orbit", orbit);
        tag.putDouble("OrbitSpeed", orbitSpeed);
        tag.putBoolean("AffectBlocks", affectBlocks);
        tag.putBoolean("BlockOrbit", blockOrbit);
        tag.putInt("LayerY", layerY);
        if (musicRl != null) tag.putString("MusicRl", musicRl.toString());
    }
}