package com.sf.friendwinememe;

import com.mojang.logging.LogUtils;
import com.sf.friendwinememe.init.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(FriendWineMemeMod.MODID)
public class FriendWineMemeMod {
    public static final String MODID = "friendwinememe";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FriendWineMemeMod(IEventBus modEventBus, ModContainer container) {
        FriendWineMemeModItem.register(modEventBus);
        FriendWineMemeModSound.register(modEventBus);
        FriendWineMemeModEntity.register(modEventBus);
        FriendWineMemeModTabs.register(modEventBus);

        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC, "FriendWineMeme.toml");

        modEventBus.addListener(FriendWineMemeModNetwork::registerPayloads);
    }
}