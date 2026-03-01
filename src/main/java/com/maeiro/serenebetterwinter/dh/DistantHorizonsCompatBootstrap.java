package com.maeiro.serenebetterwinter.dh;

import com.maeiro.serenebetterwinter.SereneBetterWinterMod;
import java.lang.reflect.Method;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.fml.ModList;

public final class DistantHorizonsCompatBootstrap {
    private static final String DH_MOD_ID = "distanthorizons";
    private static final String IMPL_CLASS_NAME = "com.maeiro.serenebetterwinter.dh.DistantHorizonsCompatImpl";
    private static volatile boolean initAttempted = false;
    private static volatile boolean active = false;
    private static volatile boolean loadFailureLogged = false;
    private static volatile long retryAfterMs = 0L;
    private static final long RETRY_DELAY_MS = 3000L;
    private static int retryLogCount = 0;
    private static final int RETRY_LOG_LIMIT = 10;
    private static volatile Method onLeaflessSeasonStateChangedMethod;
    private static volatile Method onClientTickMethod;
    private static volatile Method onClientLogoutMethod;

    private DistantHorizonsCompatBootstrap() {
    }

    public static void tryInit() {
        if (active) {
            return;
        }
        long now = System.currentTimeMillis();
        if (retryAfterMs > now) {
            return;
        }
        if (initAttempted) {
            return;
        }
        initAttempted = true;

        if (!ModList.get().isLoaded(DH_MOD_ID)) {
            return;
        }

        try {
            Class<?> impl = Class.forName(IMPL_CLASS_NAME);
            Method initMethod = impl.getMethod("init");
            onLeaflessSeasonStateChangedMethod =
                impl.getMethod("onLeaflessSeasonStateChanged", ClientLevel.class, boolean.class, boolean.class);
            onClientTickMethod = impl.getMethod("onClientTick", ClientLevel.class);
            onClientLogoutMethod = impl.getMethod("onClientLogout");
            initMethod.invoke(null);
            active = true;
            retryAfterMs = 0L;
            SereneBetterWinterMod.LOGGER.info("[{}] Distant Horizons compatibility initialized.", SereneBetterWinterMod.MOD_ID);
        } catch (Throwable t) {
            initAttempted = false;
            retryAfterMs = System.currentTimeMillis() + RETRY_DELAY_MS;
            active = false;
            if (retryLogCount < RETRY_LOG_LIMIT) {
                retryLogCount++;
                loadFailureLogged = true;
                SereneBetterWinterMod.LOGGER.warn("[{}] Failed to initialize Distant Horizons compatibility. Retrying soon.", SereneBetterWinterMod.MOD_ID, t);
            }
        }
    }

    public static void onLeaflessSeasonStateChanged(ClientLevel level, boolean previousState, boolean currentState) {
        invoke(onLeaflessSeasonStateChangedMethod, level, previousState, currentState);
    }

    public static void onClientTick(ClientLevel level) {
        invoke(onClientTickMethod, level);
    }

    public static void onClientLogout() {
        invoke(onClientLogoutMethod);
    }

    private static void invoke(Method method, Object... args) {
        if (!active || method == null) {
            return;
        }

        try {
            method.invoke(null, args);
        } catch (Throwable t) {
            active = false;
            initAttempted = false;
            retryAfterMs = System.currentTimeMillis() + RETRY_DELAY_MS;
            if (retryLogCount < RETRY_LOG_LIMIT) {
                retryLogCount++;
                loadFailureLogged = true;
                SereneBetterWinterMod.LOGGER.warn("[{}] Distant Horizons compatibility hit a runtime failure and will retry.", SereneBetterWinterMod.MOD_ID, t);
            }
        }
    }
}
