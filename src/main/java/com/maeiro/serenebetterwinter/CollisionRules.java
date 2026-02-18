package com.maeiro.serenebetterwinter;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public final class CollisionRules {
    private static final TagKey<Block> DECIDUOUS_TAG =
        BlockTags.create(new ResourceLocation(SereneBetterWinterMod.MOD_ID, "deciduous_leaves"));
    private static final TagKey<Block> CONIFER_TAG =
        BlockTags.create(new ResourceLocation(SereneBetterWinterMod.MOD_ID, "conifer_leaves"));

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

        if (shouldHideLeafLike(state)) {
            return true;
        }

        if (state.is(Blocks.SNOW)) {
            BlockState below = level.getBlockState(pos.below());
            return shouldHideLeafLike(below);
        }

        return false;
    }

    private static boolean shouldHideLeafLike(BlockState state) {
        if (state == null) {
            return false;
        }

        Block block = state.getBlock();
        if (!(state.is(BlockTags.LEAVES) || block instanceof LeavesBlock || hasLeavesPath(block))) {
            return false;
        }

        if (state.is(CONIFER_TAG)) {
            return false;
        }
        if (state.is(DECIDUOUS_TAG)) {
            return true;
        }

        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
        if (key == null) {
            return false;
        }

        String path = key.getPath();
        if (path.contains("jungle")) {
            return false;
        }

        if (path.contains("spruce")
            || path.contains("pine")
            || path.contains("fir")
            || path.contains("cypress")
            || path.contains("juniper")
            || path.contains("cedar")
            || path.contains("sequoia")
            || path.contains("redwood")
            || path.contains("hemlock")
            || path.contains("conifer")) {
            return false;
        }

        String namespace = key.getNamespace();
        if ((namespace.equals("dynamic_trees") || namespace.startsWith("dynamictrees") || namespace.startsWith("dt"))
            && path.contains("leaves")) {
            return true;
        }

        return path.contains("leaves") || state.is(BlockTags.LEAVES) || block instanceof LeavesBlock;
    }

    private static boolean hasLeavesPath(Block block) {
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
        return key != null && key.getPath().contains("leaves");
    }
}
