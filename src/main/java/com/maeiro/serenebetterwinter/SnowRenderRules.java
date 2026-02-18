package com.maeiro.serenebetterwinter;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class SnowRenderRules {
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
        return LeafTargeting.shouldHide(below);
    }
}
