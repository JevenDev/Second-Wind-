package com.jvn.secondwind.client;

import com.jvn.secondwind.network.ClientboundSecondWindStatePayload;

public final class ClientSecondWindState {
    private static boolean downed;
    private static int ticksRemaining;
    private static int maxTicks;
    private static boolean giveUpAvailable;
    private static float reviveProgress;
    private static int cooldownSeconds;
    private static int revivedFlashTicks;

    private ClientSecondWindState() {
    }

    public static boolean isDowned() {
        return downed;
    }

    public static void setDowned(boolean downed) {
        ClientSecondWindState.downed = downed;
    }

    public static void apply(ClientboundSecondWindStatePayload payload) {
        boolean wasDowned = downed;
        downed = payload.downed();
        if (wasDowned && !downed) {
            revivedFlashTicks = 50;
        }
        ticksRemaining = payload.ticksRemaining();
        maxTicks = payload.maxTicks();
        giveUpAvailable = payload.giveUpAvailable();
        reviveProgress = payload.reviveProgress();
        cooldownSeconds = payload.cooldownSeconds();
    }

    public static int ticksRemaining() {
        return ticksRemaining;
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
}
