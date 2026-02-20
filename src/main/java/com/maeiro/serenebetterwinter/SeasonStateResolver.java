package com.maeiro.serenebetterwinter;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;

public final class SeasonStateResolver {
    private static final String SERENE_MOD_ID = "sereneseasons";
    private static volatile boolean reflectionErrorLogged = false;
    private static volatile boolean reflectionInitAttempted = false;
    private static volatile Method getSeasonStateMethod;
    private static volatile Method getSubSeasonMethod;
    private static final Map<Level, TickState> CACHE_BY_LEVEL = Collections.synchronizedMap(new WeakHashMap<>());

    private SeasonStateResolver() {
    }

    public static boolean isLeaflessSeason(Level level) {
        return LeafDropSeasonRules.isConfiguredLeafDropSubSeason(getCurrentSubSeasonId(level));
    }

    public static String getCurrentSubSeasonId(Level level) {
        if (level == null || !ModList.get().isLoaded(SERENE_MOD_ID)) {
            return null;
        }

        TickState cached = CACHE_BY_LEVEL.get(level);
        long gameTime = level.getGameTime();
        if (cached != null && cached.gameTime == gameTime) {
            return cached.subSeasonId;
        }

        try {
            initializeReflection();
            if (getSeasonStateMethod == null) {
                return null;
            }

            Object seasonState = getSeasonStateMethod.invoke(null, level);
            if (seasonState == null) {
                return null;
            }

            if (getSubSeasonMethod == null) {
                synchronized (SeasonStateResolver.class) {
                    if (getSubSeasonMethod == null) {
                        getSubSeasonMethod = seasonState.getClass().getMethod("getSubSeason");
                    }
                }
            }

            Object subSeason = getSubSeasonMethod.invoke(seasonState);
            if (subSeason == null) {
                return null;
            }

            String subSeasonId = subSeason.toString().trim().toUpperCase(Locale.ROOT);
            CACHE_BY_LEVEL.put(level, new TickState(gameTime, subSeasonId));
            return subSeasonId;
        } catch (Exception ex) {
            if (!reflectionErrorLogged) {
                reflectionErrorLogged = true;
                SereneBetterWinterMod.LOGGER.warn("[{}] Could not resolve Serene Seasons season state via reflection.", SereneBetterWinterMod.MOD_ID, ex);
            }
            return null;
        }
    }

    private static void initializeReflection() throws Exception {
        if (reflectionInitAttempted) {
            return;
        }

        synchronized (SeasonStateResolver.class) {
            if (reflectionInitAttempted) {
                return;
            }
            reflectionInitAttempted = true;

            Class<?> helper = Class.forName("sereneseasons.api.season.SeasonHelper");
            getSeasonStateMethod = helper.getMethod("getSeasonState", Level.class);

        }
    }

    private record TickState(long gameTime, String subSeasonId) {
    }
}
