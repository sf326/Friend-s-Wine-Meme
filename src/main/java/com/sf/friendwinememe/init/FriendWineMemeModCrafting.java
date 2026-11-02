package com.sf.friendwinememe.init;

import com.sf.friendwinememe.FriendWineMemeMod;
import com.sf.friendwinememe.item.TransparencyModeItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FriendWineMemeMod.MODID)
public class FriendWineMemeModCrafting {
    public static final int DEFAULT_SECONDS = 5;

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack result = event.getCrafting();
        if (!(result.getItem() instanceof TransparencyModeItem)) return;

        CompoundTag tag = result.getOrCreateTag();
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

            int sec = in.getTag() != null
                    && in.getTag().contains(TransparencyModeItem.NBT_DURATION_SEC)
                    ? in.getTag().getInt(TransparencyModeItem.NBT_DURATION_SEC)
                    : DEFAULT_SECONDS;

            if (sec < 0) hasInfinite = true;
            else totalSeconds += sec;
        }

        tag.putInt(TransparencyModeItem.NBT_DURATION_SEC, hasInfinite ? -1 : totalSeconds);
        result.setTag(tag);
    }
}