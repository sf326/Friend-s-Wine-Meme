package com.sf.friendwinememe.init;

import com.sf.friendwinememe.FriendWineMemeMod;
import com.sf.friendwinememe.entity.BlockDisguiseEntity;
import com.sf.friendwinememe.entity.FloatingTransparencyEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class FriendWineMemeModEntity {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, FriendWineMemeMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<BlockDisguiseEntity>> BLOCK_DISGUISE =
            ENTITIES.register("block_disguise",
                    () -> EntityType.Builder.<BlockDisguiseEntity>of(BlockDisguiseEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .noSummon()
                            .fireImmune()
                            .build(String.valueOf(ResourceLocation.fromNamespaceAndPath(FriendWineMemeMod.MODID, "block_disguise"))));

    public static final DeferredHolder<EntityType<?>, EntityType<FloatingTransparencyEntity>> FLOATING_TRANSPARENCY =
            ENTITIES.register("floating_transparency",
                    () -> EntityType.Builder.<FloatingTransparencyEntity>of(
                                    FloatingTransparencyEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .noSummon()
                            .fireImmune()
                            .build(String.valueOf(ResourceLocation.fromNamespaceAndPath(FriendWineMemeMod.MODID, "floating_transparency"))));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}