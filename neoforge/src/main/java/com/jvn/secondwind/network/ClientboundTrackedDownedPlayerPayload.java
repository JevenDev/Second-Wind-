package com.jvn.secondwind.network;

import com.jvn.secondwind.SecondWindMod;
import com.jvn.toucanlib.util.ToucanResourceLocations;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ClientboundTrackedDownedPlayerPayload(
        int entityId,
        boolean downed,
        boolean timerVisible,
        int ticksRemaining,
        int maxTicks,
        boolean timerPaused,
        boolean reviveEnabled,
        int reviveChannelTicks,
        double reviveDistance,
        ResourceLocation pose) implements CustomPacketPayload {
    public static final Type<ClientboundTrackedDownedPlayerPayload> TYPE =
            new Type<>(ToucanResourceLocations.id(SecondWindMod.MOD_ID, "tracked_player_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundTrackedDownedPlayerPayload> STREAM_CODEC =
            StreamCodec.of(ClientboundTrackedDownedPlayerPayload::write, ClientboundTrackedDownedPlayerPayload::read);

    private static void write(RegistryFriendlyByteBuf buffer, ClientboundTrackedDownedPlayerPayload payload) {
        buffer.writeVarInt(payload.entityId);
        buffer.writeBoolean(payload.downed);
        buffer.writeBoolean(payload.timerVisible);
        buffer.writeVarInt(payload.ticksRemaining);
        buffer.writeVarInt(payload.maxTicks);
        buffer.writeBoolean(payload.timerPaused);
        buffer.writeBoolean(payload.reviveEnabled);
        buffer.writeVarInt(payload.reviveChannelTicks);
        buffer.writeDouble(payload.reviveDistance);
        buffer.writeResourceLocation(payload.pose);
    }

    private static ClientboundTrackedDownedPlayerPayload read(RegistryFriendlyByteBuf buffer) {
        return new ClientboundTrackedDownedPlayerPayload(
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readDouble(),
                buffer.readResourceLocation());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
