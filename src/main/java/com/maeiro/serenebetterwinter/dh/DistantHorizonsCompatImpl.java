package com.maeiro.serenebetterwinter.dh;

import com.maeiro.serenebetterwinter.ClientConfig;
import com.maeiro.serenebetterwinter.ClientSeasonTracker;
import com.maeiro.serenebetterwinter.LeafTargeting;
import com.maeiro.serenebetterwinter.SereneBetterWinterMod;
import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBlockStateWrapper;
import com.seibel.distanthorizons.api.methods.events.DhApiEventRegister;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiChunkProcessingEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import com.seibel.distanthorizons.api.objects.DhApiResult;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;

public final class DistantHorizonsCompatImpl {
    private static final LeafChunkProcessingEvent CHUNK_PROCESSING_EVENT = new LeafChunkProcessingEvent();
    private static final boolean ENABLE_CHUNK_PROCESSING_OVERRIDE_FALLBACK = Boolean.getBoolean(
        "serene_better_winter.dh.chunk_processing_fallback"
    );
    private static volatile boolean initialized = false;
    private static volatile boolean compatReady = false;
    private static volatile boolean chunkProcessingFallbackRegistered = false;
    private static volatile boolean registrationFailureLogged = false;
    private static volatile boolean refreshFailureLogged = false;
    private static volatile IDhApiBlockStateWrapper cachedAirBlockWrapper;
    private static int refreshLogCount = 0;
    private static final int REFRESH_LOG_LIMIT = 20;
    private static int retrievalLogCount = 0;
    private static final int RETRIEVAL_LOG_LIMIT = 20;
    private static int purgeLogCount = 0;
    private static final int PURGE_LOG_LIMIT = 20;
    private static int globalPurgeLogCount = 0;
    private static final int GLOBAL_PURGE_LOG_LIMIT = 20;
    private static int renderCacheClearLogCount = 0;
    private static final int RENDER_CACHE_CLEAR_LOG_LIMIT = 10;
    private static int processingDebugCount = 0;
    private static int overrideDebugCount = 0;
    private static int nonBlockStateDebugCount = 0;
    private static final int PROCESSING_DEBUG_LIMIT = 80;
    private static final int OVERRIDE_DEBUG_LIMIT = 80;
    private static final int NON_BLOCKSTATE_DEBUG_LIMIT = 20;

    private DistantHorizonsCompatImpl() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        compatReady = true;

        if (ENABLE_CHUNK_PROCESSING_OVERRIDE_FALLBACK) {
            registerChunkProcessingHandler();
        } else {
            SereneBetterWinterMod.LOGGER.info(
                "[{}] Using Distant Horizons full-data transformer path (chunk-processing override fallback disabled).",
                SereneBetterWinterMod.MOD_ID
            );
        }
    }

    public static void onLeaflessSeasonStateChanged(ClientLevel level, boolean previousState, boolean currentState) {
        if (!compatReady) {
            return;
        }
        DistantHorizonsLodRefreshManager.onLeaflessSeasonStateChanged(level, previousState, currentState);
    }

    public static void onClientTick(ClientLevel level) {
        if (!compatReady) {
            return;
        }
        DistantHorizonsLodRefreshManager.onClientTick(level);
    }

    public static void onClientLogout() {
        DistantHorizonsLodRefreshManager.onClientLogout();
    }

    static void clearRenderDataCacheForSeasonChange(boolean leaflessSeasonActive) {
        if (!compatReady) {
            return;
        }
        if (!ClientConfig.isDhIntegrationEnabled()) {
            return;
        }

        try {
            if (DhApi.Delayed.renderProxy == null) {
                return;
            }

            DhApiResult<Boolean> result = DhApi.Delayed.renderProxy.clearRenderDataCache();
            if (renderCacheClearLogCount < RENDER_CACHE_CLEAR_LOG_LIMIT) {
                renderCacheClearLogCount++;
                if (result != null && result.success) {
                    SereneBetterWinterMod.LOGGER.info(
                        "[{}] Cleared Distant Horizons render cache after leafless season toggle (active={}).",
                        SereneBetterWinterMod.MOD_ID,
                        leaflessSeasonActive
                    );
                } else {
                    String message = (result == null || result.message == null) ? "Unknown clearRenderDataCache result." : result.message;
                    SereneBetterWinterMod.LOGGER.info(
                        "[{}] Distant Horizons render cache clear returned non-success (active={}, message={}).",
                        SereneBetterWinterMod.MOD_ID,
                        leaflessSeasonActive,
                        message
                    );
                }
            }
        } catch (Throwable t) {
            if (!refreshFailureLogged) {
                refreshFailureLogged = true;
                SereneBetterWinterMod.LOGGER.warn(
                    "[{}] Failed to clear Distant Horizons render cache after season toggle.",
                    SereneBetterWinterMod.MOD_ID,
                    t
                );
            }
        }
    }

    static int requestLodRefreshAroundPlayers(
        ClientLevel level,
        boolean leaflessSeasonActive,
        int radiusOverride,
        int capOverride,
        int attempt
    ) {
        if (!compatReady || level == null) {
            return 0;
        }
        if (!ClientConfig.isDhIntegrationEnabled()) {
            return 0;
        }
        if (!ClientConfig.DH_AUTO_REFRESH_ON_SEASON_TOGGLE.get()) {
            return 0;
        }

        int radius = Math.max(1, radiusOverride > 0 ? radiusOverride : ClientConfig.DH_REFRESH_RADIUS_CHUNKS.get());
        int cap = Math.max(64, capOverride > 0 ? capOverride : ClientConfig.DH_REFRESH_CHUNK_CAP.get());
        Set<Long> targetChunks = collectTargetChunks(level, radius, cap);
        int enqueued = enqueueChunkRefresh(level, targetChunks, cap);

        if (refreshLogCount < REFRESH_LOG_LIMIT) {
            refreshLogCount++;
            if (enqueued > 0) {
                SereneBetterWinterMod.LOGGER.info(
                    "[{}] Requested Distant Horizons LOD refresh for {} chunk(s) after leafless state change (active={}, attempt={}, radius={}, cap={}).",
                    SereneBetterWinterMod.MOD_ID,
                    enqueued,
                    leaflessSeasonActive,
                    attempt,
                    radius,
                    cap
                );
            } else {
                SereneBetterWinterMod.LOGGER.info(
                    "[{}] Distant Horizons LOD refresh enqueued 0 chunks (active={}, attempt={}, radius={}, cap={}, targets={}, dhWorldReady={}).",
                    SereneBetterWinterMod.MOD_ID,
                    leaflessSeasonActive,
                    attempt,
                    radius,
                    cap,
                    targetChunks.size(),
                    InternalRefreshHooks.isDhWorldReady()
                );
            }
        }

        return enqueued;
    }

    static int requestFullDataRetrievalAroundPlayers(
        ClientLevel level,
        boolean leaflessSeasonActive,
        int radiusOverride,
        int capOverride,
        int attempt
    ) {
        if (!compatReady || level == null) {
            return 0;
        }
        if (!ClientConfig.isDhIntegrationEnabled()) {
            return 0;
        }
        if (!ClientConfig.DH_AUTO_REFRESH_ON_SEASON_TOGGLE.get()) {
            return 0;
        }

        int radius = Math.max(1, radiusOverride > 0 ? radiusOverride : ClientConfig.DH_REFRESH_RADIUS_CHUNKS.get());
        int cap = Math.max(64, capOverride > 0 ? capOverride : ClientConfig.DH_REFRESH_CHUNK_CAP.get());
        Set<Long> targetChunks = collectTargetChunks(level, radius, cap);
        int queued = InternalRefreshHooks.enqueueFullDataRetrieval(level, targetChunks, cap);

        if (retrievalLogCount < RETRIEVAL_LOG_LIMIT) {
            retrievalLogCount++;
            SereneBetterWinterMod.LOGGER.info(
                "[{}] Requested Distant Horizons full-data retrieval for {} chunk(s) (active={}, attempt={}, radius={}, cap={}, targets={}).",
                SereneBetterWinterMod.MOD_ID,
                queued,
                leaflessSeasonActive,
                attempt,
                radius,
                cap,
                targetChunks.size()
            );
        }

        return queued;
    }

    static int requestFullDataPurgeAroundPlayers(
        ClientLevel level,
        boolean leaflessSeasonActive,
        int radiusOverride,
        int capOverride,
        int attempt,
        Set<Long> alreadyPurgedSectionPositions
    ) {
        if (!compatReady || level == null) {
            return 0;
        }
        if (!ClientConfig.isDhIntegrationEnabled()) {
            return 0;
        }
        if (!ClientConfig.DH_AUTO_REFRESH_ON_SEASON_TOGGLE.get()) {
            return 0;
        }

        int radius = Math.max(1, radiusOverride > 0 ? radiusOverride : ClientConfig.DH_REFRESH_RADIUS_CHUNKS.get());
        int cap = Math.max(64, capOverride > 0 ? capOverride : ClientConfig.DH_REFRESH_CHUNK_CAP.get());
        Set<Long> targetChunks = collectTargetChunks(level, radius, cap);
        int purgeCap = Math.max(cap * 12, 1024);
        int purged = InternalRefreshHooks.purgeFullDataForChunks(level, targetChunks, purgeCap, alreadyPurgedSectionPositions);

        if (purgeLogCount < PURGE_LOG_LIMIT) {
            purgeLogCount++;
            SereneBetterWinterMod.LOGGER.info(
                "[{}] Requested Distant Horizons full-data purge for {} section entry(s) (active={}, attempt={}, radius={}, cap={}, targets={}).",
                SereneBetterWinterMod.MOD_ID,
                purged,
                leaflessSeasonActive,
                attempt,
                radius,
                cap,
                targetChunks.size()
            );
        }

        return purged;
    }

    static int requestGlobalFullDataPurge(
        ClientLevel level,
        boolean leaflessSeasonActive,
        int attempt,
        int maxDeletes,
        Set<Long> alreadyPurgedSectionPositions
    ) {
        if (!compatReady || level == null) {
            return 0;
        }
        if (!ClientConfig.isDhIntegrationEnabled()) {
            return 0;
        }
        if (!ClientConfig.DH_AUTO_REFRESH_ON_SEASON_TOGGLE.get()) {
            return 0;
        }

        int clampedDeletes = Math.max(1024, maxDeletes);
        int purged = InternalRefreshHooks.purgeAllFullData(level, clampedDeletes, alreadyPurgedSectionPositions);

        if (globalPurgeLogCount < GLOBAL_PURGE_LOG_LIMIT) {
            globalPurgeLogCount++;
            SereneBetterWinterMod.LOGGER.info(
                "[{}] Requested Distant Horizons GLOBAL full-data purge for {} section entry(s) (active={}, attempt={}, maxDeletes={}).",
                SereneBetterWinterMod.MOD_ID,
                purged,
                leaflessSeasonActive,
                attempt,
                clampedDeletes
            );
        }

        return purged;
    }

    private static void registerChunkProcessingHandler() {
        try {
            DhApiResult<Void> result = DhApiEventRegister.on(DhApiChunkProcessingEvent.class, CHUNK_PROCESSING_EVENT);
            if (result != null && result.success) {
                chunkProcessingFallbackRegistered = true;
                SereneBetterWinterMod.LOGGER.info(
                    "[{}] Registered Distant Horizons chunk processing handler (legacy fallback path).",
                    SereneBetterWinterMod.MOD_ID
                );
                return;
            }

            if (!registrationFailureLogged) {
                registrationFailureLogged = true;
                String message = (result == null || result.message == null) ? "Unknown registration error." : result.message;
                SereneBetterWinterMod.LOGGER.warn("[{}] Failed to register Distant Horizons chunk handler: {}", SereneBetterWinterMod.MOD_ID, message);
            }
        } catch (Throwable t) {
            if (!registrationFailureLogged) {
                registrationFailureLogged = true;
                SereneBetterWinterMod.LOGGER.warn("[{}] Error while registering Distant Horizons chunk handler.", SereneBetterWinterMod.MOD_ID, t);
            }
        }
    }

    private static IDhApiBlockStateWrapper getAirBlockWrapper() {
        IDhApiBlockStateWrapper cached = cachedAirBlockWrapper;
        if (cached != null) {
            return cached;
        }
        try {
            if (DhApi.Delayed.wrapperFactory == null) {
                return null;
            }
            cached = DhApi.Delayed.wrapperFactory.getAirBlockStateWrapper();
            if (cached != null) {
                cachedAirBlockWrapper = cached;
            }
            return cached;
        } catch (Throwable t) {
            if (!registrationFailureLogged) {
                registrationFailureLogged = true;
                SereneBetterWinterMod.LOGGER.warn("[{}] Failed to obtain Distant Horizons air block wrapper.", SereneBetterWinterMod.MOD_ID, t);
            }
            return null;
        }
    }

    private static Set<Long> collectTargetChunks(ClientLevel level, int radius, int cap) {
        Set<Long> targets = new LinkedHashSet<>();
        List<? extends Player> players = level.players();
        if (players.isEmpty()) {
            Player localPlayer = Minecraft.getInstance().player;
            if (localPlayer != null) {
                addChunksAroundPlayer(localPlayer, radius, cap, targets);
            }
            return targets;
        }

        for (Player player : players) {
            addChunksAroundPlayer(player, radius, cap, targets);
            if (targets.size() >= cap) {
                break;
            }
        }
        return targets;
    }

    private static void addChunksAroundPlayer(Player player, int radius, int cap, Set<Long> targets) {
        int centerX = SectionPos.blockToSectionCoord(player.getBlockX());
        int centerZ = SectionPos.blockToSectionCoord(player.getBlockZ());
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                targets.add(ChunkPos.asLong(centerX + dx, centerZ + dz));
                if (targets.size() >= cap) {
                    return;
                }
            }
        }
    }

    private static int enqueueChunkRefresh(ClientLevel level, Set<Long> chunkKeys, int cap) {
        int refreshed = 0;
        for (long chunkKey : chunkKeys) {
            if (refreshed >= cap) {
                break;
            }

            int chunkX = ChunkPos.getX(chunkKey);
            int chunkZ = ChunkPos.getZ(chunkKey);
            ChunkAccess chunkAccess = level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
            if (!(chunkAccess instanceof LevelChunk levelChunk)) {
                continue;
            }

            if (InternalRefreshHooks.enqueueChunk(level, levelChunk)) {
                refreshed++;
            }
        }
        return refreshed;
    }

    private static final class LeafChunkProcessingEvent extends DhApiChunkProcessingEvent {
        @Override
        public void blockOrBiomeChangedDuringChunkProcessing(DhApiEventParam<EventParam> event) {
            if (!ENABLE_CHUNK_PROCESSING_OVERRIDE_FALLBACK || !chunkProcessingFallbackRegistered) {
                return;
            }
            if (event == null || event.value == null) {
                return;
            }
            if (!ClientConfig.isDhIntegrationEnabled()) {
                return;
            }
            if (!ClientSeasonTracker.isLeaflessSeasonActive()) {
                return;
            }

            EventParam param = event.value;
            if (processingDebugCount < PROCESSING_DEBUG_LIMIT) {
                processingDebugCount++;
                SereneBetterWinterMod.LOGGER.info(
                    "[{}] DH processing event chunk=({}, {}), local=({}, {}, {}).",
                    SereneBetterWinterMod.MOD_ID,
                    param.chunkX,
                    param.chunkZ,
                    param.relativeBlockPosX,
                    param.blockPosY,
                    param.relativeBlockPosZ
                );
            }
            if (param.currentBlock == null) {
                return;
            }

            Object wrappedObject = param.currentBlock.getWrappedMcObject();
            if (!(wrappedObject instanceof BlockState blockState)) {
                if (nonBlockStateDebugCount < NON_BLOCKSTATE_DEBUG_LIMIT) {
                    nonBlockStateDebugCount++;
                    SereneBetterWinterMod.LOGGER.info(
                        "[{}] DH processing currentBlock is not BlockState: {}",
                        SereneBetterWinterMod.MOD_ID,
                        wrappedObject == null ? "null" : wrappedObject.getClass().getName()
                    );
                }
                return;
            }
            if (!LeafTargeting.shouldHide(blockState)) {
                return;
            }

            IDhApiBlockStateWrapper air = getAirBlockWrapper();
            if (air != null) {
                param.setBlockOverride(air);
                if (overrideDebugCount < OVERRIDE_DEBUG_LIMIT) {
                    overrideDebugCount++;
                    SereneBetterWinterMod.LOGGER.info(
                        "[{}] DH leaf override applied for block {} at chunk=({}, {}) local=({}, {}, {}).",
                        SereneBetterWinterMod.MOD_ID,
                        blockState.getBlock(),
                        param.chunkX,
                        param.chunkZ,
                        param.relativeBlockPosX,
                        param.blockPosY,
                        param.relativeBlockPosZ
                    );
                }
            }
        }
    }

    private static final class InternalRefreshHooks {
        private static final String CLIENT_LEVEL_WRAPPER_CLASS =
            "loaderCommon.forge.com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper";
        private static final String CHUNK_WRAPPER_CLASS =
            "loaderCommon.forge.com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper";
        private static final String SHARED_API_CLASS =
            "com.seibel.distanthorizons.core.api.internal.SharedApi";
        private static volatile boolean initAttempted = false;
        private static volatile boolean available = false;
        private static volatile Method clientLevelWrapperGetWrapperMethod;
        private static volatile Constructor<?> chunkWrapperConstructor;
        private static volatile Field sharedApiInstanceField;
        private static volatile Method sharedApiChunkLoadEventMethod;
        private static volatile Method sharedApiGetAbstractDhWorldMethod;
        private static volatile Method sharedApiTryGetDhClientWorldMethod;
        private static volatile Method dhWorldGetOrLoadLevelMethod;
        private static volatile Method dhLevelGetFullDataProviderMethod;
        private static volatile Method fullDataProviderQueuePositionForRetrievalMethod;
        private static volatile Method fullDataProviderCanQueueRetrievalNowMethod;
        private static volatile Field fullDataProviderRepoField;
        private static volatile Method repoDeleteWithKeyMethod;
        private static volatile Method repoGetAllPositionsMethod;
        private static volatile Method longArraySizeMethod;
        private static volatile Method longArrayGetLongMethod;
        private static volatile Constructor<?> dhChunkPosConstructor;
        private static volatile Method dhSectionPosEncodeContainingMethod;
        private static volatile Method dhSectionPosGetParentPosMethod;
        private static volatile byte dhSectionChunkDetailLevel;

        private InternalRefreshHooks() {
        }

        static boolean enqueueChunk(ClientLevel level, LevelChunk chunk) {
            if (!initialize()) {
                return false;
            }

            try {
                Object levelWrapper = clientLevelWrapperGetWrapperMethod.invoke(null, level);
                if (levelWrapper == null) {
                    return false;
                }

                Object chunkWrapper = chunkWrapperConstructor.newInstance(chunk, levelWrapper);
                Object sharedApiInstance = sharedApiInstanceField.get(null);
                if (sharedApiInstance == null) {
                    return false;
                }

                sharedApiChunkLoadEventMethod.invoke(sharedApiInstance, chunkWrapper, levelWrapper);
                return true;
            } catch (Throwable t) {
                if (!refreshFailureLogged) {
                    refreshFailureLogged = true;
                    SereneBetterWinterMod.LOGGER.warn(
                        "[{}] Failed to request Distant Horizons LOD chunk refresh via internal hooks.",
                        SereneBetterWinterMod.MOD_ID,
                        t
                    );
                }
                return false;
            }
        }

        static boolean isDhWorldReady() {
            if (!initialize()) {
                return false;
            }
            try {
                Object world = sharedApiGetAbstractDhWorldMethod.invoke(null);
                return world != null;
            } catch (Throwable ignored) {
                return false;
            }
        }

        static int enqueueFullDataRetrieval(ClientLevel level, Set<Long> chunkKeys, int cap) {
            if (!initialize() || chunkKeys == null || chunkKeys.isEmpty()) {
                return 0;
            }

            try {
                Object levelWrapper = clientLevelWrapperGetWrapperMethod.invoke(null, level);
                if (levelWrapper == null) {
                    return 0;
                }

                Object dhClientWorld = sharedApiTryGetDhClientWorldMethod.invoke(null);
                if (dhClientWorld == null) {
                    return 0;
                }

                Object dhLevel = dhWorldGetOrLoadLevelMethod.invoke(dhClientWorld, levelWrapper);
                if (dhLevel == null) {
                    return 0;
                }

                Object fullDataProvider = dhLevelGetFullDataProviderMethod.invoke(dhLevel);
                if (fullDataProvider == null) {
                    return 0;
                }

                int queued = 0;
                for (long chunkKey : chunkKeys) {
                    if (queued >= cap) {
                        break;
                    }

                    if (fullDataProviderCanQueueRetrievalNowMethod != null) {
                        Object canQueueObj = fullDataProviderCanQueueRetrievalNowMethod.invoke(fullDataProvider);
                        if (canQueueObj instanceof Boolean canQueueBool && !canQueueBool) {
                            break;
                        }
                    }

                    int chunkX = ChunkPos.getX(chunkKey);
                    int chunkZ = ChunkPos.getZ(chunkKey);
                    Object dhChunkPos = dhChunkPosConstructor.newInstance(chunkX, chunkZ);
                    Long sectionPos = (Long) dhSectionPosEncodeContainingMethod.invoke(
                        null,
                        dhSectionChunkDetailLevel,
                        dhChunkPos
                    );
                    Object queuedFuture = fullDataProviderQueuePositionForRetrievalMethod.invoke(fullDataProvider, sectionPos);
                    if (queuedFuture != null) {
                        queued++;
                    }
                }

                return queued;
            } catch (Throwable t) {
                if (!refreshFailureLogged) {
                    refreshFailureLogged = true;
                    SereneBetterWinterMod.LOGGER.warn(
                        "[{}] Failed to request Distant Horizons full-data retrieval via internal hooks.",
                        SereneBetterWinterMod.MOD_ID,
                        t
                    );
                }
                return 0;
            }
        }

        static int purgeFullDataForChunks(
            ClientLevel level,
            Set<Long> chunkKeys,
            int maxDeletes,
            Set<Long> alreadyPurgedSectionPositions
        ) {
            if (!initialize() || chunkKeys == null || chunkKeys.isEmpty()) {
                return 0;
            }

            try {
                Object levelWrapper = clientLevelWrapperGetWrapperMethod.invoke(null, level);
                if (levelWrapper == null) {
                    return 0;
                }

                Object dhClientWorld = sharedApiTryGetDhClientWorldMethod.invoke(null);
                if (dhClientWorld == null) {
                    return 0;
                }

                Object dhLevel = dhWorldGetOrLoadLevelMethod.invoke(dhClientWorld, levelWrapper);
                if (dhLevel == null) {
                    return 0;
                }

                Object fullDataProvider = dhLevelGetFullDataProviderMethod.invoke(dhLevel);
                if (fullDataProvider == null) {
                    return 0;
                }

                Object repo = fullDataProviderRepoField.get(fullDataProvider);
                if (repo == null) {
                    return 0;
                }

                int deleted = 0;
                for (long chunkKey : chunkKeys) {
                    if (deleted >= maxDeletes) {
                        break;
                    }

                    int chunkX = ChunkPos.getX(chunkKey);
                    int chunkZ = ChunkPos.getZ(chunkKey);
                    Object dhChunkPos = dhChunkPosConstructor.newInstance(chunkX, chunkZ);
                    Long sectionPos = (Long) dhSectionPosEncodeContainingMethod.invoke(
                        null,
                        dhSectionChunkDetailLevel,
                        dhChunkPos
                    );
                    if (sectionPos == null) {
                        continue;
                    }
                    // Purge only chunk-detail full-data rows to avoid high-detail LOD holes.
                    if (alreadyPurgedSectionPositions == null || alreadyPurgedSectionPositions.add(sectionPos)) {
                        repoDeleteWithKeyMethod.invoke(repo, sectionPos);
                        deleted++;
                    }
                }

                return deleted;
            } catch (Throwable t) {
                if (!refreshFailureLogged) {
                    refreshFailureLogged = true;
                    SereneBetterWinterMod.LOGGER.warn(
                        "[{}] Failed to purge Distant Horizons full-data entries via internal hooks.",
                        SereneBetterWinterMod.MOD_ID,
                        t
                    );
                }
                return 0;
            }
        }

        static int purgeAllFullData(
            ClientLevel level,
            int maxDeletes,
            Set<Long> alreadyPurgedSectionPositions
        ) {
            if (!initialize() || maxDeletes <= 0) {
                return 0;
            }

            try {
                Object levelWrapper = clientLevelWrapperGetWrapperMethod.invoke(null, level);
                if (levelWrapper == null) {
                    return 0;
                }

                Object dhClientWorld = sharedApiTryGetDhClientWorldMethod.invoke(null);
                if (dhClientWorld == null) {
                    return 0;
                }

                Object dhLevel = dhWorldGetOrLoadLevelMethod.invoke(dhClientWorld, levelWrapper);
                if (dhLevel == null) {
                    return 0;
                }

                Object fullDataProvider = dhLevelGetFullDataProviderMethod.invoke(dhLevel);
                if (fullDataProvider == null) {
                    return 0;
                }

                Object repo = fullDataProviderRepoField.get(fullDataProvider);
                if (repo == null) {
                    return 0;
                }

                Object positions = repoGetAllPositionsMethod.invoke(repo);
                if (positions == null) {
                    return 0;
                }

                int size = ((Number) longArraySizeMethod.invoke(positions)).intValue();
                int deleted = 0;
                for (int i = 0; i < size && deleted < maxDeletes; i++) {
                    long pos = ((Number) longArrayGetLongMethod.invoke(positions, i)).longValue();
                    if (alreadyPurgedSectionPositions == null || alreadyPurgedSectionPositions.add(pos)) {
                        repoDeleteWithKeyMethod.invoke(repo, pos);
                        deleted++;
                    }
                }

                return deleted;
            } catch (Throwable t) {
                if (!refreshFailureLogged) {
                    refreshFailureLogged = true;
                    SereneBetterWinterMod.LOGGER.warn(
                        "[{}] Failed to purge Distant Horizons global full-data entries via internal hooks.",
                        SereneBetterWinterMod.MOD_ID,
                        t
                    );
                }
                return 0;
            }
        }

        private static boolean initialize() {
            if (initAttempted) {
                return available;
            }

            synchronized (InternalRefreshHooks.class) {
                if (initAttempted) {
                    return available;
                }
                initAttempted = true;

                try {
                    ClassLoader cl = DistantHorizonsCompatImpl.class.getClassLoader();
                    Class<?> clientLevelWrapperClass = Class.forName(CLIENT_LEVEL_WRAPPER_CLASS, false, cl);
                    clientLevelWrapperGetWrapperMethod = clientLevelWrapperClass.getMethod("getWrapper", ClientLevel.class);

                    Class<?> chunkWrapperClass = Class.forName(CHUNK_WRAPPER_CLASS, false, cl);
                    for (Constructor<?> ctor : chunkWrapperClass.getConstructors()) {
                        Class<?>[] params = ctor.getParameterTypes();
                        if (params.length == 2 && ChunkAccess.class.isAssignableFrom(params[0])) {
                            chunkWrapperConstructor = ctor;
                            break;
                        }
                    }

                    Class<?> sharedApiClass = Class.forName(SHARED_API_CLASS, false, cl);
                    sharedApiInstanceField = sharedApiClass.getField("INSTANCE");
                    sharedApiGetAbstractDhWorldMethod = sharedApiClass.getMethod("getAbstractDhWorld");
                    sharedApiTryGetDhClientWorldMethod = sharedApiClass.getMethod("tryGetDhClientWorld");
                    for (Method method : sharedApiClass.getMethods()) {
                        if ("chunkLoadEvent".equals(method.getName()) && method.getParameterCount() == 2) {
                            sharedApiChunkLoadEventMethod = method;
                            break;
                        }
                    }

                    Class<?> iLevelWrapperClass = Class.forName(
                        "com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper",
                        false,
                        cl
                    );
                    Class<?> iDhWorldClass = Class.forName("com.seibel.distanthorizons.core.world.IDhWorld", false, cl);
                    dhWorldGetOrLoadLevelMethod = iDhWorldClass.getMethod("getOrLoadLevel", iLevelWrapperClass);

                    Class<?> iDhLevelClass = Class.forName("com.seibel.distanthorizons.core.level.IDhLevel", false, cl);
                    dhLevelGetFullDataProviderMethod = iDhLevelClass.getMethod("getFullDataProvider");

                    Class<?> fullDataProviderClass = Class.forName(
                        "com.seibel.distanthorizons.core.file.fullDatafile.V2.FullDataSourceProviderV2",
                        false,
                        cl
                    );
                    fullDataProviderQueuePositionForRetrievalMethod = fullDataProviderClass.getMethod("queuePositionForRetrieval", Long.class);
                    fullDataProviderCanQueueRetrievalNowMethod = fullDataProviderClass.getMethod("canQueueRetrievalNow");
                    fullDataProviderRepoField = fullDataProviderClass.getField("repo");

                    Class<?> abstractRepoClass = Class.forName("com.seibel.distanthorizons.core.sql.repo.AbstractDhRepo", false, cl);
                    repoDeleteWithKeyMethod = abstractRepoClass.getMethod("deleteWithKey", Object.class);
                    repoGetAllPositionsMethod = Class
                        .forName("com.seibel.distanthorizons.core.sql.repo.FullDataSourceV2Repo", false, cl)
                        .getMethod("getAllPositions");

                    Class<?> longArrayListClass = Class.forName("it.unimi.dsi.fastutil.longs.LongArrayList", false, cl);
                    longArraySizeMethod = longArrayListClass.getMethod("size");
                    longArrayGetLongMethod = longArrayListClass.getMethod("getLong", int.class);

                    Class<?> dhChunkPosClass = Class.forName("com.seibel.distanthorizons.core.pos.DhChunkPos", false, cl);
                    dhChunkPosConstructor = dhChunkPosClass.getConstructor(int.class, int.class);

                    Class<?> dhSectionPosClass = Class.forName("com.seibel.distanthorizons.core.pos.DhSectionPos", false, cl);
                    dhSectionPosEncodeContainingMethod = dhSectionPosClass.getMethod("encodeContaining", byte.class, dhChunkPosClass);
                    dhSectionPosGetParentPosMethod = dhSectionPosClass.getMethod("getParentPos", long.class);
                    Field sectionChunkDetailField = dhSectionPosClass.getField("SECTION_CHUNK_DETAIL_LEVEL");
                    dhSectionChunkDetailLevel = sectionChunkDetailField.getByte(null);

                    available = clientLevelWrapperGetWrapperMethod != null
                        && chunkWrapperConstructor != null
                        && sharedApiInstanceField != null
                        && sharedApiGetAbstractDhWorldMethod != null
                        && sharedApiChunkLoadEventMethod != null
                        && sharedApiTryGetDhClientWorldMethod != null
                        && dhWorldGetOrLoadLevelMethod != null
                        && dhLevelGetFullDataProviderMethod != null
                        && fullDataProviderQueuePositionForRetrievalMethod != null
                        && dhChunkPosConstructor != null
                        && dhSectionPosEncodeContainingMethod != null
                        && dhSectionPosGetParentPosMethod != null
                        && fullDataProviderRepoField != null
                        && repoDeleteWithKeyMethod != null
                        && repoGetAllPositionsMethod != null
                        && longArraySizeMethod != null
                        && longArrayGetLongMethod != null;
                    if (!available) {
                        throw new IllegalStateException("One or more internal DH refresh hooks were not found.");
                    }
                    return true;
                } catch (Throwable t) {
                    available = false;
                    if (!refreshFailureLogged) {
                        refreshFailureLogged = true;
                        SereneBetterWinterMod.LOGGER.warn(
                            "[{}] Distant Horizons internal refresh hooks unavailable. Seasonal LOD refresh fallback is disabled.",
                            SereneBetterWinterMod.MOD_ID,
                            t
                        );
                    }
                    return false;
                }
            }
        }
    }
}
