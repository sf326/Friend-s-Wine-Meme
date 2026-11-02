package com.sf.friendwinememe;

import com.sf.friendwinememe.init.FriendWineMemeModMainMusicRegistry;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue BOUNCE_SPEED;
    public static final ModConfigSpec.DoubleValue MAX_HEIGHT;
    public static final ModConfigSpec.DoubleValue MIN_HEIGHT;
    public static final ModConfigSpec.BooleanValue PLAY_MUSIC;
    public static final ModConfigSpec.IntValue MUSIC_DISTANCE;
    public static final ModConfigSpec.BooleanValue ROTATE;
    public static final ModConfigSpec.DoubleValue ROTATE_SPEED;
    public static final ModConfigSpec.BooleanValue SHOW_LYRICS;
    public static final ModConfigSpec.ConfigValue<String> MUSIC_NAME;
    public static final ModConfigSpec.BooleanValue MUSIC_LOOP;
    public static final ModConfigSpec.IntValue RANGE_RADIUS;
    public static final ModConfigSpec.BooleanValue ORBIT;
    public static final ModConfigSpec.DoubleValue ORBIT_SPEED;
    public static final ModConfigSpec.BooleanValue AFFECT_BLOCKS;
    public static final ModConfigSpec.BooleanValue BLOCK_ORBIT;
    public static final ModConfigSpec.BooleanValue VIEW_SHAKE;
    public static final ModConfigSpec.DoubleValue VIEW_SHAKE_MAX;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("FriendWineMeme configuration").push("TransparencyMode");
        BOUNCE_SPEED = b.comment("Bounce speed multiplier")
                .defineInRange("bounceSpeed", 1.0, 0.1, 10.0);
        MAX_HEIGHT = b.comment("Maximum squash height multiplier")
                .defineInRange("maxHeight", 1.3, 0.0, 3.0);
        MIN_HEIGHT = b.comment("Minimum squash height multiplier")
                .defineInRange("minHeight", 0.5, 0.0, 1.0);
        PLAY_MUSIC = b.comment("Whether to play music").define("playMusic", true);
        MUSIC_DISTANCE = b.comment("Maximum audible distance for music (blocks)")
                .defineInRange("musicDistance", 25, 1, 200);
        ROTATE = b.comment("Whether to rotate during the effect").define("rotate", true);
        ROTATE_SPEED = b.comment("Rotation speed (degrees/second)")
                .defineInRange("rotateSpeed", 360.0, 0.0, 3600.0);
        SHOW_LYRICS = b.comment("Whether to show lyrics on HUD").define("showLyrics", true);
        MUSIC_NAME = b.comment("Currently selected music name")
                .define("musicName", FriendWineMemeModMainMusicRegistry.getDefault().getPath());
        MUSIC_LOOP = b.comment("Whether to loop the music").define("musicLoop", true);
        RANGE_RADIUS = b.comment("Range mode radius (blocks)")
                .defineInRange("rangeRadius", 7, 1, 32);
        ORBIT = b.comment("Whether targets orbit the floating item")
                .define("orbit", true);
        ORBIT_SPEED = b.comment("Orbit speed (degrees/second)")
                .defineInRange("orbitSpeed", 25.0, 0.0, 3600.0);
        AFFECT_BLOCKS = b.comment("Whether blocks are affected by range mode")
                .define("affectBlocks", false);
        BLOCK_ORBIT = b.comment("Whether blocks orbit (requires orbit=true)")
                .define("blockOrbit", false);
        VIEW_SHAKE = b.comment("Whether the player's view shakes during the effect")
                .define("viewShake", true);
        VIEW_SHAKE_MAX = b.comment("Maximum view shake amplitude (degrees)")
                .defineInRange("viewShakeMax", 5.0, 0.0, 90.0);
        b.pop();
        SPEC = b.build();
    }

    private static boolean useRemote = false;
    private static double remoteBounceSpeed, remoteMaxHeight, remoteMinHeight;
    private static double remoteRotateSpeed, remoteOrbitSpeed, remoteViewShakeMax;
    private static int remoteMusicDistance, remoteRangeRadius;
    private static boolean remotePlayMusic, remoteRotate, remoteShowLyrics, remoteMusicLoop;
    private static boolean remoteOrbit, remoteAffectBlocks, remoteBlockOrbit, remoteViewShake;
    private static String remoteMusicName = FriendWineMemeModMainMusicRegistry.getDefault().getPath();

    public static void clearRemote() { useRemote = false; }

    public static void applyRemote(double bounce, double maxH, double minH,
                                   boolean music, int distance, boolean rotate,
                                   double rotateSpeed, boolean showLyrics, String musicName,
                                   boolean musicLoop,
                                   int rangeRadius, boolean orbit, double orbitSpeed,
                                   boolean affectBlocks, boolean blockOrbit,
                                   boolean viewShake, double viewShakeMax) {
        remoteBounceSpeed = bounce;
        remoteMaxHeight = maxH;
        remoteMinHeight = minH;
        remotePlayMusic = music;
        remoteMusicDistance = distance;
        remoteRotate = rotate;
        remoteRotateSpeed = rotateSpeed;
        remoteShowLyrics = showLyrics;
        remoteMusicName = musicName;
        remoteMusicLoop = musicLoop;
        remoteRangeRadius = rangeRadius;
        remoteOrbit = orbit;
        remoteOrbitSpeed = orbitSpeed;
        remoteAffectBlocks = affectBlocks;
        remoteBlockOrbit = blockOrbit;
        remoteViewShake = viewShake;
        remoteViewShakeMax = viewShakeMax;
        useRemote = true;
    }

    public static double getBounceSpeed() { return useRemote ? remoteBounceSpeed : BOUNCE_SPEED.get(); }
    public static double getMaxHeight() { return useRemote ? remoteMaxHeight : MAX_HEIGHT.get(); }
    public static double getMinHeight() { return useRemote ? remoteMinHeight : MIN_HEIGHT.get(); }
    public static boolean isPlayMusic() { return useRemote ? remotePlayMusic : PLAY_MUSIC.get(); }
    public static int getMusicDistance() { return useRemote ? remoteMusicDistance : MUSIC_DISTANCE.get(); }
    public static boolean isRotate() { return useRemote ? remoteRotate : ROTATE.get(); }
    public static double getRotateSpeed() { return useRemote ? remoteRotateSpeed : ROTATE_SPEED.get(); }
    public static boolean isShowLyrics() { return useRemote ? remoteShowLyrics : SHOW_LYRICS.get(); }
    public static String getMusicName() { return useRemote ? remoteMusicName : MUSIC_NAME.get(); }
    public static boolean isMusicLoop() { return useRemote ? remoteMusicLoop : MUSIC_LOOP.get(); }
    public static int getRangeRadius() { return useRemote ? remoteRangeRadius : RANGE_RADIUS.get(); }
    public static boolean isOrbit() { return useRemote ? remoteOrbit : ORBIT.get(); }
    public static double getOrbitSpeed() { return useRemote ? remoteOrbitSpeed : ORBIT_SPEED.get(); }
    public static boolean isAffectBlocks() { return useRemote ? remoteAffectBlocks : AFFECT_BLOCKS.get(); }
    public static boolean isBlockOrbit() {
        boolean orbitOn = isOrbit();
        boolean blockOn = useRemote ? remoteBlockOrbit : BLOCK_ORBIT.get();
        return orbitOn && blockOn;
    }
    public static boolean isViewShake() { return useRemote ? remoteViewShake : VIEW_SHAKE.get(); }
    public static double getViewShakeMax() { return useRemote ? remoteViewShakeMax : VIEW_SHAKE_MAX.get(); }

    public static void save() { SPEC.save(); }
}