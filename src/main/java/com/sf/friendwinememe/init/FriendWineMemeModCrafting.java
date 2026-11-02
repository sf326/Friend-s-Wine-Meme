package com.sf.friendwinememe.init;

import com.sf.friendwinememe.FriendWineMemeMod;
import com.sf.friendwinememe.item.TransparencyModeItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = FriendWineMemeMod.MODID)
public class FriendWineMemeModCrafting {
    public static final int DEFAULT_SECONDS = 5;

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack result = event.getCrafting();
        if (!(result.getItem() instanceof TransparencyModeItem)) return;

        CustomData data = result.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = data != null ? data.copyTag() : new CompoundTag();

        int existing = tag.contains(TransparencyModeItem.NBT_DURATION_SEC)
                ? tag.getInt(TransparencyModeItem.NBT_DURATION_SEC)
                : 0;

        int totalSeconds = existing > 0 ? existing : DEFAULT_SECONDS;
        boolean hasInfinite = existing < 0;

        var inventory = event.getInventory();

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack in = inventory.getItem(i);
            if (in.isEmpty()) continue;
            if (!(in.getItem() instanceof TransparencyModeItem)) continue;

            CustomData inData = in.get(DataComponents.CUSTOM_DATA);
            int sec = DEFAULT_SECONDS;
            if (inData != null) {
                CompoundTag inTag = inData.copyTag();
                if (inTag.contains(TransparencyModeItem.NBT_DURATION_SEC)) {
                    sec = inTag.getInt(TransparencyModeItem.NBT_DURATION_SEC);
                }
            }

            if (sec < 0) hasInfinite = true;
            else totalSeconds += sec;
        }

        tag.putInt(TransparencyModeItem.NBT_DURATION_SEC, hasInfinite ? -1 : totalSeconds);
        result.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}