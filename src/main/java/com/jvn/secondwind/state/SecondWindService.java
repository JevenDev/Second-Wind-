package com.jvn.secondwind.state;

import com.jvn.secondwind.config.CooldownMode;
import com.jvn.secondwind.config.SecondWindConfig;
import com.jvn.secondwind.util.SecondWindDamageSources;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

public final class SecondWindService {
    public static final int TICKS_PER_SECOND = 20;
    public static final long TICKS_PER_MC_DAY = 24000L;

    private SecondWindService() {
    }

    public static SecondWindPlayerState getState(ServerPlayer player) {
        return player.getData(SecondWindData.PLAYER_STATE);
    }

    public static boolean isDowned(ServerPlayer player) {
        return getState(player).isDowned();
    }

    public static boolean canTriggerSecondWind(ServerPlayer player, DamageSource damageSource) {
        SecondWindPlayerState state = getState(player);
        return !state.isDowned()
                && !state.isForcedDeathFlow()
                && !player.isCreative()
                && !player.isSpectator()
                && !isCooldownActive(player)
                && SecondWindDamageSources.canTriggerSecondWind(damageSource);
    }

    public static void enterDowned(ServerPlayer player, DamageSource damageSource) {
        SecondWindPlayerState state = getState(player);
        int timerSeconds = Math.max(
                SecondWindConfig.MINIMUM_DOWNED_TIMER_SECONDS.get(),
                SecondWindConfig.DOWNED_TIMER_SECONDS.get()
                        - state.getDownPenaltyCount() * SecondWindConfig.TIMER_PENALTY_PER_DOWN.get());
        int ticks = timerSeconds * TICKS_PER_SECOND;
        state.setDowned(true);
        state.setDownedMaxTicks(ticks);
        state.setDownedTicksRemaining(ticks);
        state.setDownedStartGameTime(player.serverLevel().getGameTime());
        state.setForcedDeathFlow(false);
        state.clearReviveChannel();
    }

    public static void revive(ServerPlayer player, ReviveReason reason) {
        SecondWindPlayerState state = getState(player);
        state.clearDownedRuntime();
        state.incrementDownPenaltyCount();
        applyCooldown(player);
    }

    public static void failDowned(ServerPlayer player, FailureReason reason) {
        SecondWindPlayerState state = getState(player);
        state.clearDownedRuntime();
        state.incrementDownPenaltyCount();
        state.setForcedDeathFlow(true);
        applyCooldown(player);
    }

    public static void applyCooldown(ServerPlayer player) {
        SecondWindPlayerState state = getState(player);
        CooldownMode mode = SecondWindConfig.COOLDOWN_MODE.get();
        long gameTime = player.serverLevel().getGameTime();
        long day = currentMcDay(player);

        switch (mode) {
            case NONE -> {
                state.setCooldownExpiresGameTime(0L);
                state.setConsumedToday(false);
                state.setConsumedSinceSleep(false);
            }
            case TIMED -> {
                int seconds = SecondWindConfig.COOLDOWN_DURATION_SECONDS.get();
                state.setCooldownExpiresGameTime(seconds <= 0 ? 0L : gameTime + seconds * (long) TICKS_PER_SECOND);
                state.setConsumedToday(false);
                state.setConsumedSinceSleep(false);
            }
            case MC_DAY -> {
                state.setCooldownExpiresGameTime(0L);
                state.setLastMcDayUsed(day);
                state.setConsumedToday(true);
                state.setConsumedSinceSleep(false);
            }
            case ON_SLEEP -> {
                state.setCooldownExpiresGameTime(0L);
                state.setConsumedToday(false);
                state.setConsumedSinceSleep(true);
            }
        }
        state.setPendingUnsafeExitCooldown(false);
    }

    public static boolean isCooldownActive(ServerPlayer player) {
        SecondWindPlayerState state = getState(player);
        resetCooldownForNewDayIfNeeded(player);
        return switch (SecondWindConfig.COOLDOWN_MODE.get()) {
            case NONE -> false;
            case TIMED -> state.getCooldownExpiresGameTime() > player.serverLevel().getGameTime();
            case MC_DAY -> state.hasConsumedToday() && state.getLastMcDayUsed() == currentMcDay(player);
            case ON_SLEEP -> state.hasConsumedSinceSleep();
        };
    }

    public static void resetCooldownForSleep(ServerPlayer player) {
        SecondWindPlayerState state = getState(player);
        state.setConsumedSinceSleep(false);
        state.setCooldownExpiresGameTime(0L);
        state.setDownPenaltyCount(0);
        state.setPendingUnsafeExitCooldown(false);
    }

    public static void resetCooldownForNewDayIfNeeded(ServerPlayer player) {
        SecondWindPlayerState state = getState(player);
        if (SecondWindConfig.COOLDOWN_MODE.get() != CooldownMode.MC_DAY) {
            return;
        }

        long day = currentMcDay(player);
        if (state.getLastMcDayUsed() >= 0L && state.getLastMcDayUsed() < day) {
            state.setConsumedToday(false);
            state.setLastMcDayUsed(day);
            state.setDownPenaltyCount(0);
        }
    }

    public static void handleUnsafeExitIfNeeded(ServerPlayer player) {
        SecondWindPlayerState state = getState(player);
        if (state.hasPendingUnsafeExitCooldown()) {
            state.clearDownedRuntime();
            state.incrementDownPenaltyCount();
            applyCooldown(player);
        }
    }

    private static long currentMcDay(ServerPlayer player) {
        return player.serverLevel().getDayTime() / TICKS_PER_MC_DAY;
    }
}
