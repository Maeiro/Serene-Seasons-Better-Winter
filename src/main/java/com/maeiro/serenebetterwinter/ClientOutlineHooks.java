package com.maeiro.serenebetterwinter;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SereneBetterWinterMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientOutlineHooks {
    private ClientOutlineHooks() {
    }

    @SubscribeEvent
    public static void onRenderBlockHighlight(RenderHighlightEvent.Block event) {
        if (!ClientConfig.HIDE_OUTLINE_FOR_HIDDEN_BLOCKS.get()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        BlockHitResult target = event.getTarget();
        if (target == null) {
            return;
        }

        if (ClientVisualRules.shouldHideBlockVisual(minecraft.level, target.getBlockPos(), minecraft.level.getBlockState(target.getBlockPos()))) {
            event.setCanceled(true);
        }
    }
}

