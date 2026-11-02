package com.sf.friendwinememe.init;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sf.friendwinememe.Config;
import com.sf.friendwinememe.ConfigSync;
import com.sf.friendwinememe.FriendWineMemeMod;
import com.sf.friendwinememe.entity.BlockDisguiseEntity;
import com.sf.friendwinememe.item.TransparencyModeItem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.StringJoiner;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = FriendWineMemeMod.MODID)
public class FriendWineMemeModCommand {

    private static final Predicate<CommandSourceStack> OP = src -> src.hasPermission(2);

    private static final String[] HELP_KEYS = {
            "command.friendwinememe.help.playershake",
            "command.friendwinememe.help.playerlyrics",
            "command.friendwinememe.help.reload",
            "command.friendwinememe.help.stopall",
            "command.friendwinememe.help.give",
            "command.friendwinememe.help.inspect",
            "command.friendwinememe.help.musiclist",
            "command.friendwinememe.help.musicselect",
            "command.friendwinememe.help.bounce",
            "command.friendwinememe.help.maxheight",
            "command.friendwinememe.help.minheight",
            "command.friendwinememe.help.music",
            "command.friendwinememe.help.distance",
            "command.friendwinememe.help.rotate",
            "command.friendwinememe.help.rotatespeed",
            "command.friendwinememe.help.lyrics",
            "command.friendwinememe.help.musicloop",
            "command.friendwinememe.help.rangeradius",
            "command.friendwinememe.help.orbit",
            "command.friendwinememe.help.orbitspeed",
            "command.friendwinememe.help.affectblocks",
            "command.friendwinememe.help.blockorbit",
            "command.friendwinememe.help.viewshake",
            "command.friendwinememe.help.viewshakemax",
            "command.friendwinememe.help.giveeffectself"
    };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();

        d.register(Commands.literal("friendwinememe")
                // 帮助
                .then(Commands.literal("help")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(
                                    () -> Component.translatable("command.friendwinememe.help.header"),
                                    false);
                            for (String key : HELP_KEYS) {
                                ctx.getSource().sendSuccess(
                                        () -> Component.translatable(key),
                                        false);
                            }
                            return 1;
                        }))

                // 重载配置
                .then(Commands.literal("reload")
                        .requires(OP)
                        .executes(ctx -> {
                            Config.SPEC.save();
                            ConfigSync.readServerValues();
                            MinecraftServer server = ctx.getSource().getServer();
                            if (server != null && server.isDedicatedServer()) {
                                ConfigSync.syncToAll(server);
                            }
                            ctx.getSource().sendSuccess(
                                    () -> Component.translatable("command.friendwinememe.reload"),
                                    true);
                            return 1;
                        }))

                // 停止所有特效
                .then(Commands.literal("stopAll")
                        .requires(OP)
                        .executes(ctx -> {
                            MinecraftServer server = ctx.getSource().getServer();
                            int count = 0;
                            if (server != null) {
                                for (var lvl : server.getAllLevels()) {
                                    ArrayList<BlockDisguiseEntity> list = new ArrayList<>();
                                    for (var e : lvl.getEntities().getAll()) {
                                        if (e instanceof BlockDisguiseEntity bde) list.add(bde);
                                    }
                                    for (BlockDisguiseEntity bde : list) {
                                        bde.restore();
                                        count++;
                                    }
                                }
                            }
                            FriendWineMemeModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
                                    new FriendWineMemeModNetwork.StopAllEffectPacket());
                            final int c = count;
                            ctx.getSource().sendSuccess(
                                    () -> Component.translatable("command.friendwinememe.stopall", c),
                                    true);
                            return 1;
                        }))

                // 给玩家物品
                .then(Commands.literal("give")
                        .requires(OP)
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0, 999999))
                                .executes(ctx -> {
                                    int sec = IntegerArgumentType.getInteger(ctx, "seconds");
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    ItemStack stack = new ItemStack(FriendWineMemeModItem.TRANSPARENCY_MODE.get());
                                    CompoundTag tag = stack.getOrCreateTag();
                                    tag.putInt(TransparencyModeItem.NBT_DURATION_SEC, sec);
                                    if (!player.getInventory().add(stack)) {
                                        player.drop(stack, false);
                                    }
                                    final int s = sec;
                                    ctx.getSource().sendSuccess(
                                            () -> Component.translatable("command.friendwinememe.give", s),
                                            true);
                                    return 1;
                                })))

                // 检测方块
                .then(Commands.literal("inspect")
                        .requires(OP)
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            var level = player.serverLevel();
                            double reach = 5.0D;
                            Vec3 eye = player.getEyePosition();
                            Vec3 look = player.getViewVector(1.0F);
                            Vec3 end = eye.add(look.scale(reach));
                            BlockHitResult hit = level.clip(new ClipContext(
                                    eye, end,
                                    ClipContext.Block.OUTLINE,
                                    ClipContext.Fluid.NONE,
                                    player));

                            if (hit.getType() == HitResult.Type.MISS) {
                                ctx.getSource().sendSuccess(
                                        () -> Component.translatable("command.friendwinememe.inspect.miss"),
                                        false);
                                return 0;
                            }

                            BlockPos pos = hit.getBlockPos();
                            var state = level.getBlockState(pos);
                            var be = level.getBlockEntity(pos);

                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("§aBlock: §f" + pos), false);
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("§aState: §f"
                                            + ForgeRegistries.BLOCKS.getKey(state.getBlock())), false);
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("§aBlockEntity: §f"
                                            + (be != null ? be.getType() : "null")), false);

                            if (be != null) {
                                CompoundTag nbt = be.saveWithoutMetadata();
                                ResourceLocation typeId = ForgeRegistries.BLOCK_ENTITY_TYPES
                                        .getKey(be.getType());
                                if (typeId != null) {
                                    nbt.putString("id", typeId.toString());
                                }
                                ctx.getSource().sendSuccess(
                                        () -> Component.literal("§aType id: §f" + typeId), false);
                                ctx.getSource().sendSuccess(
                                        () -> Component.literal("§aNBT keys: §f" + nbt.getAllKeys()), false);
                                ctx.getSource().sendSuccess(
                                        () -> Component.literal("§aNBT: §f" + nbt), false);
                            }
                            return 1;
                        }))

                // 音乐列表
                .then(Commands.literal("musicList")
                        .requires(OP)
                        .executes(ctx -> {
                            StringJoiner joiner = new StringJoiner(", ");
                            for (String name : FriendWineMemeModMainMusicRegistry.names()) {
                                joiner.add(name);
                            }
                            String list = joiner.toString();
                            ctx.getSource().sendSuccess(
                                    () -> Component.translatable("command.friendwinememe.musiclist", list),
                                    false);
                            return 1;
                        }))

                // 音乐切换
                .then(Commands.literal("musicSelect")
                        .requires(OP)
                        .executes(ctx -> query(Config.MUSIC_NAME, "musicName", ctx.getSource()))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (String name : FriendWineMemeModMainMusicRegistry.names()) {
                                        if (name.startsWith(builder.getRemaining())) {
                                            builder.suggest(name);
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "name");
                                    if (!FriendWineMemeModMainMusicRegistry.MUSIC.containsKey(name)) {
                                        ctx.getSource().sendSuccess(
                                                () -> Component.translatable(
                                                        "command.friendwinememe.musicselect.unknown", name),
                                                false);
                                        return 0;
                                    }
                                    Config.MUSIC_NAME.set(name);
                                    Config.SPEC.save();
                                    ConfigSync.readServerValues();
                                    MinecraftServer server = ctx.getSource().getServer();
                                    if (server != null && server.isDedicatedServer()) {
                                        ConfigSync.syncToAll(server);
                                    }
                                    ctx.getSource().sendSuccess(
                                            () -> Component.translatable(
                                                    "command.friendwinememe.musicselect", name),
                                            true);
                                    return 1;
                                })))

                .then(Commands.literal("bounce")
                        .requires(OP)
                        .executes(ctx -> query(Config.BOUNCE_SPEED, "bounceSpeed", ctx.getSource()))
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.1, 10.0))
                                .executes(ctx -> set(Config.BOUNCE_SPEED,
                                        DoubleArgumentType.getDouble(ctx, "value"),
                                        "bounceSpeed", ctx.getSource()))))

                .then(Commands.literal("maxHeight")
                        .requires(OP)
                        .executes(ctx -> query(Config.MAX_HEIGHT, "maxHeight", ctx.getSource()))
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 3.0))
                                .executes(ctx -> set(Config.MAX_HEIGHT,
                                        DoubleArgumentType.getDouble(ctx, "value"),
                                        "maxHeight", ctx.getSource()))))

                .then(Commands.literal("minHeight")
                        .requires(OP)
                        .executes(ctx -> query(Config.MIN_HEIGHT, "minHeight", ctx.getSource()))
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 1.0))
                                .executes(ctx -> set(Config.MIN_HEIGHT,
                                        DoubleArgumentType.getDouble(ctx, "value"),
                                        "minHeight", ctx.getSource()))))

                .then(Commands.literal("music")
                        .requires(OP)
                        .executes(ctx -> query(Config.PLAY_MUSIC, "playMusic", ctx.getSource()))
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> set(Config.PLAY_MUSIC,
                                        BoolArgumentType.getBool(ctx, "value"),
                                        "playMusic", ctx.getSource()))))

                .then(Commands.literal("distance")
                        .requires(OP)
                        .executes(ctx -> query(Config.MUSIC_DISTANCE, "musicDistance", ctx.getSource()))
                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 100))
                                .executes(ctx -> set(Config.MUSIC_DISTANCE,
                                        IntegerArgumentType.getInteger(ctx, "value"),
                                        "musicDistance", ctx.getSource()))))

                .then(Commands.literal("rotate")
                        .requires(OP)
                        .executes(ctx -> query(Config.ROTATE, "rotate", ctx.getSource()))
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> set(Config.ROTATE,
                                        BoolArgumentType.getBool(ctx, "value"),
                                        "rotate", ctx.getSource()))))

                .then(Commands.literal("rotateSpeed")
                        .requires(OP)
                        .executes(ctx -> query(Config.ROTATE_SPEED, "rotateSpeed", ctx.getSource()))
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 3600.0))
                                .executes(ctx -> set(Config.ROTATE_SPEED,
                                        DoubleArgumentType.getDouble(ctx, "value"),
                                        "rotateSpeed", ctx.getSource()))))

                .then(Commands.literal("lyrics")
                        .requires(OP)
                        .executes(ctx -> query(Config.SHOW_LYRICS, "showLyrics", ctx.getSource()))
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> set(Config.SHOW_LYRICS,
                                        BoolArgumentType.getBool(ctx, "value"),
                                        "showLyrics", ctx.getSource()))))

                .then(Commands.literal("musicLoop")
                        .requires(OP)
                        .executes(ctx -> query(Config.MUSIC_LOOP, "musicLoop", ctx.getSource()))
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> set(Config.MUSIC_LOOP,
                                        BoolArgumentType.getBool(ctx, "value"),
                                        "musicLoop", ctx.getSource()))))

                .then(Commands.literal("rangeRadius")
                        .requires(OP)
                        .executes(ctx -> query(Config.RANGE_RADIUS, "rangeRadius", ctx.getSource()))
                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 32))
                                .executes(ctx -> set(Config.RANGE_RADIUS,
                                        IntegerArgumentType.getInteger(ctx, "value"),
                                        "rangeRadius", ctx.getSource()))))

                .then(Commands.literal("orbit")
                        .requires(OP)
                        .executes(ctx -> query(Config.ORBIT, "orbit", ctx.getSource()))
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> set(Config.ORBIT,
                                        BoolArgumentType.getBool(ctx, "value"),
                                        "orbit", ctx.getSource()))))

                .then(Commands.literal("orbitSpeed")
                        .requires(OP)
                        .executes(ctx -> query(Config.ORBIT_SPEED, "orbitSpeed", ctx.getSource()))
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 3600.0))
                                .executes(ctx -> set(Config.ORBIT_SPEED,
                                        DoubleArgumentType.getDouble(ctx, "value"),
                                        "orbitSpeed", ctx.getSource()))))

                .then(Commands.literal("affectBlocks")
                        .requires(OP)
                        .executes(ctx -> query(Config.AFFECT_BLOCKS, "affectBlocks", ctx.getSource()))
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> set(Config.AFFECT_BLOCKS,
                                        BoolArgumentType.getBool(ctx, "value"),
                                        "affectBlocks", ctx.getSource()))))

                .then(Commands.literal("blockOrbit")
                        .requires(OP)
                        .executes(ctx -> query(Config.BLOCK_ORBIT, "blockOrbit", ctx.getSource()))
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> set(Config.BLOCK_ORBIT,
                                        BoolArgumentType.getBool(ctx, "value"),
                                        "blockOrbit", ctx.getSource()))))

                .then(Commands.literal("viewShake")
                        .requires(OP)
                        .executes(ctx -> query(Config.VIEW_SHAKE, "viewShake", ctx.getSource()))
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> set(Config.VIEW_SHAKE,
                                        BoolArgumentType.getBool(ctx, "value"),
                                        "viewShake", ctx.getSource()))))

                .then(Commands.literal("viewShakeMax")
                        .requires(OP)
                        .executes(ctx -> query(Config.VIEW_SHAKE_MAX, "viewShakeMax", ctx.getSource()))
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 90.0))
                                .executes(ctx -> set(Config.VIEW_SHAKE_MAX,
                                        DoubleArgumentType.getDouble(ctx, "value"),
                                        "viewShakeMax", ctx.getSource()))))

                .then(Commands.literal("giveeffectself")
                        .requires(OP)
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0, 999999))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int sec = IntegerArgumentType.getInteger(ctx, "seconds");
                                    long durationMs = sec > 0 ? (long) sec * 1000L : -1L;
                                    long now = System.currentTimeMillis();

                                    player.getPersistentData().putLong("FriendWineMemeDuration", durationMs);
                                    player.getPersistentData().putLong("FriendWineMemeStart", now);

                                    FriendWineMemeModNetwork.CHANNEL.send(
                                            PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                                            new FriendWineMemeModNetwork.TransparencyEffectPacket(
                                                    player.getId(), sec, 0L));
                                    if (Config.isPlayMusic()) {
                                        var musicRl = FriendWineMemeModMainMusicRegistry.get(Config.getMusicName());
                                        FriendWineMemeModNetwork.CHANNEL.send(
                                                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                                                new FriendWineMemeModNetwork.PlayMusicPacket(
                                                        player.blockPosition(), player.getId(), now, musicRl));
                                    }

                                    ctx.getSource().sendSuccess(
                                            () -> Component.translatable("command.friendwinememe.giveeffectself", sec),
                                            true);
                                    return 1;
                                })))
        );
    }

    private static <T> int query(ForgeConfigSpec.ConfigValue<T> value,
                                 String key, CommandSourceStack source) {
        source.sendSuccess(
                () -> Component.translatable("command.friendwinememe.query",
                        key, String.valueOf(value.get())),
                false);
        return 1;
    }

    private static <T> int set(ForgeConfigSpec.ConfigValue<T> value, T newValue,
                               String key, CommandSourceStack source) {
        value.set(newValue);
        Config.SPEC.save();
        ConfigSync.readServerValues();

        MinecraftServer server = source.getServer();
        if (server != null && server.isDedicatedServer()) {
            ConfigSync.syncToAll(server);
        }

        source.sendSuccess(
                () -> Component.translatable("command.friendwinememe.set", key, String.valueOf(newValue)),
                true);
        return 1;
    }
}