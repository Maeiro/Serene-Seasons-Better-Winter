package com.maeiro.serenebetterwinter.network;

import com.maeiro.serenebetterwinter.AttachedHiddenLeafRules;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record SyncVisualRulesS2CPacket(boolean hideBlocksAttachedToHiddenLeaves) {
    public static void encode(SyncVisualRulesS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.hideBlocksAttachedToHiddenLeaves);
    }

    public static SyncVisualRulesS2CPacket decode(FriendlyByteBuf buf) {
        return new SyncVisualRulesS2CPacket(buf.readBoolean());
    }

    public static void handle(SyncVisualRulesS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> AttachedHiddenLeafRules.applyServerConfig(packet.hideBlocksAttachedToHiddenLeaves));
        context.setPacketHandled(true);
    }
}

