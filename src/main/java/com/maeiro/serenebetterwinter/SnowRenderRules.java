package com.maeiro.serenebetterwinter;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public final class SnowRenderRules {
    private static final int MAX_DEBUG_LINES = 80;
    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();
    private static final Set<String> PROBED = ConcurrentHashMap.newKeySet();

    private SnowRenderRules() {
    }

    public static boolean shouldHideSnowLayerAboveHiddenLeaves(BlockAndTintGetter level, BlockPos pos, BlockState snowState) {
        if (level == null || pos == null || snowState == null) {
            return false;
        }
        if (!ClientConfig.ENABLED.get() || !ClientConfig.HIDE_SNOW_ABOVE_HIDDEN_LEAVES.get()) {
            return false;
        }
        if (!ClientSeasonTracker.isLeaflessSeasonActive()) {
            return false;
        }
        if (!snowState.is(Blocks.SNOW)) {
            return false;
        }

        BlockState below = level.getBlockState(pos.below());
        boolean shouldHide = LeafTargeting.shouldHide(below);
        if (PROBED.size() < MAX_DEBUG_LINES) {
            ResourceLocation belowId = ForgeRegistries.BLOCKS.getKey(below.getBlock());
            String probe = pos.toShortString() + "|" + (belowId == null ? "unknown" : belowId.toString()) + "|hide=" + shouldHide;
            if (PROBED.add(probe)) {
                SereneBetterWinterMod.LOGGER.info("[{}] Snow render probe at {} above {} hide={}", SereneBetterWinterMod.MOD_ID, pos.toShortString(), belowId, shouldHide);
            }
        }
        if (shouldHide && LOGGED.size() < MAX_DEBUG_LINES) {
            ResourceLocation belowId = ForgeRegistries.BLOCKS.getKey(below.getBlock());
            String entry = pos.toShortString() + "|" + (belowId == null ? "unknown" : belowId.toString());
            if (LOGGED.add(entry)) {
                SereneBetterWinterMod.LOGGER.info("[{}] Snow layer skipped at {} above {}", SereneBetterWinterMod.MOD_ID, pos.toShortString(), belowId);
            }
        }

        return shouldHide;
    }
}
