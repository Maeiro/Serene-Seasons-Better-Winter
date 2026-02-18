package com.maeiro.serenebetterwinter;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SereneBetterWinterMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientSeasonTracker {
    private static volatile boolean leaflessSeasonActive = false;
    private static boolean lastEnabledConfig = true;

    public static boolean isLeaflessSeasonActive() {
        return leaflessSeasonActive;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        boolean enabled = ClientConfig.ENABLED.get();
        if (enabled != lastEnabledConfig) {
            lastEnabledConfig = enabled;
            SereneBetterWinterMod.LOGGER.info("[{}] Config changed: enabled={}", SereneBetterWinterMod.MOD_ID, enabled);
        }

        if (!enabled) {
            leaflessSeasonActive = false;
            return;
        }

        if (net.minecraft.client.Minecraft.getInstance().level == null) {
            leaflessSeasonActive = false;
            return;
        }

        boolean nextState = SeasonStateResolver.isLeaflessSeason(net.minecraft.client.Minecraft.getInstance().level);
        if (leaflessSeasonActive != nextState) {
            leaflessSeasonActive = nextState;
            SereneBetterWinterMod.LOGGER.info("[{}] Leafless season state changed: {}", SereneBetterWinterMod.MOD_ID, nextState);
        } else {
            leaflessSeasonActive = nextState;
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        leaflessSeasonActive = false;
    }
}
