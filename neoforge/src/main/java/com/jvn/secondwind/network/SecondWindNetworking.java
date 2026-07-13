package com.jvn.secondwind.network;

import com.jvn.secondwind.SecondWindMod;
import com.jvn.secondwind.state.FailureReason;
import com.jvn.secondwind.state.SecondWindPlayerState;
import com.jvn.secondwind.state.SecondWindService;
import com.jvn.secondwind.state.SecondWindEntityService;
import com.jvn.secondwind.state.SecondWindEntityState;
import com.jvn.secondwind.api.ResolvedEntityPolicy;
import com.jvn.secondwind.config.SecondWindConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.LivingEntity;
import com.jvn.toucanlib.neoforge.network.ToucanNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

public final class SecondWindNetworking {
    private static final String NETWORK_VERSION = "5";

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
        safeSendToPlayer(player, new ClientboundSecondWindStatePayload(
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
                true,
                state.getDownedTicksRemaining(),
                state.getDownedMaxTicks(),
                SecondWindService.isBeingRevived(player),
                SecondWindConfig.MULTIPLAYER_REVIVE.get(),
                (int) Math.ceil(SecondWindConfig.REVIVE_CHANNEL_SECONDS.get() * 20.0D),
                SecondWindConfig.REVIVE_DISTANCE.get(),
                ResourceLocation.fromNamespaceAndPath(SecondWindMod.MOD_ID, "crawl"));

        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            if (other.serverLevel() == player.serverLevel()) {
                safeSendToPlayer(other, payload);
            }
        }
    }

    public static void syncTrackedEntity(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            syncToPlayer(player);
            return;
        }
        SecondWindEntityState state = SecondWindEntityService.getState(entity);
        ResolvedEntityPolicy policy = state.policy();
        ClientboundTrackedDownedPlayerPayload payload = new ClientboundTrackedDownedPlayerPayload(
                entity.getId(), state.isDowned(), policy != null && policy.showTimer(),
                state.ticksRemaining(), state.maxTicks(), state.reviveChannelReviver().isPresent(),
                policy != null && policy.reviveEnabled(),
                policy == null ? 0 : policy.reviveChannelTicks(),
                policy == null ? 0.0D : policy.reviveDistance(),
                policy == null ? ResourceLocation.fromNamespaceAndPath(SecondWindMod.MOD_ID, "crawl") : policy.pose());
        for (ServerPlayer player : entity.level().getServer().getPlayerList().getPlayers()) {
            if (player.serverLevel() == entity.level()) {
                safeSendToPlayer(player, payload);
            }
        }
    }

    public static void sendTrackedEntity(ServerPlayer player, LivingEntity entity) {
        SecondWindEntityState state = SecondWindEntityService.getState(entity);
        ResolvedEntityPolicy policy = state.policy();
        safeSendToPlayer(player, new ClientboundTrackedDownedPlayerPayload(
                entity.getId(), state.isDowned(), policy != null && policy.showTimer(), state.ticksRemaining(), state.maxTicks(),
                state.reviveChannelReviver().isPresent(), policy != null && policy.reviveEnabled(), policy == null ? 0 : policy.reviveChannelTicks(),
                policy == null ? 0.0D : policy.reviveDistance(),
                policy == null ? ResourceLocation.fromNamespaceAndPath(SecondWindMod.MOD_ID, "crawl") : policy.pose()));
    }

    private static void safeSendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (NetworkRegistry.hasChannel(player.connection, payload.type().id())) {
            PacketDistributor.sendToPlayer(player, payload);
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
            if (reviver.serverLevel().getEntity(payload.targetEntityId()) instanceof LivingEntity target) {
                SecondWindEntityService.refreshReviveChannel(reviver, target);
            }
        });
    }
}
