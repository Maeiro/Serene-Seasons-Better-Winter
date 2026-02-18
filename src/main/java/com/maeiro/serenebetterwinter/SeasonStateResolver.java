package com.maeiro.serenebetterwinter;

import java.lang.reflect.Method;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;

public final class SeasonStateResolver {
    private static final String SERENE_MOD_ID = "sereneseasons";
    private static volatile boolean reflectionErrorLogged = false;

    private SeasonStateResolver() {
    }

    public static boolean isLeaflessSeason(Level level) {
        if (level == null || !ModList.get().isLoaded(SERENE_MOD_ID)) {
            return false;
        }

        try {
            Class<?> helper = Class.forName("sereneseasons.api.season.SeasonHelper");
            Method getSeasonState = helper.getMethod("getSeasonState", Level.class);
            Object seasonState = getSeasonState.invoke(null, level);
            if (seasonState == null) {
                return false;
            }

            Method getSubSeason = seasonState.getClass().getMethod("getSubSeason");
            Object subSeason = getSubSeason.invoke(seasonState);
            if (subSeason == null) {
                return false;
            }

            String name = subSeason.toString().toUpperCase(java.util.Locale.ROOT);
            return name.contains("LATE_AUTUMN") || name.contains("WINTER");
        } catch (Exception ex) {
            if (!reflectionErrorLogged) {
                reflectionErrorLogged = true;
                SereneBetterWinterMod.LOGGER.warn("[{}] Could not resolve Serene Seasons season state via reflection.", SereneBetterWinterMod.MOD_ID, ex);
            }
            return false;
        }
    }
}
