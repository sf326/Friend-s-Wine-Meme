package com.sf.friendwinememe.init;

import com.sf.friendwinememe.FriendWineMemeMod;
import com.sf.friendwinememe.item.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class FriendWineMemeModItem {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, FriendWineMemeMod.MODID);

    public static final DeferredHolder<Item, Item> TRANSPARENCY_MODE =
            ITEMS.register("transparency_mode",
                    () -> new TransparencyModeItem(new Item.Properties().stacksTo(64)));

    public static final DeferredHolder<Item, Item> TRANSPARENCY_MODE_OFF =
            ITEMS.register("transparency_mode_off",
                    () -> new TransparencyModeOffItem(new Item.Properties().stacksTo(64)));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}