package com.jvn.secondwind.network;

import com.jvn.secondwind.state.FailureReason;
import com.jvn.secondwind.state.SecondWindPlayerState;
import com.jvn.secondwind.state.SecondWindService;
import com.jvn.secondwind.client.ClientSecondWindState;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class SecondWindNetworking {
    private static final String NETWORK_VERSION = "1";

    private SecondWindNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToServer(ServerboundGiveUpPayload.TYPE, ServerboundGiveUpPayload.STREAM_CODEC, SecondWindNetworking::handleGiveUp);
        registrar.playToClient(ClientboundSecondWindStatePayload.TYPE, ClientboundSecondWindStatePayload.STREAM_CODEC, SecondWindNetworking::handleClientState);
    }

    public static void syncToPlayer(ServerPlayer player) {
        SecondWindPlayerState state = SecondWindService.getState(player);
        int cooldownSeconds = SecondWindService.getCooldownRemainingSeconds(player);
        PacketDistributor.sendToPlayer(player, new ClientboundSecondWindStatePayload(
                state.isDowned(),
                state.getDownedTicksRemaining(),
                state.getDownedMaxTicks(),
                state.isDowned(),
                state.getReviveChannelProgress(),
                cooldownSeconds));
    }

    public static void sendGiveUpRequest() {
        PacketDistributor.sendToServer(ServerboundGiveUpPayload.INSTANCE);
    }

    private static void handleGiveUp(ServerboundGiveUpPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && SecondWindService.isDowned(player)) {
            SecondWindService.failAndKill(player, FailureReason.GIVE_UP);
        }
    }

    private static void handleClientState(ClientboundSecondWindStatePayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        ClientSecondWindState.apply(payload);
    }
}
