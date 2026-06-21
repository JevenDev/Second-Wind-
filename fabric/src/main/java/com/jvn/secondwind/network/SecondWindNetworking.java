package com.jvn.secondwind.network;

import com.jvn.secondwind.client.ClientSecondWindState;
import com.jvn.secondwind.client.ClientTrackedDownedPlayers;
import com.jvn.secondwind.state.FailureReason;
import com.jvn.secondwind.state.SecondWindPlayerState;
import com.jvn.secondwind.state.SecondWindService;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class SecondWindNetworking {
    private SecondWindNetworking() {
    }

    public static void registerPayloads() {
        PayloadTypeRegistry.playC2S().register(ServerboundGiveUpPayload.TYPE, ServerboundGiveUpPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ServerboundReviveHoldPayload.TYPE, ServerboundReviveHoldPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ClientboundSecondWindStatePayload.TYPE, ClientboundSecondWindStatePayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ClientboundTrackedDownedPlayerPayload.TYPE, ClientboundTrackedDownedPlayerPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ServerboundGiveUpPayload.TYPE, SecondWindNetworking::handleGiveUp);
        ServerPlayNetworking.registerGlobalReceiver(ServerboundReviveHoldPayload.TYPE, SecondWindNetworking::handleReviveHold);
    }

    public static void registerClientPayloads() {
        ClientPlayNetworking.registerGlobalReceiver(ClientboundSecondWindStatePayload.TYPE,
                (payload, context) -> context.client().execute(() -> ClientSecondWindState.apply(payload)));
        ClientPlayNetworking.registerGlobalReceiver(ClientboundTrackedDownedPlayerPayload.TYPE,
                (payload, context) -> context.client().execute(() -> ClientTrackedDownedPlayers.apply(payload)));
    }

    public static void syncToPlayer(ServerPlayer player) {
        syncToPlayer(player, false, 0);
    }

    public static void syncToPlayer(ServerPlayer player, boolean showReviveFlash) {
        syncToPlayer(player, showReviveFlash, 0);
    }

    public static void syncToPlayer(ServerPlayer player, boolean showReviveFlash, int damageTicksLost) {
        SecondWindPlayerState state = SecondWindService.getState(player);
        int cooldownSeconds = SecondWindService.getCooldownRemainingSeconds(player);
        ServerPlayNetworking.send(player, new ClientboundSecondWindStatePayload(
                state.isDowned(),
                state.getDownedTicksRemaining(),
                state.getDownedMaxTicks(),
                Math.max(0, damageTicksLost),
                state.isDowned(),
                state.getReviveChannelProgress(),
                SecondWindService.isBeingRevived(player),
                cooldownSeconds,
                showReviveFlash,
                currentReviverName(player, state)));

        syncTrackedDownedState(player, state);
    }

    private static String currentReviverName(ServerPlayer player, SecondWindPlayerState state) {
        return state.getReviveChannelReviver()
                .map(player.server.getPlayerList()::getPlayer)
                .map(ServerPlayer::getName)
                .map(component -> component.getString())
                .orElse("");
    }

    private static void syncTrackedDownedState(ServerPlayer player, SecondWindPlayerState state) {
        ClientboundTrackedDownedPlayerPayload payload = new ClientboundTrackedDownedPlayerPayload(
                player.getId(),
                state.isDowned(),
                state.getDownedTicksRemaining(),
                state.getDownedMaxTicks(),
                SecondWindService.isBeingRevived(player));

        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            if (other.serverLevel() == player.serverLevel()) {
                ServerPlayNetworking.send(other, payload);
            }
        }
    }

    public static void sendGiveUpRequest() {
        ClientPlayNetworking.send(ServerboundGiveUpPayload.INSTANCE);
    }

    public static void sendReviveHoldRequest(int targetEntityId) {
        ClientPlayNetworking.send(new ServerboundReviveHoldPayload(targetEntityId));
    }

    private static void handleGiveUp(ServerboundGiveUpPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            if (SecondWindService.isDowned(player)) {
                SecondWindService.failAndKill(player, FailureReason.GIVE_UP);
            }
        });
    }

    private static void handleReviveHold(ServerboundReviveHoldPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer reviver = context.player();
            if (reviver.serverLevel().getEntity(payload.targetEntityId()) instanceof ServerPlayer downedPlayer) {
                SecondWindService.refreshReviveChannel(reviver, downedPlayer);
            }
        });
    }
}
