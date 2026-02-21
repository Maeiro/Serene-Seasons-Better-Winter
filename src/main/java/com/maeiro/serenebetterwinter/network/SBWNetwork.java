package com.maeiro.serenebetterwinter.network;

import com.maeiro.serenebetterwinter.SereneBetterWinterMod;
import com.maeiro.serenebetterwinter.ServerConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class SBWNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        ResourceLocation.tryBuild(SereneBetterWinterMod.MOD_ID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
    private static int nextId = 0;
    private static boolean initialized = false;

    private SBWNetwork() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        CHANNEL.registerMessage(
            nextId++,
            SyncVisualRulesS2CPacket.class,
            SyncVisualRulesS2CPacket::encode,
            SyncVisualRulesS2CPacket::decode,
            SyncVisualRulesS2CPacket::handle
        );
        initialized = true;
    }

    public static void syncToPlayer(ServerPlayer player) {
        CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new SyncVisualRulesS2CPacket(ServerConfig.HIDE_BLOCKS_ATTACHED_TO_HIDDEN_LEAVES.get())
        );
    }

    public static void broadcast(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncToPlayer(player);
        }
    }
}

