package com.maeiro.serenebetterwinter;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class CollisionRules {
    private CollisionRules() {
    }

    public static boolean shouldDisableCollision(Level level, BlockPos pos, BlockState state) {
        if (level == null || pos == null || state == null) {
            return false;
        }
        if (!ServerConfig.PASS_THROUGH_HIDDEN_BLOCKS.get()) {
            return false;
        }
        if (!SeasonStateResolver.isLeaflessSeason(level)) {
            return false;
        }

        if (LeafTargeting.shouldHide(state)) {
            return true;
        }

        if (state.is(Blocks.SNOW)) {
            BlockState below = level.getBlockState(pos.below());
            return LeafTargeting.shouldHide(below);
        }

        return false;
    }
}
