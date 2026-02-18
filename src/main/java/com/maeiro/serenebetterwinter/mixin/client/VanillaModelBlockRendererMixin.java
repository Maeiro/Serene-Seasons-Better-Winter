package com.maeiro.serenebetterwinter.mixin.client;

import com.maeiro.serenebetterwinter.SnowRenderRules;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBlockRenderer.class)
public abstract class VanillaModelBlockRendererMixin {
    @Inject(
        method = "tesselateWithAO",
        at = @At("HEAD"),
        cancellable = true
    )
    private void sereneBetterWinter$skipSnowLayerOnHiddenLeavesWithAO(
        BlockAndTintGetter level,
        BakedModel model,
        BlockState state,
        BlockPos pos,
        PoseStack poseStack,
        VertexConsumer consumer,
        boolean checkSides,
        RandomSource random,
        long seed,
        int packedOverlay,
        CallbackInfo ci
    ) {
        if (SnowRenderRules.shouldHideSnowLayerAboveHiddenLeaves(level, pos, state)) {
            ci.cancel();
        }
    }

    @Inject(
        method = "tesselateWithoutAO",
        at = @At("HEAD"),
        cancellable = true
    )
    private void sereneBetterWinter$skipSnowLayerOnHiddenLeavesWithoutAO(
        BlockAndTintGetter level,
        BakedModel model,
        BlockState state,
        BlockPos pos,
        PoseStack poseStack,
        VertexConsumer consumer,
        boolean checkSides,
        RandomSource random,
        long seed,
        int packedOverlay,
        CallbackInfo ci
    ) {
        if (SnowRenderRules.shouldHideSnowLayerAboveHiddenLeaves(level, pos, state)) {
            ci.cancel();
        }
    }
}
