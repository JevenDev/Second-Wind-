package com.jvn.secondwind.network;

import com.jvn.secondwind.SecondWindMod;
import com.jvn.toucanlib.util.ToucanResourceLocations;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ServerboundGiveUpPayload() implements CustomPacketPayload {
    public static final ServerboundGiveUpPayload INSTANCE = new ServerboundGiveUpPayload();
    public static final Type<ServerboundGiveUpPayload> TYPE =
            new Type<>(ToucanResourceLocations.id(SecondWindMod.MOD_ID, "give_up"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundGiveUpPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {
            }, buffer -> INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
