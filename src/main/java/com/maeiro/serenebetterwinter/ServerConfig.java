package com.maeiro.serenebetterwinter;

import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;

public final class ServerConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue PASS_THROUGH_HIDDEN_BLOCKS;
    public static final ForgeConfigSpec.BooleanValue REMOVE_LIGHT_BLOCKING_FROM_HIDDEN_BLOCKS;
    public static final ForgeConfigSpec.BooleanValue FORCE_RELIGHT_ON_SEASON_CHANGE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> LEAF_DROP_SUBSEASONS;
    public static final ForgeConfigSpec.BooleanValue BROADCAST_LEAF_DROP_SEASON_MESSAGE;
    public static final ForgeConfigSpec.ConfigValue<String> LEAF_DROP_SEASON_MESSAGE;
    public static final ForgeConfigSpec.BooleanValue BROADCAST_LEAF_RETURN_SEASON_MESSAGE;
    public static final ForgeConfigSpec.ConfigValue<String> LEAF_RETURN_SEASON_MESSAGE;

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
        LEAF_DROP_SUBSEASONS = builder
            .comment("Sub-seasons that activate the leaf-drop effect.")
            .defineListAllowEmpty(
                List.of("leaf_drop_subseasons"),
                List.of("LATE_AUTUMN", "EARLY_WINTER", "MID_WINTER", "LATE_WINTER"),
                value -> value instanceof String
            );
        BROADCAST_LEAF_DROP_SEASON_MESSAGE = builder
            .comment("Broadcast a chat message when entering a configured leaf-drop sub-season.")
            .define("broadcast_leaf_drop_season_message", true);
        LEAF_DROP_SEASON_MESSAGE = builder
            .comment("Message broadcast when entering a leaf-drop sub-season. Placeholders: %subseason%, %dimension%")
            .define("leaf_drop_season_message", "🌲🍁🍂 The air grows colder… leaves begin to fall. Winter is coming.");
        BROADCAST_LEAF_RETURN_SEASON_MESSAGE = builder
            .comment("Broadcast a chat message when leaving configured leaf-drop sub-seasons.")
            .define("broadcast_leaf_return_season_message", true);
        LEAF_RETURN_SEASON_MESSAGE = builder
            .comment("Message broadcast when leaves return. Placeholders: %subseason%, %dimension%")
            .define("leaf_return_season_message", "🌱🌳 The air turns warmer... leaves begin to grow back.");
        builder.pop();
        SPEC = builder.build();
    }

    private ServerConfig() {
    }
}
