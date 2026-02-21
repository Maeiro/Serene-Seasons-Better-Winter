package com.maeiro.serenebetterwinter;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public final class AttachedHiddenLeafRules {
    private static final TagKey<Block> ALWAYS_HIDE_TAG =
        BlockTags.create(ResourceLocation.tryBuild(SereneBetterWinterMod.MOD_ID, "always_hide_when_attached_to_hidden_leaves"));
    private static final TagKey<Block> NEVER_HIDE_TAG =
        BlockTags.create(ResourceLocation.tryBuild(SereneBetterWinterMod.MOD_ID, "never_hide_when_attached_to_hidden_leaves"));
    private static volatile boolean hideAttachedToHiddenLeaves = true;
    private static final Set<String> ATTACHMENT_HINTS = Set.of("apple", "cocoon", "cotton", "silk", "fruit", "pod");
    private static final Set<String> WOODY_HINTS = Set.of("branch", "trunk", "log", "stem", "wood", "bark", "root", "stump", "twig");

    private AttachedHiddenLeafRules() {
    }

    public static void applyServerConfig(boolean enabled) {
        hideAttachedToHiddenLeaves = enabled;
    }

    public static void resetToDefaults() {
        hideAttachedToHiddenLeaves = true;
    }

    public static boolean shouldHideAttachedBlock(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        if (level == null || pos == null || state == null) {
            return false;
        }
        if (!hideAttachedToHiddenLeaves) {
            return false;
        }
        if (state.isAir()) {
            return false;
        }
        if (state.is(NEVER_HIDE_TAG)) {
            return false;
        }
        if (LeafTargeting.shouldHide(state)) {
            return false;
        }
        if (SnowRenderRules.shouldHideSnowLayerAboveHiddenLeaves(level, pos, state)) {
            return false;
        }

        BlockState above = level.getBlockState(pos.above());
        if (!LeafTargeting.shouldHide(above)) {
            return false;
        }
        if (state.is(ALWAYS_HIDE_TAG)) {
            return true;
        }

        // Never hide structural tree blocks.
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.LOGS_THAT_BURN)) {
            return false;
        }

        // Soft hint fallback for modded attachment-like blocks (apple/cocoon/etc).
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (key != null) {
            String path = key.getPath();
            for (String hint : WOODY_HINTS) {
                if (path.contains(hint)) {
                    return false;
                }
            }
            for (String hint : ATTACHMENT_HINTS) {
                if (path.contains(hint)) {
                    return true;
                }
            }
        }

        // Conservative default to avoid hiding random tree structure pieces.
        return false;
    }
}
