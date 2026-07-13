package com.jvn.secondwind.api;

import net.minecraft.resources.ResourceLocation;

public record TrackedDownedEntityState(
        boolean downed,
        boolean timerVisible,
        int ticksRemaining,
        int maxTicks,
        boolean timerPaused,
        boolean reviveEnabled,
        int reviveChannelTicks,
        double reviveDistance,
        ResourceLocation pose) {
}
