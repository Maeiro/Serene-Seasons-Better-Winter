package com.maeiro.serenebetterwinter;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ServerConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue PASS_THROUGH_HIDDEN_BLOCKS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("gameplay");
        PASS_THROUGH_HIDDEN_BLOCKS = builder
            .comment("Allow entities to pass through season-hidden leaves and hidden snow layers.")
            .define("pass_through_hidden_blocks", true);
        builder.pop();
        SPEC = builder.build();
    }

    private ServerConfig() {
    }
}
