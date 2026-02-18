package com.maeiro.serenebetterwinter;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(SereneBetterWinterMod.MOD_ID)
public final class SereneBetterWinterMod {
    public static final String MOD_ID = "serene_better_winter";
    public static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings("removal")
    public SereneBetterWinterMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        LOGGER.info("[{}] Loaded. Client config enabled default={}", MOD_ID, ClientConfig.ENABLED.getDefault());
    }
}
