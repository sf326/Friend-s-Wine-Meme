package com.sf.friendwinememe.init;

import com.sf.friendwinememe.FriendWineMemeMod;
import com.sf.friendwinememe.entity.BlockDisguiseEntity;
import com.sf.friendwinememe.entity.FloatingTransparencyEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class FriendWineMemeModEntity {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FriendWineMemeMod.MODID);

    public static final RegistryObject<EntityType<BlockDisguiseEntity>> BLOCK_DISGUISE =
            ENTITIES.register("block_disguise",
                    () -> EntityType.Builder.<BlockDisguiseEntity>of(BlockDisguiseEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .noSummon()
                            .fireImmune()
                            .build("block_disguise"));

    public static final RegistryObject<EntityType<FloatingTransparencyEntity>> FLOATING_TRANSPARENCY =
            ENTITIES.register("floating_transparency",
                    () -> EntityType.Builder.<FloatingTransparencyEntity>of(
                                    FloatingTransparencyEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .noSummon()
                            .fireImmune()
                            .build("floating_transparency"));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}