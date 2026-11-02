package com.sf.friendwinememe.init;

import com.sf.friendwinememe.FriendWineMemeMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class FriendWineMemeModSound {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, FriendWineMemeMod.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> TRANSPARENCY_MODE_MUSIC =
            SOUNDS.register("transparency_mode_music",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(
                                    FriendWineMemeMod.MODID, "transparency_mode_music")));

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }
}