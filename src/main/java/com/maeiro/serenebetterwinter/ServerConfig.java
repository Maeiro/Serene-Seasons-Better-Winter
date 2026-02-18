package com.maeiro.serenebetterwinter;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ServerConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue PASS_THROUGH_HIDDEN_BLOCKS;
    public static final ForgeConfigSpec.BooleanValue REMOVE_LIGHT_BLOCKING_FROM_HIDDEN_BLOCKS;
    public static final ForgeConfigSpec.BooleanValue FORCE_RELIGHT_ON_SEASON_CHANGE;

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
        builder.pop();
        SPEC = builder.build();
    }

    private ServerConfig() {
    }
}
