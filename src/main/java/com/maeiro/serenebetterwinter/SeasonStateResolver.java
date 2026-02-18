package com.maeiro.serenebetterwinter;

import java.lang.reflect.Method;
import java.util.Collections;
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
        if (level == null || !ModList.get().isLoaded(SERENE_MOD_ID)) {
            return false;
        }

        TickState cached = CACHE_BY_LEVEL.get(level);
        long gameTime = level.getGameTime();
        if (cached != null && cached.gameTime == gameTime) {
            return cached.leafless;
        }

        try {
            initializeReflection();
            if (getSeasonStateMethod == null) {
                return false;
            }

            Object seasonState = getSeasonStateMethod.invoke(null, level);
            if (seasonState == null) {
                return false;
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
                return false;
            }

            String name = subSeason.toString().toUpperCase(java.util.Locale.ROOT);
            boolean leafless = name.contains("LATE_AUTUMN") || name.contains("WINTER");
            CACHE_BY_LEVEL.put(level, new TickState(gameTime, leafless));
            return leafless;
        } catch (Exception ex) {
            if (!reflectionErrorLogged) {
                reflectionErrorLogged = true;
                SereneBetterWinterMod.LOGGER.warn("[{}] Could not resolve Serene Seasons season state via reflection.", SereneBetterWinterMod.MOD_ID, ex);
            }
            return false;
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

    private record TickState(long gameTime, boolean leafless) {
    }
}
