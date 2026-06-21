package com.jvn.secondwind.network;

import com.jvn.secondwind.SecondWindMod;
import com.jvn.secondwind.state.FailureReason;
import com.jvn.secondwind.state.SecondWindPlayerState;
import com.jvn.secondwind.state.SecondWindService;
import com.jvn.toucanlib.neoforge.network.ToucanNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class SecondWindNetworking {
    private static final String NETWORK_VERSION = "4";

    private SecondWindNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        ToucanNetwork network = ToucanNetwork.create(SecondWindMod.MOD_ID, NETWORK_VERSION, event);
        network.playToServer(ServerboundGiveUpPayload.TYPE, ServerboundGiveUpPayload.STREAM_CODEC, SecondWindNetworking::handleGiveUp);
        network.playToServer(ServerboundReviveHoldPayload.TYPE, ServerboundReviveHoldPayload.STREAM_CODEC, SecondWindNetworking::handleReviveHold);
        network.safePlayToClient(
                ClientboundSecondWindStatePayload.TYPE,
                ClientboundSecondWindStatePayload.STREAM_CODEC,
                "com.jvn.secondwind.client.ClientSecondWindState",
                "apply");
        network.safePlayToClient(
                ClientboundTrackedDownedPlayerPayload.TYPE,
                ClientboundTrackedDownedPlayerPayload.STREAM_CODEC,
                "com.jvn.secondwind.client.ClientTrackedDownedPlayers",
                "apply");
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
        PacketDistributor.sendToPlayer(player, new ClientboundSecondWindStatePayload(
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
                PacketDistributor.sendToPlayer(other, payload);
            }
        }
    }

    public static void sendGiveUpRequest() {
        PacketDistributor.sendToServer(ServerboundGiveUpPayload.INSTANCE);
    }

    public static void sendReviveHoldRequest(int targetEntityId) {
        PacketDistributor.sendToServer(new ServerboundReviveHoldPayload(targetEntityId));
    }

    private static void handleGiveUp(ServerboundGiveUpPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        ToucanNetwork.withServerPlayer(context, player -> {
            if (SecondWindService.isDowned(player)) {
                SecondWindService.failAndKill(player, FailureReason.GIVE_UP);
            }
        });
    }

    private static void handleReviveHold(ServerboundReviveHoldPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        ToucanNetwork.withServerPlayer(context, reviver -> {
            if (reviver.serverLevel().getEntity(payload.targetEntityId()) instanceof ServerPlayer downedPlayer) {
                SecondWindService.refreshReviveChannel(reviver, downedPlayer);
            }
        });
    }
}
