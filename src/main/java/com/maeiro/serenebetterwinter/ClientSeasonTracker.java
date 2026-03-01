package com.maeiro.serenebetterwinter;

import com.maeiro.serenebetterwinter.dh.DistantHorizonsCompatBootstrap;
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

        DistantHorizonsCompatBootstrap.tryInit();

        boolean enabled = ClientConfig.ENABLED.get();
        if (enabled != lastEnabledConfig) {
            lastEnabledConfig = enabled;
            SereneBetterWinterMod.LOGGER.info("[{}] Config changed: enabled={}", SereneBetterWinterMod.MOD_ID, enabled);
        }

        if (!enabled) {
            leaflessSeasonActive = false;
            DistantHorizonsCompatBootstrap.onClientTick(net.minecraft.client.Minecraft.getInstance().level);
            return;
        }

        if (net.minecraft.client.Minecraft.getInstance().level == null) {
            leaflessSeasonActive = false;
            DistantHorizonsCompatBootstrap.onClientTick(null);
            return;
        }

        net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
        boolean nextState = SeasonStateResolver.isLeaflessSeason(level);
        if (leaflessSeasonActive != nextState) {
            boolean previousState = leaflessSeasonActive;
            leaflessSeasonActive = nextState;
            SereneBetterWinterMod.LOGGER.info("[{}] Leafless season state changed: {}", SereneBetterWinterMod.MOD_ID, nextState);
            DistantHorizonsCompatBootstrap.onLeaflessSeasonStateChanged(level, previousState, nextState);
        } else {
            leaflessSeasonActive = nextState;
        }

        DistantHorizonsCompatBootstrap.onClientTick(level);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        leaflessSeasonActive = false;
        AttachedHiddenLeafRules.resetToDefaults();
        DistantHorizonsCompatBootstrap.onClientLogout();
    }
}
