package com.sf.friendwinememe.init;

import com.sf.friendwinememe.FriendWineMemeMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class FriendWineMemeModSound {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, FriendWineMemeMod.MODID);

    public static final RegistryObject<SoundEvent> TRANSPARENCY_MODE_MUSIC = SOUNDS.register(
            "transparency_mode_music",
            () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(FriendWineMemeMod.MODID, "transparency_mode_music")));

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }
}