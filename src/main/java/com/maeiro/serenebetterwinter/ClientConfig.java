package com.maeiro.serenebetterwinter;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.BooleanValue HIDE_SNOW_ABOVE_HIDDEN_LEAVES;
    public static final ForgeConfigSpec.BooleanValue HIDE_OUTLINE_FOR_HIDDEN_BLOCKS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_EXPERIMENTAL_DH_INTEGRATION;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DH_LOD_LEAF_HIDING;
    public static final ForgeConfigSpec.BooleanValue DH_AUTO_REFRESH_ON_SEASON_TOGGLE;
    public static final ForgeConfigSpec.BooleanValue DH_ENABLE_FULL_DATA_PURGE_RECOVERY;
    public static final ForgeConfigSpec.IntValue DH_REFRESH_RADIUS_CHUNKS;
    public static final ForgeConfigSpec.IntValue DH_REFRESH_CHUNK_CAP;
    public static final ForgeConfigSpec.IntValue DH_REFRESH_DELAY_TICKS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("general");
        ENABLED = builder
            .comment("Enable winter leafless effect.")
            .define("enabled", true);
        HIDE_SNOW_ABOVE_HIDDEN_LEAVES = builder
            .comment("Hide snow layer rendered above leaves hidden by this mod.")
            .define("hide_snow_above_hidden_leaves", true);
        HIDE_OUTLINE_FOR_HIDDEN_BLOCKS = builder
            .comment("Hide block outline when targeting blocks visually hidden by this mod.")
            .define("hide_outline_for_hidden_blocks", true);
        builder.pop();

        builder.push("distant_horizons");
        ENABLE_EXPERIMENTAL_DH_INTEGRATION = builder
            .comment(
                "EXPERIMENTAL: enable Serene Better Winter integration with Distant Horizons.",
                "May cause unexpected LOD behavior on some modpacks/setups.",
                "Disable if you notice stale/flickering/holey distant chunks."
            )
            .define("enable_experimental_dh_integration", false);
        ENABLE_DH_LOD_LEAF_HIDING = builder
            .comment("Enable DH LOD leaf hiding. Requires enable_experimental_dh_integration=true.")
            .define("enable_dh_lod_leaf_hiding", true);
        DH_AUTO_REFRESH_ON_SEASON_TOGGLE = builder
            .comment("Automatically refresh nearby DH LOD chunks when leafless season state changes.")
            .define("dh_auto_refresh_on_season_toggle", true);
        DH_ENABLE_FULL_DATA_PURGE_RECOVERY = builder
            .comment(
                "Advanced experimental fallback: delete nearby DH full-data entries while leaves are returning.",
                "Can help recover stubborn stale LOD trees, but may cause temporary LOD holes on some setups.",
                "Disabled by default for safety."
            )
            .define("dh_enable_full_data_purge_recovery", false);
        DH_REFRESH_RADIUS_CHUNKS = builder
            .comment("Chunk radius around players used for Distant Horizons LOD refresh.")
            .defineInRange("dh_refresh_radius_chunks", 8, 1, 32);
        DH_REFRESH_CHUNK_CAP = builder
            .comment("Maximum chunk count refreshed per season toggle for Distant Horizons integration.")
            .defineInRange("dh_refresh_chunk_cap", 1024, 64, 8192);
        DH_REFRESH_DELAY_TICKS = builder
            .comment("Ticks to wait after season toggle before triggering Distant Horizons LOD refresh.")
            .defineInRange("dh_refresh_delay_ticks", 20, 0, 400);
        builder.pop();

        SPEC = builder.build();
    }

    public static boolean isDhIntegrationEnabled() {
        return ENABLED.get() && ENABLE_EXPERIMENTAL_DH_INTEGRATION.get() && ENABLE_DH_LOD_LEAF_HIDING.get();
    }

    private ClientConfig() {
    }
}
