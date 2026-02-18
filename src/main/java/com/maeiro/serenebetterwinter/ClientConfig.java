package com.maeiro.serenebetterwinter;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.BooleanValue HIDE_SNOW_ABOVE_HIDDEN_LEAVES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("general");
        ENABLED = builder
            .comment("Enable winter leafless effect.")
            .define("enabled", true);
        HIDE_SNOW_ABOVE_HIDDEN_LEAVES = builder
            .comment("Hide snow layer rendered above leaves hidden by this mod.")
            .define("hide_snow_above_hidden_leaves", true);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientConfig() {
    }
}
