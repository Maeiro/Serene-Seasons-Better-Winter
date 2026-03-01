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
    private static volatile boolean initialized = false;
    private static volatile boolean handlerRegistered = false;
    private static volatile boolean registrationFailureLogged = false;
    private static volatile boolean refreshFailureLogged = false;
    private static volatile IDhApiBlockStateWrapper cachedAirBlockWrapper;
    private static int refreshLogCount = 0;
    private static final int REFRESH_LOG_LIMIT = 20;
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
        registerChunkProcessingHandler();
    }

    public static void onLeaflessSeasonStateChanged(ClientLevel level, boolean previousState, boolean currentState) {
        if (!handlerRegistered) {
            return;
        }
        DistantHorizonsLodRefreshManager.onLeaflessSeasonStateChanged(level, previousState, currentState);
    }

    public static void onClientTick(ClientLevel level) {
        if (!handlerRegistered) {
            return;
        }
        DistantHorizonsLodRefreshManager.onClientTick(level);
    }

    public static void onClientLogout() {
        DistantHorizonsLodRefreshManager.onClientLogout();
    }

    static void clearRenderDataCacheForSeasonChange(boolean leaflessSeasonActive) {
        if (!handlerRegistered) {
            return;
        }
        if (!ClientConfig.ENABLED.get() || !ClientConfig.ENABLE_DH_LOD_LEAF_HIDING.get()) {
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

    static int requestLodRefreshAroundPlayers(ClientLevel level, boolean leaflessSeasonActive) {
        if (!handlerRegistered || level == null) {
            return 0;
        }
        if (!ClientConfig.ENABLED.get() || !ClientConfig.ENABLE_DH_LOD_LEAF_HIDING.get()) {
            return 0;
        }
        if (!ClientConfig.DH_AUTO_REFRESH_ON_SEASON_TOGGLE.get()) {
            return 0;
        }

        int radius = Math.max(1, ClientConfig.DH_REFRESH_RADIUS_CHUNKS.get());
        int cap = Math.max(64, ClientConfig.DH_REFRESH_CHUNK_CAP.get());
        Set<Long> targetChunks = collectTargetChunks(level, radius, cap);
        int enqueued = enqueueChunkRefresh(level, targetChunks, cap);

        if (refreshLogCount < REFRESH_LOG_LIMIT) {
            refreshLogCount++;
            if (enqueued > 0) {
                SereneBetterWinterMod.LOGGER.info(
                    "[{}] Requested Distant Horizons LOD refresh for {} chunk(s) after leafless state change (active={}).",
                    SereneBetterWinterMod.MOD_ID,
                    enqueued,
                    leaflessSeasonActive
                );
            } else {
                SereneBetterWinterMod.LOGGER.info(
                    "[{}] Distant Horizons LOD refresh enqueued 0 chunks (active={}, targets={}, dhWorldReady={}).",
                    SereneBetterWinterMod.MOD_ID,
                    leaflessSeasonActive,
                    targetChunks.size(),
                    InternalRefreshHooks.isDhWorldReady()
                );
            }
        }

        return enqueued;
    }

    private static void registerChunkProcessingHandler() {
        try {
            DhApiResult<Void> result = DhApiEventRegister.on(DhApiChunkProcessingEvent.class, CHUNK_PROCESSING_EVENT);
            if (result != null && result.success) {
                handlerRegistered = true;
                SereneBetterWinterMod.LOGGER.info("[{}] Registered Distant Horizons chunk processing handler.", SereneBetterWinterMod.MOD_ID);
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
            if (event == null || event.value == null) {
                return;
            }
            if (!ClientConfig.ENABLED.get() || !ClientConfig.ENABLE_DH_LOD_LEAF_HIDING.get()) {
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
                    for (Method method : sharedApiClass.getMethods()) {
                        if ("chunkLoadEvent".equals(method.getName()) && method.getParameterCount() == 2) {
                            sharedApiChunkLoadEventMethod = method;
                            break;
                        }
                    }

                    available = clientLevelWrapperGetWrapperMethod != null
                        && chunkWrapperConstructor != null
                        && sharedApiInstanceField != null
                        && sharedApiGetAbstractDhWorldMethod != null
                        && sharedApiChunkLoadEventMethod != null;
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
