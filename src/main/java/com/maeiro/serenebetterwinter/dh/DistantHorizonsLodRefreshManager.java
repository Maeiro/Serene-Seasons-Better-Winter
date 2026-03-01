package com.maeiro.serenebetterwinter.dh;

import com.maeiro.serenebetterwinter.ClientConfig;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.multiplayer.ClientLevel;

final class DistantHorizonsLodRefreshManager {
    private static int pendingTicks = -1;
    private static boolean pendingLeaflessState = false;
    private static int retriesLeft = 0;
    private static int flushAttempt = 0;
    private static int postSuccessRoundsLeft = 0;
    private static final int RETRY_INTERVAL_TICKS = 40;
    private static final int SUCCESS_RETRY_INTERVAL_TICKS = 20;
    private static final int MAX_RETRIES = 30;
    private static final int POST_SUCCESS_ROUNDS = 4;
    private static final int RADIUS_STEP_EVERY_ATTEMPTS = 3;
    private static final int RADIUS_STEP_CHUNKS = 2;
    private static final int MAX_RADIUS_BONUS = 16;
    private static final int CAP_STEP_PER_ATTEMPT = 256;
    private static final int MAX_CAP_BONUS = 4096;
    private static final int PURGE_RECOVERY_FIRST_ATTEMPT = 8;
    private static final int PURGE_RECOVERY_EVERY_ATTEMPTS = 4;
    private static final Set<Long> PURGED_SECTION_POSITIONS = new HashSet<>();

    private DistantHorizonsLodRefreshManager() {
    }

    static void onLeaflessSeasonStateChanged(ClientLevel level, boolean previousState, boolean currentState) {
        if (level == null || previousState == currentState) {
            return;
        }
        if (!ClientConfig.isDhIntegrationEnabled()) {
            return;
        }
        if (!ClientConfig.DH_AUTO_REFRESH_ON_SEASON_TOGGLE.get()) {
            return;
        }

        DistantHorizonsCompatImpl.clearRenderDataCacheForSeasonChange(currentState);

        pendingLeaflessState = currentState;
        retriesLeft = MAX_RETRIES;
        flushAttempt = 0;
        postSuccessRoundsLeft = 0;
        PURGED_SECTION_POSITIONS.clear();
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
        if (!ClientConfig.isDhIntegrationEnabled()) {
            clear();
            return;
        }
        if (!ClientConfig.DH_AUTO_REFRESH_ON_SEASON_TOGGLE.get()) {
            clear();
            return;
        }

        int baseRadius = Math.max(1, ClientConfig.DH_REFRESH_RADIUS_CHUNKS.get());
        int baseCap = Math.max(64, ClientConfig.DH_REFRESH_CHUNK_CAP.get());
        int radiusBonus = Math.min((flushAttempt / RADIUS_STEP_EVERY_ATTEMPTS) * RADIUS_STEP_CHUNKS, MAX_RADIUS_BONUS);
        int capBonus = Math.min(flushAttempt * CAP_STEP_PER_ATTEMPT, MAX_CAP_BONUS);
        int radius = baseRadius + radiusBonus;
        int cap = baseCap + capBonus;
        int purged = 0;

        boolean purgeRecoveryEnabled = ClientConfig.DH_ENABLE_FULL_DATA_PURGE_RECOVERY.get();
        boolean shouldTryPurgeRecovery = !pendingLeaflessState
            && purgeRecoveryEnabled
            && flushAttempt >= PURGE_RECOVERY_FIRST_ATTEMPT
            && (flushAttempt - PURGE_RECOVERY_FIRST_ATTEMPT) % PURGE_RECOVERY_EVERY_ATTEMPTS == 0;

        if (shouldTryPurgeRecovery) {
            purged = DistantHorizonsCompatImpl.requestFullDataPurgeAroundPlayers(
                level,
                pendingLeaflessState,
                radius,
                cap,
                flushAttempt,
                PURGED_SECTION_POSITIONS
            );
        }

        int refreshed = DistantHorizonsCompatImpl.requestLodRefreshAroundPlayers(
            level,
            pendingLeaflessState,
            radius,
            cap,
            flushAttempt
        );
        int retrievalQueued = DistantHorizonsCompatImpl.requestFullDataRetrievalAroundPlayers(
            level,
            pendingLeaflessState,
            radius,
            cap,
            flushAttempt
        );
        flushAttempt++;

        if (purged > 0 || refreshed > 0 || retrievalQueued > 0) {
            postSuccessRoundsLeft = POST_SUCCESS_ROUNDS;
        }

        if (retriesLeft > 0) {
            retriesLeft--;

            if (postSuccessRoundsLeft > 0) {
                postSuccessRoundsLeft--;
                pendingTicks = SUCCESS_RETRY_INTERVAL_TICKS;
            } else {
                pendingTicks = RETRY_INTERVAL_TICKS;
            }
            return;
        }

        clear();
    }

    private static void clear() {
        pendingTicks = -1;
        pendingLeaflessState = false;
        retriesLeft = 0;
        flushAttempt = 0;
        postSuccessRoundsLeft = 0;
        PURGED_SECTION_POSITIONS.clear();
    }
}
