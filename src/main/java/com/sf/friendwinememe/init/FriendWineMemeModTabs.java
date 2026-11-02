package com.sf.friendwinememe.init;

import com.sf.friendwinememe.FriendWineMemeMod;
import com.sf.friendwinememe.item.TransparencyModeItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class FriendWineMemeModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FriendWineMemeMod.MODID);


    private static ItemStack TRANSPARENCY_MODE_preset(int seconds) {
        ItemStack stack = new ItemStack(FriendWineMemeModItem.TRANSPARENCY_MODE.get());
        if (seconds >= 0) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putInt(TransparencyModeItem.NBT_DURATION_SEC, seconds);
        }
        return stack;
    }

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.friendwinememe"))
                    .icon(() -> TRANSPARENCY_MODE_preset(5))
                    .displayItems((params, output) -> {
                        output.accept(TRANSPARENCY_MODE_preset(5));
                        output.accept(TRANSPARENCY_MODE_preset(15));
                        output.accept(TRANSPARENCY_MODE_preset(30));
                        output.accept(TRANSPARENCY_MODE_preset(60));
                        output.accept(TRANSPARENCY_MODE_preset(114514));
                        output.accept(TRANSPARENCY_MODE_preset(-1));
                        output.accept(new ItemStack(FriendWineMemeModItem.TRANSPARENCY_MODE_OFF.get()));
                    })
                    .build());

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}