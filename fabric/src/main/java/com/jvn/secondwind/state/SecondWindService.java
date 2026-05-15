package com.jvn.secondwind.state;

import com.jvn.secondwind.advancement.SecondWindCriteria;
import com.jvn.secondwind.config.CooldownMode;
import com.jvn.secondwind.config.SecondWindConfig;
import com.jvn.secondwind.network.SecondWindNetworking;
import com.jvn.secondwind.util.SecondWindDamageSources;
import com.jvn.secondwind.util.SecondWindEntityRules;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Pose;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import org.joml.Vector3f;

public final class SecondWindService {
    public static final int TICKS_PER_SECOND = 20;
    public static final long TICKS_PER_MC_DAY = 24000L;
    private static final float DOWNED_SAFE_HEALTH = 1.0F;
    private static final int DOWNED_SLOWNESS_REFRESH_TICKS = 10;
    private static final int LAST_SECOND_REVIVE_TICKS = Math.max(1, TICKS_PER_SECOND / 10);
    private static final int REVIVE_HOLD_GRACE_TICKS = 2;
    private static final int DOWNED_ANNOUNCEMENT_VARIANTS = 7;
    private static final int PLAYER_REVIVE_ANNOUNCEMENT_VARIANTS = 7;
    private static final int KILL_REVIVE_ANNOUNCEMENT_VARIANTS = 7;
    private static final int ADMIN_REVIVE_ANNOUNCEMENT_VARIANTS = 2;
    private static final Vector3f REVIVE_PARTICLE_PURPLE = new Vector3f(0.73F, 0.42F, 0.98F);
    private static final Vector3f REVIVE_PARTICLE_WHITE = new Vector3f(0.98F, 0.96F, 1.0F);

    private SecondWindService() {
    }

    public static SecondWindPlayerState getState(ServerPlayer player) {
        return ((AttachmentTarget) player).getAttachedOrCreate(SecondWindData.PLAYER_STATE);
    }

    public static boolean isDowned(ServerPlayer player) {
        return getState(player).isDowned();
    }

    public static boolean canEnterDownedState(ServerPlayer player) {
        return !isDowned(player) && !player.isCreative() && !player.isSpectator();
    }

    public static boolean down(ServerPlayer player, DamageSource damageSource) {
        if (!canEnterDownedState(player)) {
            return false;
        }

        enterDowned(player, damageSource);
        player.setHealth(DOWNED_SAFE_HEALTH);
        player.fallDistance = 0.0F;
        player.setDeltaMovement(player.getDeltaMovement().multiply(0.15D, 0.0D, 0.15D));

        if (SecondWindConfig.ENABLE_SOUNDS.get()) {
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.8F, 0.6F);
        }

        announcePlayerDowned(player);
        SecondWindNetworking.syncToPlayer(player);
        return true;
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
        state.setLastDownedDamageGameTime(0L);
        state.setDownedByPlayer(SecondWindEntityRules.findCreditedPlayer(damageSource)
            .filter(attacker -> attacker != player)
            .map(ServerPlayer::getUUID)
            .orElse(null));
        state.setOriginalDownedDamageSource(damageSource);
        state.setOriginalDownedDeathMessage(damageSource.getLocalizedDeathMessage(player).getString());
        state.setForcedDeathFlow(false);
        state.clearReviveChannel();
        applyDownedMobilityEffects(player);
    }

    public static void revive(ServerPlayer player, ReviveReason reason) {
        SecondWindPlayerState state = getState(player);
        int remainingTicks = state.getDownedTicksRemaining();
        ServerPlayer reviver = state.getReviveChannelReviver()
                .map(player.server.getPlayerList()::getPlayer)
                .orElse(null);
        ServerPlayer downer = state.getDownedByPlayer()
            .map(player.server.getPlayerList()::getPlayer)
            .orElse(null);
        state.clearDownedRuntime();
        clearDownedMobilityEffects(player);
        state.incrementDownPenaltyCount();
        applyReviveHealthAndEffects(player);
        applyCooldown(player);
        spawnRevivePopParticles(player);
        announcePlayerRevived(player, reason);
        SecondWindCriteria.triggerRevive(player, reason, remainingTicks, LAST_SECOND_REVIVE_TICKS, reviver, downer);
        SecondWindNetworking.syncToPlayer(player, true);
    }

    public static void failDowned(ServerPlayer player, FailureReason reason) {
        SecondWindPlayerState state = getState(player);
        state.clearDownedRuntime();
        clearDownedMobilityEffects(player);
        state.incrementDownPenaltyCount();
        state.setForcedDeathFlow(true);
        applyCooldown(player);
    }

    public static void failAndKill(ServerPlayer player, FailureReason reason) {
        SecondWindPlayerState state = getState(player);
        DamageSource damageSource = SecondWindDamageSources.failureSource(player, state, reason);
        failDowned(player, reason);
        player.setHealth(1.0F);
        player.invulnerableTime = 0;
        if (!player.hurt(damageSource, Float.MAX_VALUE)) {
            player.kill();
        }
        SecondWindNetworking.syncToPlayer(player);
    }

    public static void tickDowned(ServerPlayer player) {
        SecondWindPlayerState state = getState(player);
        if (!state.isDowned()) {
            resetCooldownForNewDayIfNeeded(player);
            return;
        }

        applyDownedMobilityEffects(player);
        if (SecondWindConfig.BLOCK_HEALING_WHILE_DOWNED.get() && player.getHealth() > DOWNED_SAFE_HEALTH) {
            player.setHealth(DOWNED_SAFE_HEALTH);
        }
        tickReviveChannel(player, state);
        if (!state.isDowned()) {
            return;
        }

        if (isReviveChannelActive(state)) {
            return;
        }

        int remaining = state.getDownedTicksRemaining() - 1;
        state.setDownedTicksRemaining(remaining);
        if (remaining <= 0) {
            failAndKill(player, FailureReason.TIMER_EXPIRED);
        } else if (remaining % TICKS_PER_SECOND == 0 || remaining <= 60) {
            SecondWindNetworking.syncToPlayer(player);
        }
    }

    public static void enforceDownedMovement(ServerPlayer player) {
        if (isDowned(player)) {
            applyDownedMobilityEffects(player);
        }
    }

    public static boolean canPlayerRevive(ServerPlayer reviver, ServerPlayer downedPlayer) {
        return SecondWindConfig.MULTIPLAYER_REVIVE.get()
                && reviver != downedPlayer
                && !reviver.isCreative()
                && !reviver.isSpectator()
                && !isDowned(reviver)
                && isDowned(downedPlayer)
                && isWithinReviveDistance(reviver, downedPlayer);
    }

    public static boolean refreshReviveChannel(ServerPlayer reviver, ServerPlayer downedPlayer) {
        if (!canPlayerRevive(reviver, downedPlayer)) {
            return false;
        }

        SecondWindPlayerState state = getState(downedPlayer);
        int requiredTicks = (int) Math.ceil(SecondWindConfig.REVIVE_CHANNEL_SECONDS.get() * TICKS_PER_SECOND);
        if (requiredTicks <= 0) {
            state.setReviveChannel(reviver.getUUID(), 0);
            revive(downedPlayer, ReviveReason.PLAYER_REVIVE);
            return true;
        }

        long gameTime = downedPlayer.serverLevel().getGameTime();
        if (state.getReviveChannelReviver().filter(reviver.getUUID()::equals).isEmpty()) {
            if (state.getReviveChannelReviver().isPresent()
                    && state.getReviveChannelLastHoldGameTime() >= gameTime - REVIVE_HOLD_GRACE_TICKS) {
                return false;
            }

            state.setReviveChannel(reviver.getUUID(), requiredTicks);
            SecondWindNetworking.syncToPlayer(downedPlayer);
        }

        state.setReviveChannelLastHoldGameTime(gameTime);
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

    public static boolean isBeingRevived(ServerPlayer player) {
        return isReviveChannelActive(getState(player));
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

    public static void resetCooldownAfterDeath(ServerPlayer player) {
        SecondWindPlayerState state = getState(player);
        state.setCooldownExpiresGameTime(0L);
        state.setCooldownExpiresEpochMillis(0L);
        state.setLastMcDayUsed(-1L);
        state.setConsumedToday(false);
        state.setConsumedSinceSleep(false);
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
            state.setPendingUnsafeExitCooldown(false);
            failAndKill(player, FailureReason.LOGOUT_WHILE_DOWNED);
        }
    }

    public static void markUnsafeExitWhileDowned(ServerPlayer player) {
        SecondWindPlayerState state = getState(player);
        if (!state.isDowned()) {
            return;
        }

        String originalDownedDeathMessage = state.getOriginalDownedDeathMessage();
        state.clearDownedRuntime();
        clearDownedMobilityEffects(player);
        state.setOriginalDownedDeathMessage(originalDownedDeathMessage);
        state.setForcedDeathFlow(false);
        state.setPendingUnsafeExitCooldown(true);
    }

    public static boolean applyDownedDamageToTimer(ServerPlayer player, float damageAmount) {
        return applyDownedDamageToTimer(player, null, damageAmount);
    }

    public static boolean applyDownedDamageToTimer(ServerPlayer player, DamageSource damageSource, float damageAmount) {
        SecondWindPlayerState state = getState(player);
        if (!state.isDowned() || damageAmount <= 0.0F) {
            return false;
        }

        int cooldownTicks = SecondWindConfig.DOWNED_DAMAGE_COOLDOWN_TICKS.get();
        long gameTime = player.serverLevel().getGameTime();
        if (cooldownTicks > 0 && state.getLastDownedDamageGameTime() > 0L
                && gameTime - state.getLastDownedDamageGameTime() < cooldownTicks) {
            return false;
        }

        int timerPenaltyTicks = Math.max(1, Math.round(damageAmount * TICKS_PER_SECOND));
        int remainingTicks = state.getDownedTicksRemaining() - timerPenaltyTicks;
        state.setDownedTicksRemaining(remainingTicks);
        state.setLastDownedDamageGameTime(gameTime);

        if (remainingTicks <= 0) {
            if (damageSource != null) {
                SecondWindEntityRules.findCreditedPlayer(damageSource)
                        .filter(attacker -> attacker != player)
                        .ifPresent(SecondWindCriteria::triggerFinishHim);
            }
            failAndKill(player, FailureReason.TIMER_EXPIRED);
            return true;
        }

        SecondWindNetworking.syncToPlayer(player);
        return false;
    }

    private static long currentMcDay(ServerPlayer player) {
        return player.serverLevel().getDayTime() / TICKS_PER_MC_DAY;
    }

    private static void announcePlayerDowned(ServerPlayer player) {
        int variant = player.getRandom().nextInt(DOWNED_ANNOUNCEMENT_VARIANTS);
        Component message = Component.translatable("message.secondwind.downed." + variant, player.getDisplayName())
                .withStyle(ChatFormatting.RED);
        player.server.getPlayerList().broadcastSystemMessage(message, false);
    }

    private static void announcePlayerRevived(ServerPlayer player, ReviveReason reason) {
        int variantBound = switch (reason) {
            case PLAYER_REVIVE -> PLAYER_REVIVE_ANNOUNCEMENT_VARIANTS;
            case KILL -> KILL_REVIVE_ANNOUNCEMENT_VARIANTS;
            case ADMIN -> ADMIN_REVIVE_ANNOUNCEMENT_VARIANTS;
        };
        int variant = player.getRandom().nextInt(variantBound);
        String reasonKey = switch (reason) {
            case PLAYER_REVIVE -> "player_revive";
            case KILL -> "kill";
            case ADMIN -> "admin";
        };
        Component message = Component.translatable("message.secondwind.revived." + reasonKey + "." + variant, player.getDisplayName())
            .withStyle(ChatFormatting.GREEN);
        player.server.getPlayerList().broadcastSystemMessage(message, false);
    }

    private static void spawnRevivePopParticles(ServerPlayer player) {
        double centerX = player.getX();
        double centerY = player.getY() + player.getBbHeight() * 0.55D;
        double centerZ = player.getZ();
        double width = player.getBbWidth() * 0.45D;
        double height = player.getBbHeight() * 0.35D;

        DustColorTransitionOptions purpleToWhite = new DustColorTransitionOptions(
                REVIVE_PARTICLE_PURPLE,
                REVIVE_PARTICLE_WHITE,
                1.35F);
        DustColorTransitionOptions whiteToPurple = new DustColorTransitionOptions(
                REVIVE_PARTICLE_WHITE,
                REVIVE_PARTICLE_PURPLE,
                0.95F);

        player.serverLevel().sendParticles(purpleToWhite, centerX, centerY, centerZ, 28, width, height, width, 0.08D);
        player.serverLevel().sendParticles(whiteToPurple, centerX, centerY + 0.2D, centerZ, 16, width * 0.65D, height * 0.8D, width * 0.65D, 0.02D);
        player.serverLevel().sendParticles(ParticleTypes.END_ROD, centerX, centerY + 0.1D, centerZ, 10, width * 0.4D, height * 0.75D, width * 0.4D, 0.02D);
    }

    private static void tickReviveChannel(ServerPlayer downedPlayer, SecondWindPlayerState state) {
        if (state.getReviveChannelReviver().isEmpty()) {
            return;
        }

        ServerPlayer reviver = downedPlayer.server.getPlayerList().getPlayer(state.getReviveChannelReviver().get());
        long gameTime = downedPlayer.serverLevel().getGameTime();
        if (reviver == null
                || reviver.isRemoved()
                || reviver.isSpectator()
                || isDowned(reviver)
                || !isWithinReviveDistance(reviver, downedPlayer)
                || state.getReviveChannelLastHoldGameTime() < gameTime - REVIVE_HOLD_GRACE_TICKS) {
            state.clearReviveChannel();
            SecondWindNetworking.syncToPlayer(downedPlayer);
            return;
        }

        state.setReviveChannelTicks(state.getReviveChannelTicks() + 1);

        if (state.getReviveChannelTicks() >= state.getReviveChannelRequiredTicks()) {
            revive(downedPlayer, ReviveReason.PLAYER_REVIVE);
            reviver.displayClientMessage(Component.literal("Revived"), true);
        } else if (state.getReviveChannelTicks() % 5 == 0) {
            SecondWindNetworking.syncToPlayer(downedPlayer);
        }
    }

    private static boolean isReviveChannelActive(SecondWindPlayerState state) {
        return state.getReviveChannelReviver().isPresent();
    }

    private static boolean isWithinReviveDistance(ServerPlayer reviver, ServerPlayer downedPlayer) {
        double maxDistance = SecondWindConfig.REVIVE_DISTANCE.get();
        return reviver.distanceToSqr(downedPlayer) <= maxDistance * maxDistance;
    }

    private static void applyDownedMobilityEffects(ServerPlayer player) {
        int slownessLevel = SecondWindConfig.DOWNED_SLOWNESS_LEVEL.get();
        if (slownessLevel > 0) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    DOWNED_SLOWNESS_REFRESH_TICKS,
                    slownessLevel - 1,
                    false,
                    false,
                    false));
        }

        player.setPose(Pose.SWIMMING);
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

    private static void clearDownedMobilityEffects(ServerPlayer player) {
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.setPose(Pose.STANDING);
        player.setSprinting(false);
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
