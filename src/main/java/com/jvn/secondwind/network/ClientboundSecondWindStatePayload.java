package com.jvn.secondwind.network;

import com.jvn.secondwind.SecondWindMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ClientboundSecondWindStatePayload(
        boolean downed,
        int ticksRemaining,
        int maxTicks,
        boolean giveUpAvailable,
        float reviveProgress,
        int cooldownSeconds) implements CustomPacketPayload {
    public static final Type<ClientboundSecondWindStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SecondWindMod.MOD_ID, "state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSecondWindStatePayload> STREAM_CODEC =
            StreamCodec.of(ClientboundSecondWindStatePayload::write, ClientboundSecondWindStatePayload::read);

    private static void write(RegistryFriendlyByteBuf buffer, ClientboundSecondWindStatePayload payload) {
        buffer.writeBoolean(payload.downed);
        buffer.writeVarInt(payload.ticksRemaining);
        buffer.writeVarInt(payload.maxTicks);
        buffer.writeBoolean(payload.giveUpAvailable);
        buffer.writeFloat(payload.reviveProgress);
        buffer.writeVarInt(payload.cooldownSeconds);
    }

    private static ClientboundSecondWindStatePayload read(RegistryFriendlyByteBuf buffer) {
        return new ClientboundSecondWindStatePayload(
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readFloat(),
                buffer.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
