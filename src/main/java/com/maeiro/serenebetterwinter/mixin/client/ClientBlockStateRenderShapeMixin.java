package com.maeiro.serenebetterwinter.mixin.client;

import com.maeiro.serenebetterwinter.ClientConfig;
import com.maeiro.serenebetterwinter.ClientSeasonTracker;
import com.maeiro.serenebetterwinter.LeafTargeting;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class ClientBlockStateRenderShapeMixin {
    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void sereneBetterWinter$hideLeavesAtRenderShape(CallbackInfoReturnable<RenderShape> cir) {
        if (!ClientConfig.ENABLED.get() || !ClientSeasonTracker.isLeaflessSeasonActive()) {
            return;
        }

        BlockState state = (BlockState) (Object) this;
        if (LeafTargeting.shouldHide(state)) {
            cir.setReturnValue(RenderShape.INVISIBLE);
        }
    }
}
