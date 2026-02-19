package com.maeiro.serenebetterwinter;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ServerConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue PASS_THROUGH_HIDDEN_BLOCKS;
    public static final ForgeConfigSpec.BooleanValue REMOVE_LIGHT_BLOCKING_FROM_HIDDEN_BLOCKS;
    public static final ForgeConfigSpec.BooleanValue FORCE_RELIGHT_ON_SEASON_CHANGE;
    public static final ForgeConfigSpec.IntValue RELIGHT_CHUNK_LIMIT;
    public static final ForgeConfigSpec.IntValue RELIGHT_SCAN_BELOW_TOP;
    public static final ForgeConfigSpec.IntValue RELIGHT_SCAN_ABOVE_TOP;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("gameplay");
        PASS_THROUGH_HIDDEN_BLOCKS = builder
            .comment("Allow entities to pass through season-hidden leaves and hidden snow layers.")
            .define("pass_through_hidden_blocks", true);
        REMOVE_LIGHT_BLOCKING_FROM_HIDDEN_BLOCKS = builder
            .comment("Treat season-hidden leaves (and hidden snow above them) as non-light-blocking to avoid dark ground shadows.")
            .define("remove_light_blocking_from_hidden_blocks", true);
        FORCE_RELIGHT_ON_SEASON_CHANGE = builder
            .comment("Force server relight around players when leafless season toggles to refresh cached lighting.")
            .define("force_relight_on_season_change", true);
        RELIGHT_CHUNK_LIMIT = builder
            .comment("Max chunk count relit around players on season transition.")
            .defineInRange("relight_chunk_limit", 900, 64, 4096);
        RELIGHT_SCAN_BELOW_TOP = builder
            .comment("How many blocks below top heightmap to scan for relight candidates.")
            .defineInRange("relight_scan_below_top", 20, 0, 128);
        RELIGHT_SCAN_ABOVE_TOP = builder
            .comment("How many blocks above top heightmap to scan for relight candidates.")
            .defineInRange("relight_scan_above_top", 4, 0, 64);
        builder.pop();
        SPEC = builder.build();
    }

    private ServerConfig() {
    }
}
