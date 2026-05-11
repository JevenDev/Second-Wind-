package com.jvn.secondwind.client;

import com.jvn.secondwind.network.ClientboundSecondWindStatePayload;

public final class ClientSecondWindState {
    private static boolean downed;
    private static int ticksRemaining;
    private static int maxTicks;
    private static boolean giveUpAvailable;
    private static float reviveProgress;
    private static int cooldownSeconds;

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
}
