package com.maeiro.serenebetterwinter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SereneBetterWinterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ServerSeasonTracker {
    private static final Map<ResourceKey<Level>, Boolean> LAST_LEAFLESS_STATE = new HashMap<>();
    private static final Map<ResourceKey<Level>, Boolean> PENDING_RELIGHT = new HashMap<>();

    private ServerSeasonTracker() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) {
            return;
        }
        if (!(event.level instanceof ServerLevel level)) {
            return;
        }

        if (level.getGameTime() % 20L != 0L) {
            return;
        }

        boolean currentLeafless = SeasonStateResolver.isLeaflessSeason(level);
        ResourceKey<Level> dimension = level.dimension();
        Boolean previousLeafless = LAST_LEAFLESS_STATE.put(dimension, currentLeafless);
        if (previousLeafless == null) {
            SereneBetterWinterMod.LOGGER.info(
                "[{}] Server leafless season initial state in {}: {}",
                SereneBetterWinterMod.MOD_ID,
                dimension.location(),
                currentLeafless
            );
            if (currentLeafless && ServerConfig.FORCE_RELIGHT_ON_SEASON_CHANGE.get() && ServerConfig.REMOVE_LIGHT_BLOCKING_FROM_HIDDEN_BLOCKS.get()) {
                if (!forceRelightAroundPlayers(level, true)) {
                    PENDING_RELIGHT.put(dimension, true);
                    SereneBetterWinterMod.LOGGER.info(
                        "[{}] Delaying relight in {} until players are present.",
                        SereneBetterWinterMod.MOD_ID,
                        dimension.location()
                    );
                }
            }
            return;
        }
        if (previousLeafless.booleanValue() == currentLeafless) {
            Boolean pending = PENDING_RELIGHT.get(dimension);
            if (Boolean.TRUE.equals(pending) && currentLeafless) {
                if (forceRelightAroundPlayers(level, true)) {
                    PENDING_RELIGHT.remove(dimension);
                }
            }
            return;
        }

        SereneBetterWinterMod.LOGGER.info(
            "[{}] Server leafless season changed in {}: {} -> {}",
            SereneBetterWinterMod.MOD_ID,
            dimension.location(),
            previousLeafless,
            currentLeafless
        );

        if (ServerConfig.FORCE_RELIGHT_ON_SEASON_CHANGE.get() && ServerConfig.REMOVE_LIGHT_BLOCKING_FROM_HIDDEN_BLOCKS.get()) {
            if (!forceRelightAroundPlayers(level, currentLeafless) && currentLeafless) {
                PENDING_RELIGHT.put(dimension, true);
            } else {
                PENDING_RELIGHT.remove(dimension);
            }
        }
    }

    private static boolean forceRelightAroundPlayers(ServerLevel level, boolean leaflessSeasonActive) {
        if (level.players().isEmpty()) {
            return false;
        }

        int relightChunkLimit = ServerConfig.RELIGHT_CHUNK_LIMIT.get();
        int relightScanBelowTop = ServerConfig.RELIGHT_SCAN_BELOW_TOP.get();
        int relightScanAboveTop = ServerConfig.RELIGHT_SCAN_ABOVE_TOP.get();

        Set<Long> chunksToUpdate = new HashSet<>();
        int radius = Math.max(6, level.getServer().getPlayerList().getViewDistance() + 1);

        for (ServerPlayer player : level.players()) {
            int centerX = SectionPos.blockToSectionCoord(player.getBlockX());
            int centerZ = SectionPos.blockToSectionCoord(player.getBlockZ());
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    chunksToUpdate.add(ChunkPos.asLong(centerX + dx, centerZ + dz));
                    if (chunksToUpdate.size() >= relightChunkLimit) {
                        break;
                    }
                }
                if (chunksToUpdate.size() >= relightChunkLimit) {
                    break;
                }
            }
            if (chunksToUpdate.size() >= relightChunkLimit) {
                break;
            }
        }

        LevelLightEngine lightEngine = level.getChunkSource().getLightEngine();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int relightChecks = 0;

        for (long packedChunkPos : chunksToUpdate) {
            int chunkX = ChunkPos.getX(packedChunkPos);
            int chunkZ = ChunkPos.getZ(packedChunkPos);
            LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
            if (chunk == null) {
                continue;
            }

            for (int localZ = 0; localZ < 16; localZ++) {
                for (int localX = 0; localX < 16; localX++) {
                    int topY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, localX, localZ);
                    int minY = Math.max(level.getMinBuildHeight(), topY - relightScanBelowTop);
                    int maxY = Math.min(level.getMaxBuildHeight() - 1, topY + relightScanAboveTop);
                    int worldX = (chunkX << 4) + localX;
                    int worldZ = (chunkZ << 4) + localZ;

                    for (int y = minY; y <= maxY; y++) {
                        cursor.set(worldX, y, worldZ);
                        if (!CollisionRules.shouldDisableLightBlockingForSeasonState(level, cursor, level.getBlockState(cursor), leaflessSeasonActive)) {
                            continue;
                        }

                        lightEngine.checkBlock(cursor);
                        lightEngine.checkBlock(cursor.above());
                        lightEngine.checkBlock(cursor.below());
                        relightChecks += 3;
                    }
                }
            }
        }

        SereneBetterWinterMod.LOGGER.info(
            "[{}] Forced relight in {} around {} chunk(s), checkBlock calls={}, limit={}, belowTop={}, aboveTop={}",
            SereneBetterWinterMod.MOD_ID,
            level.dimension().location(),
            chunksToUpdate.size(),
            relightChecks,
            relightChunkLimit,
            relightScanBelowTop,
            relightScanAboveTop
        );
        return true;
    }
}
