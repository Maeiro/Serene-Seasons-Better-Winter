package com.maeiro.serenebetterwinter.mixin.client;

import com.maeiro.serenebetterwinter.ClientConfig;
import com.maeiro.serenebetterwinter.ClientSeasonTracker;
import com.maeiro.serenebetterwinter.LeafTargeting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.model.light.data.LightDataAccess", remap = false)
public abstract class EmbeddiumLightDataAccessMixin {
    @Shadow
    protected BlockAndTintGetter world;

    private static final ThreadLocal<BlockPos.MutableBlockPos> POS_CACHE =
        ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

    @Inject(method = "compute(III)I", at = @At("HEAD"), cancellable = true)
    private void sereneBetterWinter$overrideHiddenLeavesLightData(
        int x,
        int y,
        int z,
        CallbackInfoReturnable<Integer> cir
    ) {
        if (!ClientConfig.ENABLED.get() || !ClientSeasonTracker.isLeaflessSeasonActive()) {
            return;
        }
        if (world == null) {
            return;
        }

        BlockPos.MutableBlockPos pos = POS_CACHE.get().set(x, y, z);
        BlockState state = world.getBlockState(pos);

        boolean hiddenLeaf = LeafTargeting.shouldHide(state);
        boolean hiddenSnowAboveLeaf = !hiddenLeaf && state.is(Blocks.SNOW) && LeafTargeting.shouldHide(world.getBlockState(pos.below()));
        if (!hiddenLeaf && !hiddenSnowAboveLeaf) {
            return;
        }

        int blockLight = world.getBrightness(LightLayer.BLOCK, pos);
        int skyLight = world.getBrightness(LightLayer.SKY, pos);
        cir.setReturnValue(packLightData(blockLight, skyLight, 0, 1.0f, false, false, false, false));
    }

    private static int packLightData(
        int blockLight,
        int skyLight,
        int luminance,
        float ao,
        boolean emissive,
        boolean opaque,
        boolean fullOpaque,
        boolean fullCube
    ) {
        int bl = blockLight & 15;
        int sl = (skyLight & 15) << 4;
        int lu = (luminance & 15) << 8;
        int aoBits = (((int) (ao * 4096.0f)) & 0xFFFF) << 12;
        int em = (emissive ? 1 : 0) << 28;
        int op = (opaque ? 1 : 0) << 29;
        int fo = (fullOpaque ? 1 : 0) << 30;
        int fc = (fullCube ? 1 : 0) << 31;
        return bl | sl | lu | aoBits | em | op | fo | fc;
    }
}
