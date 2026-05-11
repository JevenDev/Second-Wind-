package com.jvn.secondwind.network;

import com.jvn.secondwind.state.FailureReason;
import com.jvn.secondwind.state.SecondWindService;
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
    }

    public static void syncToPlayer(ServerPlayer player) {
        // Real payload registration is added with the HUD/client phase.
    }

    public static void sendGiveUpRequest() {
        PacketDistributor.sendToServer(ServerboundGiveUpPayload.INSTANCE);
    }

    private static void handleGiveUp(ServerboundGiveUpPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && SecondWindService.isDowned(player)) {
            SecondWindService.failAndKill(player, FailureReason.GIVE_UP);
        }
    }
}
