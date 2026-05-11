package com.jvn.secondwind.client;

public final class ClientSecondWindState {
    private static boolean downed;

    private ClientSecondWindState() {
    }

    public static boolean isDowned() {
        return downed;
    }

    public static void setDowned(boolean downed) {
        ClientSecondWindState.downed = downed;
    }
}
