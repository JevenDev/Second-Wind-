package com.jvn.secondwind.network;

import com.jvn.secondwind.SecondWindMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ClientboundTrackedDownedPlayerPayload(
        int entityId,
        boolean downed,
        int ticksRemaining,
        int maxTicks) implements CustomPacketPayload {
    public static final Type<ClientboundTrackedDownedPlayerPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SecondWindMod.MOD_ID, "tracked_player_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundTrackedDownedPlayerPayload> STREAM_CODEC =
            StreamCodec.of(ClientboundTrackedDownedPlayerPayload::write, ClientboundTrackedDownedPlayerPayload::read);

    private static void write(RegistryFriendlyByteBuf buffer, ClientboundTrackedDownedPlayerPayload payload) {
        buffer.writeVarInt(payload.entityId);
        buffer.writeBoolean(payload.downed);
        buffer.writeVarInt(payload.ticksRemaining);
        buffer.writeVarInt(payload.maxTicks);
    }

    private static ClientboundTrackedDownedPlayerPayload read(RegistryFriendlyByteBuf buffer) {
        return new ClientboundTrackedDownedPlayerPayload(
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}