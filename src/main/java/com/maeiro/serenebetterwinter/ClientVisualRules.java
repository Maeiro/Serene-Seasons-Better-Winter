package com.maeiro.serenebetterwinter;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

public final class ClientVisualRules {
    private ClientVisualRules() {
    }

    public static boolean shouldHideBlockVisual(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        if (level == null || pos == null || state == null) {
            return false;
        }
        if (!ClientConfig.ENABLED.get() || !ClientSeasonTracker.isLeaflessSeasonActive()) {
            return false;
        }
        if (LeafTargeting.shouldHide(state)) {
            return true;
        }
        return SnowRenderRules.shouldHideSnowLayerAboveHiddenLeaves(level, pos, state);
    }
}

