package com.maeiro.serenebetterwinter.mixin.client;

import com.maeiro.serenebetterwinter.ClientConfig;
import com.maeiro.serenebetterwinter.ClientSeasonTracker;
import com.maeiro.serenebetterwinter.LeafTargeting;
import com.maeiro.serenebetterwinter.SereneBetterWinterMod;
import com.seibel.distanthorizons.core.dataObjects.fullData.FullDataPointIdMap;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.distanthorizons.core.wrapperInterfaces.block.IBlockStateWrapper;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(
    targets = "com.seibel.distanthorizons.core.dataObjects.transformers.FullDataToRenderDataTransformer",
    remap = false
)
public abstract class DistantHorizonsFullDataTransformerMixin {
    private static volatile IBlockStateWrapper SBW_AIR_WRAPPER;
    private static volatile boolean SBW_LOGGED_TRANSFORMER_ACTIVE = false;
    private static int SBW_REPLACEMENT_LOG_COUNT = 0;
    private static final int SBW_REPLACEMENT_LOG_LIMIT = 120;

    @Redirect(
        method = "setRenderColumnView",
        at = @At(
            value = "INVOKE",
            target = "Lcom/seibel/distanthorizons/core/dataObjects/fullData/FullDataPointIdMap;getBlockStateWrapper(I)Lcom/seibel/distanthorizons/core/wrapperInterfaces/block/IBlockStateWrapper;"
        ),
        require = 0
    )
    private static IBlockStateWrapper sereneBetterWinter$replaceHiddenLeavesWithAir(
        FullDataPointIdMap map,
        int id
    ) {
        if (!SBW_LOGGED_TRANSFORMER_ACTIVE) {
            SBW_LOGGED_TRANSFORMER_ACTIVE = true;
            SereneBetterWinterMod.LOGGER.info(
                "[{}] DH full-data transformer hook is active.",
                SereneBetterWinterMod.MOD_ID
            );
        }

        IBlockStateWrapper blockStateWrapper = map.getBlockStateWrapper(id);
        if (blockStateWrapper == null) {
            return null;
        }
        if (!ClientConfig.isDhIntegrationEnabled()) {
            return blockStateWrapper;
        }
        if (!ClientSeasonTracker.isLeaflessSeasonActive()) {
            return blockStateWrapper;
        }

        Object wrapped = blockStateWrapper.getWrappedMcObject();
        if (!(wrapped instanceof BlockState blockState)) {
            return blockStateWrapper;
        }
        if (!LeafTargeting.shouldHide(blockState)) {
            return blockStateWrapper;
        }

        IBlockStateWrapper air = getAirWrapper();
        if (air != null && SBW_REPLACEMENT_LOG_COUNT < SBW_REPLACEMENT_LOG_LIMIT) {
            SBW_REPLACEMENT_LOG_COUNT++;
            SereneBetterWinterMod.LOGGER.info(
                "[{}] DH full-data leaf override applied for block {} (id={}).",
                SereneBetterWinterMod.MOD_ID,
                blockState.getBlock(),
                id
            );
        }
        return air != null ? air : blockStateWrapper;
    }

    private static IBlockStateWrapper getAirWrapper() {
        IBlockStateWrapper cached = SBW_AIR_WRAPPER;
        if (cached != null) {
            return cached;
        }

        try {
            IWrapperFactory wrapperFactory = SingletonInjector.INSTANCE.get(IWrapperFactory.class);
            if (wrapperFactory != null) {
                cached = wrapperFactory.getAirBlockStateWrapper();
                SBW_AIR_WRAPPER = cached;
            }
        } catch (Throwable ignored) {
        }

        return cached;
    }
}
