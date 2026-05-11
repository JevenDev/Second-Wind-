package com.jvn.secondwind.state;

import com.jvn.secondwind.config.CooldownMode;
import com.jvn.secondwind.config.SecondWindConfig;
import com.jvn.secondwind.network.SecondWindNetworking;
import com.jvn.secondwind.util.SecondWindDamageSources;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

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
        applyReviveHealthAndEffects(player);
        applyCooldown(player);
        SecondWindNetworking.syncToPlayer(player);
    }

    public static void failDowned(ServerPlayer player, FailureReason reason) {
        SecondWindPlayerState state = getState(player);
        state.clearDownedRuntime();
        state.incrementDownPenaltyCount();
        state.setForcedDeathFlow(true);
        applyCooldown(player);
    }

    public static void failAndKill(ServerPlayer player, FailureReason reason) {
        failDowned(player, reason);
        player.setHealth(1.0F);
        player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
        SecondWindNetworking.syncToPlayer(player);
    }

    public static void tickDowned(ServerPlayer player) {
        SecondWindPlayerState state = getState(player);
        if (!state.isDowned()) {
            resetCooldownForNewDayIfNeeded(player);
            return;
        }

        restrictDownedMovement(player);
        tickReviveChannel(player, state);
        int remaining = state.getDownedTicksRemaining() - 1;
        state.setDownedTicksRemaining(remaining);
        if (remaining <= 0) {
            failAndKill(player, FailureReason.TIMER_EXPIRED);
        } else if (remaining % TICKS_PER_SECOND == 0 || remaining <= 60) {
            SecondWindNetworking.syncToPlayer(player);
        }
    }

    public static boolean startReviveChannel(ServerPlayer reviver, ServerPlayer downedPlayer) {
        if (!SecondWindConfig.MULTIPLAYER_REVIVE.get()
                || reviver == downedPlayer
                || reviver.isCreative()
                || reviver.isSpectator()
                || !isDowned(downedPlayer)
                || isDowned(reviver)
                || !isWithinReviveDistance(reviver, downedPlayer)) {
            return false;
        }

        int requiredTicks = (int) Math.ceil(SecondWindConfig.REVIVE_CHANNEL_SECONDS.get() * TICKS_PER_SECOND);
        if (requiredTicks <= 0) {
            revive(downedPlayer, ReviveReason.PLAYER_REVIVE);
            return true;
        }

        SecondWindPlayerState state = getState(downedPlayer);
        state.setReviveChannel(reviver.getUUID(), requiredTicks);
        reviver.displayClientMessage(Component.literal("Reviving..."), true);
        SecondWindNetworking.syncToPlayer(downedPlayer);
        return true;
    }

    public static void interruptReviveChannelsFor(ServerPlayer player) {
        if (isDowned(player)) {
            SecondWindPlayerState state = getState(player);
            if (state.getReviveChannelReviver().isPresent()) {
                state.clearReviveChannel();
                SecondWindNetworking.syncToPlayer(player);
            }
            return;
        }

        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            SecondWindPlayerState state = getState(other);
            if (state.getReviveChannelReviver().filter(player.getUUID()::equals).isPresent()) {
                state.clearReviveChannel();
                player.displayClientMessage(Component.literal("Revive interrupted"), true);
                SecondWindNetworking.syncToPlayer(other);
            }
        }
    }

    public static void applyCooldown(ServerPlayer player) {
        SecondWindPlayerState state = getState(player);
        CooldownMode mode = SecondWindConfig.COOLDOWN_MODE.get();
        long gameTime = player.serverLevel().getGameTime();
        long day = currentMcDay(player);

        switch (mode) {
            case NONE -> {
                state.setCooldownExpiresGameTime(0L);
                state.setCooldownExpiresEpochMillis(0L);
                state.setConsumedToday(false);
                state.setConsumedSinceSleep(false);
            }
            case TIMED -> {
                int seconds = SecondWindConfig.COOLDOWN_DURATION_SECONDS.get();
                state.setCooldownExpiresGameTime(seconds <= 0 ? 0L : gameTime + seconds * (long) TICKS_PER_SECOND);
                state.setCooldownExpiresEpochMillis(seconds <= 0 ? 0L : System.currentTimeMillis() + seconds * 1000L);
                state.setConsumedToday(false);
                state.setConsumedSinceSleep(false);
            }
            case MC_DAY -> {
                state.setCooldownExpiresGameTime(0L);
                state.setCooldownExpiresEpochMillis(0L);
                state.setLastMcDayUsed(day);
                state.setConsumedToday(true);
                state.setConsumedSinceSleep(false);
            }
            case ON_SLEEP -> {
                state.setCooldownExpiresGameTime(0L);
                state.setCooldownExpiresEpochMillis(0L);
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
            case TIMED -> state.getCooldownExpiresEpochMillis() > System.currentTimeMillis()
                    || state.getCooldownExpiresGameTime() > player.serverLevel().getGameTime();
            case MC_DAY -> state.hasConsumedToday() && state.getLastMcDayUsed() == currentMcDay(player);
            case ON_SLEEP -> state.hasConsumedSinceSleep();
        };
    }

    public static int getCooldownRemainingSeconds(ServerPlayer player) {
        SecondWindPlayerState state = getState(player);
        return switch (SecondWindConfig.COOLDOWN_MODE.get()) {
            case NONE -> 0;
            case TIMED -> {
                long millisRemaining = Math.max(0L, state.getCooldownExpiresEpochMillis() - System.currentTimeMillis());
                long ticksRemaining = Math.max(0L, state.getCooldownExpiresGameTime() - player.serverLevel().getGameTime());
                yield (int) Math.max((millisRemaining + 999L) / 1000L, (ticksRemaining + TICKS_PER_SECOND - 1L) / TICKS_PER_SECOND);
            }
            case MC_DAY -> isCooldownActive(player)
                    ? (int) (Math.max(1L, TICKS_PER_MC_DAY - player.serverLevel().getDayTime() % TICKS_PER_MC_DAY) / TICKS_PER_SECOND)
                    : 0;
            case ON_SLEEP -> state.hasConsumedSinceSleep() ? -1 : 0;
        };
    }

    public static void resetCooldownForSleep(ServerPlayer player) {
        SecondWindPlayerState state = getState(player);
        state.setConsumedSinceSleep(false);
        state.setCooldownExpiresGameTime(0L);
        state.setCooldownExpiresEpochMillis(0L);
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

    private static void tickReviveChannel(ServerPlayer downedPlayer, SecondWindPlayerState state) {
        if (state.getReviveChannelReviver().isEmpty()) {
            return;
        }

        ServerPlayer reviver = downedPlayer.server.getPlayerList().getPlayer(state.getReviveChannelReviver().get());
        if (reviver == null || reviver.isRemoved() || reviver.isSpectator() || isDowned(reviver) || !isWithinReviveDistance(reviver, downedPlayer)) {
            state.clearReviveChannel();
            SecondWindNetworking.syncToPlayer(downedPlayer);
            return;
        }

        state.setReviveChannelTicks(state.getReviveChannelTicks() + 1);
        int percent = Math.round(state.getReviveChannelProgress() * 100.0F);
        reviver.displayClientMessage(Component.literal("Reviving " + percent + "%"), true);

        if (state.getReviveChannelTicks() >= state.getReviveChannelRequiredTicks()) {
            revive(downedPlayer, ReviveReason.PLAYER_REVIVE);
            reviver.displayClientMessage(Component.literal("Revived"), true);
        } else if (state.getReviveChannelTicks() % 5 == 0) {
            SecondWindNetworking.syncToPlayer(downedPlayer);
        }
    }

    private static boolean isWithinReviveDistance(ServerPlayer reviver, ServerPlayer downedPlayer) {
        double maxDistance = SecondWindConfig.REVIVE_DISTANCE.get();
        return reviver.distanceToSqr(downedPlayer) <= maxDistance * maxDistance;
    }

    private static void restrictDownedMovement(ServerPlayer player) {
        double multiplier = SecondWindConfig.DOWNED_MOVEMENT_MULTIPLIER.get();
        Vec3 motion = player.getDeltaMovement();
        double y = motion.y > 0.0D ? motion.y * 0.15D : motion.y;
        player.setDeltaMovement(motion.x * multiplier, y, motion.z * multiplier);
        player.setSprinting(false);
        player.fallDistance = 0.0F;
        if (player.getAbilities().flying) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        if (player.isFallFlying()) {
            player.stopFallFlying();
        }
    }

    private static void applyReviveHealthAndEffects(ServerPlayer player) {
        float maxHealth = player.getMaxHealth();
        float targetHealth = Math.min(maxHealth, SecondWindConfig.REVIVE_HEALTH_HALF_HEARTS.get().floatValue());
        int regenTicks = SecondWindConfig.REVIVE_REGENERATION_SECONDS.get() * TICKS_PER_SECOND;
        float initialHealth = regenTicks > 0 ? Math.max(4.0F, targetHealth * 0.5F) : targetHealth;
        player.setHealth(Math.min(maxHealth, Math.max(player.getHealth(), initialHealth)));
        if (regenTicks > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, regenTicks, 1));
        }

        int invulnerableTicks = SecondWindConfig.POST_REVIVE_INVULNERABILITY_SECONDS.get() * TICKS_PER_SECOND;
        if (invulnerableTicks > 0) {
            player.invulnerableTime = Math.max(player.invulnerableTime, invulnerableTicks);
        }

        if (SecondWindConfig.ENABLE_SOUNDS.get()) {
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.4F);
        }
    }
}
