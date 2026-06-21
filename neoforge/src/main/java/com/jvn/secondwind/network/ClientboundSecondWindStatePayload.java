package com.jvn.secondwind.network;

import com.jvn.secondwind.SecondWindMod;
import com.jvn.toucanlib.util.ToucanResourceLocations;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClientboundSecondWindStatePayload(
        boolean downed,
        int ticksRemaining,
        int maxTicks,
        int damageTicksLost,
        boolean giveUpAvailable,
        float reviveProgress,
        boolean timerPaused,
        int cooldownSeconds,
        boolean showReviveFlash,
        String reviverName) implements CustomPacketPayload {
    public static final Type<ClientboundSecondWindStatePayload> TYPE =
            new Type<>(ToucanResourceLocations.id(SecondWindMod.MOD_ID, "state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSecondWindStatePayload> STREAM_CODEC =
            StreamCodec.of(ClientboundSecondWindStatePayload::write, ClientboundSecondWindStatePayload::read);

    private static void write(RegistryFriendlyByteBuf buffer, ClientboundSecondWindStatePayload payload) {
        buffer.writeBoolean(payload.downed);
        buffer.writeVarInt(payload.ticksRemaining);
        buffer.writeVarInt(payload.maxTicks);
        buffer.writeVarInt(payload.damageTicksLost);
        buffer.writeBoolean(payload.giveUpAvailable);
        buffer.writeFloat(payload.reviveProgress);
        buffer.writeBoolean(payload.timerPaused);
        buffer.writeVarInt(payload.cooldownSeconds);
        buffer.writeBoolean(payload.showReviveFlash);
        buffer.writeBoolean(payload.reviverName != null && !payload.reviverName.isBlank());
        if (payload.reviverName != null && !payload.reviverName.isBlank()) {
            buffer.writeUtf(payload.reviverName);
        }
    }

    private static ClientboundSecondWindStatePayload read(RegistryFriendlyByteBuf buffer) {
        return new ClientboundSecondWindStatePayload(
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readFloat(),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readBoolean() ? buffer.readUtf() : "");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
