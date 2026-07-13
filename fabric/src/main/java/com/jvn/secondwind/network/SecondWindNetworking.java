package com.jvn.secondwind.network;

import com.jvn.secondwind.client.ClientSecondWindState;
import com.jvn.secondwind.client.ClientTrackedDownedPlayers;
import com.jvn.secondwind.state.FailureReason;
import com.jvn.secondwind.state.SecondWindPlayerState;
import com.jvn.secondwind.state.SecondWindService;
import com.jvn.secondwind.state.SecondWindEntityService;
import com.jvn.secondwind.state.SecondWindEntityState;
import com.jvn.secondwind.api.ResolvedEntityPolicy;
import com.jvn.secondwind.config.SecondWindConfig;
import com.jvn.secondwind.SecondWindMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

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
                SecondWindConfig.FORCE_CRAWLING_POSE.get(),
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
                ServerPlayNetworking.send(other, payload);
            }
        }
    }

    public static void syncTrackedEntity(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) { syncToPlayer(player); return; }
        ClientboundTrackedDownedPlayerPayload payload = trackedPayload(entity);
        for (ServerPlayer player : PlayerLookup.tracking(entity)) ServerPlayNetworking.send(player, payload);
    }

    public static void sendTrackedEntity(ServerPlayer player, LivingEntity entity) {
        ServerPlayNetworking.send(player, trackedPayload(entity));
    }

    private static ClientboundTrackedDownedPlayerPayload trackedPayload(LivingEntity entity) {
        SecondWindEntityState state = SecondWindEntityService.getState(entity);
        ResolvedEntityPolicy policy = state.policy();
        return new ClientboundTrackedDownedPlayerPayload(entity.getId(), state.isDowned(), policy != null && policy.showTimer(),
                state.ticksRemaining(), state.maxTicks(), state.reviveChannelReviver().isPresent(), policy != null && policy.reviveEnabled(),
                policy == null ? 0 : policy.reviveChannelTicks(), policy == null ? 0.0D : policy.reviveDistance(),
                policy == null ? ResourceLocation.fromNamespaceAndPath(SecondWindMod.MOD_ID, "crawl") : policy.pose());
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
            if (reviver.serverLevel().getEntity(payload.targetEntityId()) instanceof LivingEntity target) {
                SecondWindEntityService.refreshReviveChannel(reviver, target);
            }
        });
    }
}
