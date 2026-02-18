package com.maeiro.serenebetterwinter;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public final class LeafTargeting {
    private static final TagKey<Block> DECIDUOUS_TAG =
        BlockTags.create(ResourceLocation.tryBuild(SereneBetterWinterMod.MOD_ID, "deciduous_leaves"));
    private static final TagKey<Block> CONIFER_TAG =
        BlockTags.create(ResourceLocation.tryBuild(SereneBetterWinterMod.MOD_ID, "conifer_leaves"));

    private static final Set<String> CONIFER_HINTS = Set.of(
        "spruce", "pine", "fir", "cypress", "juniper", "cedar", "sequoia", "redwood", "hemlock", "conifer"
    );

    private LeafTargeting() {
    }

    public static boolean isLeavesLike(Block block) {
        if (block == null) {
            return false;
        }
        if (block instanceof LeavesBlock) {
            return true;
        }
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
        return key != null && key.getPath().contains("leaves");
    }

    public static boolean shouldHide(BlockState state) {
        if (state == null) {
            return false;
        }

        Block block = state.getBlock();
        if (!(state.is(BlockTags.LEAVES) || isLeavesLike(block))) {
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
        for (String hint : CONIFER_HINTS) {
            if (path.contains(hint)) {
                return false;
            }
        }

        String namespace = key.getNamespace();
        if ((namespace.equals("dynamic_trees") || namespace.startsWith("dynamictrees") || namespace.startsWith("dt"))
            && path.contains("leaves")) {
            return true;
        }

        return path.contains("leaves") || state.is(BlockTags.LEAVES) || block instanceof LeavesBlock;
    }
}
