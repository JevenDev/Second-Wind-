package com.jvn.secondwind.client;

import com.jvn.secondwind.network.ClientboundTrackedDownedPlayerPayload;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;

public final class ClientTrackedDownedPlayers {
    private static final Map<Integer, TrackedDownedPlayerState> STATES = new HashMap<>();

    private ClientTrackedDownedPlayers() {
    }

    public static void apply(ClientboundTrackedDownedPlayerPayload payload) {
        if (!payload.downed()) {
            STATES.remove(payload.entityId());
            return;
        }

        STATES.put(payload.entityId(), new TrackedDownedPlayerState(payload.ticksRemaining(), payload.maxTicks(), System.nanoTime()));
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

        long elapsedNanos = Math.max(0L, System.nanoTime() - state.syncNanos());
        float elapsedTicks = elapsedNanos / 50_000_000.0F;
        return Math.max(0.0F, state.ticksRemaining() - elapsedTicks);
    }

    public static boolean isDowned(int entityId) {
        return STATES.containsKey(entityId);
    }

    private record TrackedDownedPlayerState(int ticksRemaining, int maxTicks, long syncNanos) {
    }
}