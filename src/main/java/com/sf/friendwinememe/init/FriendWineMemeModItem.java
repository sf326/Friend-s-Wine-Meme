package com.sf.friendwinememe.init;

import com.sf.friendwinememe.FriendWineMemeMod;
import com.sf.friendwinememe.item.*;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class FriendWineMemeModItem {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FriendWineMemeMod.MODID);

    public static final RegistryObject<Item> TRANSPARENCY_MODE =
            ITEMS.register("transparency_mode",
                    () -> new TransparencyModeItem(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> TRANSPARENCY_MODE_OFF =
            ITEMS.register("transparency_mode_off",
                    () -> new TransparencyModeOffItem(new Item.Properties().stacksTo(64)));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}