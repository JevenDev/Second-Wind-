package com.jvn.secondwind.network;

import com.jvn.secondwind.SecondWindMod;
import com.jvn.toucanlib.util.toucanResourceLocations;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClientboundTrackedDownedPlayerPayload(
        int entityId,
        boolean downed,
        int ticksRemaining,
    int maxTicks,
    boolean timerPaused) implements CustomPacketPayload {
    public static final Type<ClientboundTrackedDownedPlayerPayload> TYPE =
            new Type<>(toucanResourceLocations.id(SecondWindMod.MOD_ID, "tracked_player_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundTrackedDownedPlayerPayload> STREAM_CODEC =
            StreamCodec.of(ClientboundTrackedDownedPlayerPayload::write, ClientboundTrackedDownedPlayerPayload::read);

    private static void write(RegistryFriendlyByteBuf buffer, ClientboundTrackedDownedPlayerPayload payload) {
        buffer.writeVarInt(payload.entityId);
        buffer.writeBoolean(payload.downed);
        buffer.writeVarInt(payload.ticksRemaining);
        buffer.writeVarInt(payload.maxTicks);
        buffer.writeBoolean(payload.timerPaused);
    }

    private static ClientboundTrackedDownedPlayerPayload read(RegistryFriendlyByteBuf buffer) {
        return new ClientboundTrackedDownedPlayerPayload(
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
