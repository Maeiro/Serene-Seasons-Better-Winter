package com.maeiro.serenebetterwinter.dh;

import com.maeiro.serenebetterwinter.ClientConfig;
import net.minecraft.client.multiplayer.ClientLevel;

final class DistantHorizonsLodRefreshManager {
    private static int pendingTicks = -1;
    private static boolean pendingLeaflessState = false;
    private static int retriesLeft = 0;
    private static final int RETRY_INTERVAL_TICKS = 40;
    private static final int MAX_RETRIES = 30;

    private DistantHorizonsLodRefreshManager() {
    }

    static void onLeaflessSeasonStateChanged(ClientLevel level, boolean previousState, boolean currentState) {
        if (level == null || previousState == currentState) {
            return;
        }
        if (!ClientConfig.ENABLED.get() || !ClientConfig.ENABLE_DH_LOD_LEAF_HIDING.get()) {
            return;
        }
        if (!ClientConfig.DH_AUTO_REFRESH_ON_SEASON_TOGGLE.get()) {
            return;
        }

        DistantHorizonsCompatImpl.clearRenderDataCacheForSeasonChange(currentState);

        pendingLeaflessState = currentState;
        retriesLeft = MAX_RETRIES;
        pendingTicks = Math.max(0, ClientConfig.DH_REFRESH_DELAY_TICKS.get());
        if (pendingTicks == 0) {
            flush(level);
        }
    }

    static void onClientTick(ClientLevel level) {
        if (level == null) {
            clear();
            return;
        }
        if (pendingTicks < 0) {
            return;
        }
        if (pendingTicks > 0) {
            pendingTicks--;
            return;
        }

        flush(level);
    }

    static void onClientLogout() {
        clear();
    }

    private static void flush(ClientLevel level) {
        if (!ClientConfig.ENABLED.get() || !ClientConfig.ENABLE_DH_LOD_LEAF_HIDING.get()) {
            clear();
            return;
        }
        if (!ClientConfig.DH_AUTO_REFRESH_ON_SEASON_TOGGLE.get()) {
            clear();
            return;
        }

        int refreshed = DistantHorizonsCompatImpl.requestLodRefreshAroundPlayers(level, pendingLeaflessState);
        if (refreshed > 0) {
            clear();
            return;
        }

        if (retriesLeft > 0) {
            retriesLeft--;
            pendingTicks = RETRY_INTERVAL_TICKS;
            return;
        }

        clear();
    }

    private static void clear() {
        pendingTicks = -1;
        pendingLeaflessState = false;
        retriesLeft = 0;
    }
}
