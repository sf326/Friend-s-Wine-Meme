package com.sf.friendwinememe;

import com.mojang.logging.LogUtils;
import com.sf.friendwinememe.init.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(FriendWineMemeMod.MODID)
public class FriendWineMemeMod {
    public static final String MODID = "friendwinememe";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FriendWineMemeMod(FMLJavaModLoadingContext context) {
        IEventBus bus = context.getModEventBus();

        FriendWineMemeModItem.register(bus);
        FriendWineMemeModSound.register(bus);
        FriendWineMemeModEntity.register(bus);
        FriendWineMemeModTabs.register(bus);

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC, "FriendWineMeme.toml");

        FriendWineMemeModNetwork.init();
        MinecraftForge.EVENT_BUS.register(this);
    }
}