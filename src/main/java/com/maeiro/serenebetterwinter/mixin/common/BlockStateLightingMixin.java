package com.maeiro.serenebetterwinter.mixin.common;

import com.maeiro.serenebetterwinter.CollisionRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateLightingMixin {
    @Inject(
        method = "getLightBlock(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)I",
        at = @At("HEAD"),
        cancellable = true
    )
    private void sereneBetterWinter$reduceLightBlocking(
        BlockGetter level,
        BlockPos pos,
        CallbackInfoReturnable<Integer> cir
    ) {
        if (!(level instanceof Level realLevel)) {
            return;
        }

        BlockState state = (BlockState) (Object) this;
        if (CollisionRules.shouldDisableLightBlocking(realLevel, pos, state)) {
            cir.setReturnValue(0);
        }
    }

    @Inject(
        method = "propagatesSkylightDown(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void sereneBetterWinter$forceSkylightPropagation(
        BlockGetter level,
        BlockPos pos,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(level instanceof Level realLevel)) {
            return;
        }

        BlockState state = (BlockState) (Object) this;
        if (CollisionRules.shouldDisableLightBlocking(realLevel, pos, state)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
        method = "getShadeBrightness(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F",
        at = @At("HEAD"),
        cancellable = true
    )
    private void sereneBetterWinter$brightenHiddenBlocks(
        BlockGetter level,
        BlockPos pos,
        CallbackInfoReturnable<Float> cir
    ) {
        if (!(level instanceof Level realLevel)) {
            return;
        }

        BlockState state = (BlockState) (Object) this;
        if (CollisionRules.shouldDisableLightBlocking(realLevel, pos, state)) {
            cir.setReturnValue(1.0F);
        }
    }

    @Inject(
        method = "getOcclusionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void sereneBetterWinter$clearOcclusionShapeForHiddenBlocks(
        BlockGetter level,
        BlockPos pos,
        CallbackInfoReturnable<VoxelShape> cir
    ) {
        if (!(level instanceof Level realLevel)) {
            return;
        }

        BlockState state = (BlockState) (Object) this;
        if (CollisionRules.shouldDisableLightBlocking(realLevel, pos, state)) {
            cir.setReturnValue(Shapes.empty());
        }
    }

    @Inject(
        method = "isSolidRender(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void sereneBetterWinter$disableSolidRenderForHiddenBlocks(
        BlockGetter level,
        BlockPos pos,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(level instanceof Level realLevel)) {
            return;
        }

        BlockState state = (BlockState) (Object) this;
        if (CollisionRules.shouldDisableLightBlocking(realLevel, pos, state)) {
            cir.setReturnValue(false);
        }
    }
}
