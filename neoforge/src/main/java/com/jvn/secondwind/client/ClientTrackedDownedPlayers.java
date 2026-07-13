package com.jvn.secondwind.client;

import com.jvn.secondwind.network.ClientboundTrackedDownedPlayerPayload;
import com.jvn.secondwind.api.SecondWindClientApi;
import com.jvn.secondwind.api.TrackedDownedEntityState;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class ClientTrackedDownedPlayers {
    private static final Map<Integer, TrackedDownedPlayerState> STATES = new HashMap<>();

    private ClientTrackedDownedPlayers() {
    }

    static {
        SecondWindClientApi.installLookup(ClientTrackedDownedPlayers::publicState);
    }

    public static void apply(ClientboundTrackedDownedPlayerPayload payload) {
        if (!payload.downed()) {
            STATES.remove(payload.entityId());
            return;
        }

        STATES.put(payload.entityId(), new TrackedDownedPlayerState(
                payload.timerVisible(),
                payload.ticksRemaining(),
                payload.maxTicks(),
                System.nanoTime(),
                payload.timerPaused(),
                payload.reviveEnabled(),
                payload.reviveChannelTicks(),
                payload.reviveDistance(),
                payload.pose()));
    }

    public static void tickClient() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            STATES.clear();
            return;
        }

        Iterator<Integer> iterator = STATES.keySet().iterator();
        while (iterator.hasNext()) {
            if (minecraft.level.getEntity(iterator.next()) == null) {
                iterator.remove();
            }
        }
    }

    public static float displayedTicksRemaining(int entityId) {
        TrackedDownedPlayerState state = STATES.get(entityId);
        if (state == null) {
            return 0.0F;
        }

        if (state.timerPaused()) {
            return state.ticksRemaining();
        }

        long elapsedNanos = Math.max(0L, System.nanoTime() - state.syncNanos());
        float elapsedTicks = elapsedNanos / 50_000_000.0F;
        return Math.max(0.0F, state.ticksRemaining() - elapsedTicks);
    }

    public static boolean isDowned(int entityId) {
        return STATES.containsKey(entityId);
    }

    public static boolean timerVisible(int entityId) {
        TrackedDownedPlayerState state = STATES.get(entityId);
        return state != null && state.timerVisible();
    }

    public static int reviveChannelTicks(int entityId) {
        TrackedDownedPlayerState state = STATES.get(entityId);
        return state == null ? 0 : state.reviveChannelTicks();
    }

    public static boolean reviveEnabled(int entityId) {
        TrackedDownedPlayerState state = STATES.get(entityId);
        return state != null && state.reviveEnabled();
    }

    public static double reviveDistance(int entityId) {
        TrackedDownedPlayerState state = STATES.get(entityId);
        return state == null ? 0.0D : state.reviveDistance();
    }

    private static Optional<TrackedDownedEntityState> publicState(Entity entity) {
        TrackedDownedPlayerState state = STATES.get(entity.getId());
        if (state == null) return Optional.empty();
        return Optional.of(new TrackedDownedEntityState(true, state.timerVisible(), Math.round(displayedTicksRemaining(entity.getId())),
                state.maxTicks(), state.timerPaused(), state.reviveEnabled(), state.reviveChannelTicks(), state.reviveDistance(), state.pose()));
    }

    private record TrackedDownedPlayerState(boolean timerVisible, int ticksRemaining, int maxTicks, long syncNanos,
                                            boolean timerPaused, boolean reviveEnabled, int reviveChannelTicks, double reviveDistance,
                                            net.minecraft.resources.ResourceLocation pose) {
    }
}
