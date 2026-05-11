package com.jvn.secondwind.client;

import com.jvn.secondwind.network.ClientboundSecondWindStatePayload;
import net.minecraft.client.Minecraft;

public final class ClientSecondWindState {
    private static boolean downed;
    private static int ticksRemaining;
    private static int maxTicks;
    private static boolean giveUpAvailable;
    private static float reviveProgress;
    private static int cooldownSeconds;
    private static int revivedFlashTicks;
    private static long ticksRemainingSyncNanos;
    private static long pausedElapsedNanos;
    private static long pauseStartedNanos = -1L;

    private ClientSecondWindState() {
    }

    public static boolean isDowned() {
        return downed;
    }

    public static void setDowned(boolean downed) {
        ClientSecondWindState.downed = downed;
    }

    public static void apply(ClientboundSecondWindStatePayload payload) {
        downed = payload.downed();
        if (payload.showReviveFlash()) {
            revivedFlashTicks = 50;
        }
        ticksRemaining = payload.ticksRemaining();
        maxTicks = payload.maxTicks();
        giveUpAvailable = payload.giveUpAvailable();
        reviveProgress = payload.reviveProgress();
        cooldownSeconds = payload.cooldownSeconds();
        ticksRemainingSyncNanos = System.nanoTime();
        pausedElapsedNanos = 0L;
        pauseStartedNanos = -1L;
    }

    public static int ticksRemaining() {
        return ticksRemaining;
    }

    public static float displayedTicksRemaining() {
        if (!downed) {
            return 0.0F;
        }

        long now = System.nanoTime();
        if (shouldFreezeForLocalPause()) {
            if (pauseStartedNanos < 0L) {
                pauseStartedNanos = now;
            }
        } else if (pauseStartedNanos >= 0L) {
            pausedElapsedNanos += now - pauseStartedNanos;
            pauseStartedNanos = -1L;
        }

        long effectiveNow = pauseStartedNanos >= 0L ? pauseStartedNanos : now;
        long elapsedNanos = Math.max(0L, effectiveNow - ticksRemainingSyncNanos - pausedElapsedNanos);
        float elapsedTicks = elapsedNanos / 50_000_000.0F;
        return Math.max(0.0F, ticksRemaining - elapsedTicks);
    }

    public static int maxTicks() {
        return maxTicks;
    }

    public static boolean giveUpAvailable() {
        return giveUpAvailable;
    }

    public static float reviveProgress() {
        return reviveProgress;
    }

    public static int cooldownSeconds() {
        return cooldownSeconds;
    }

    public static int revivedFlashTicks() {
        return revivedFlashTicks;
    }

    public static void tickClient() {
        if (revivedFlashTicks > 0) {
            revivedFlashTicks--;
        }
    }

    private static boolean shouldFreezeForLocalPause() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.isPaused()
                && minecraft.hasSingleplayerServer()
                && minecraft.getSingleplayerServer() != null
                && !minecraft.getSingleplayerServer().isPublished();
    }
}
