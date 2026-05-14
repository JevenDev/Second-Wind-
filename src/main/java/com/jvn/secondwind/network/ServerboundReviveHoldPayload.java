package com.jvn.secondwind.network;

import com.jvn.secondwind.SecondWindMod;
import com.jvn.toucanlib.util.ToucanResourceLocations;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ServerboundReviveHoldPayload(int targetEntityId) implements CustomPacketPayload {
    public static final Type<ServerboundReviveHoldPayload> TYPE =
            new Type<>(ToucanResourceLocations.id(SecondWindMod.MOD_ID, "revive_hold"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundReviveHoldPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.INT, ServerboundReviveHoldPayload::targetEntityId, ServerboundReviveHoldPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
