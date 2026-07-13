package com.jvn.secondwind.client;

import com.jvn.secondwind.client.hud.SecondWindReviveFlashEffect;
import com.jvn.secondwind.config.SecondWindConfig;
import com.jvn.secondwind.network.ClientboundSecondWindStatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class ClientSecondWindState {
    private static final int REVIVE_OVERLAY_FADE_TICKS = 6;
    private static final long DOWNED_TIMER_DAMAGE_CATCHUP_NANOS = 700_000_000L;
    private static final long SIMPLE_TIMER_DAMAGE_FLASH_HOLD_NANOS = 120_000_000L;
    private static final long SIMPLE_TIMER_DAMAGE_FLASH_FADE_NANOS = 1_050_000_000L;
    private static boolean downed;
    private static int ticksRemaining;
    private static int maxTicks;
    private static float downedTimerDamageLostTicks;
    private static long downedTimerDamageStartNanos;
    private static long downedTimerDamagePausedElapsedNanos;
    private static long downedTimerDamagePauseStartedNanos = -1L;
    private static boolean giveUpAvailable;
    private static float reviveProgress;
    private static float displayedReviveProgress;
    private static boolean timerPaused;
    private static boolean forceCrawlingPose = true;
    private static String reviverName = "";
    private static String displayedReviverName = "";
    private static int cooldownSeconds;
    private static int revivedFlashTicks;
    private static long ticksRemainingSyncNanos;
    private static long pausedElapsedNanos;
    private static long pauseStartedNanos = -1L;
    private static long reviveProgressSyncNanos;
    private static int reviveOverlayFadeTicks;

    private ClientSecondWindState() {
    }

    public static boolean isDowned() {
        return downed;
    }

    public static void setDowned(boolean downed) {
        ClientSecondWindState.downed = downed;
    }

    public static void apply(ClientboundSecondWindStatePayload payload) {
        float currentDisplayedReviveProgress = displayedReviveProgress();
        boolean hadReviveOverlay = hasReviveOverlay();
        boolean wasDowned = downed;
        boolean newDowned = payload.downed();
        if (payload.showReviveFlash() && SecondWindConfig.ENABLE_SECOND_WIND_POPUP.get()) {
            revivedFlashTicks = SecondWindReviveFlashEffect.DURATION_TICKS;
            SecondWindReviveFlashEffect.beginActivation();
            SecondWindClient.playReviveItemActivation();
        } else if (!SecondWindConfig.ENABLE_SECOND_WIND_POPUP.get()) {
            revivedFlashTicks = 0;
        }
        ticksRemaining = payload.ticksRemaining();
        maxTicks = payload.maxTicks();
        downed = newDowned;
        giveUpAvailable = payload.giveUpAvailable();
        reviveProgress = payload.reviveProgress();
        timerPaused = payload.timerPaused();
        forceCrawlingPose = payload.forceCrawlingPose();
        reviverName = payload.reviverName();
        cooldownSeconds = payload.cooldownSeconds();
        ticksRemainingSyncNanos = System.nanoTime();
        pausedElapsedNanos = 0L;
        pauseStartedNanos = -1L;

        if (downed && wasDowned && payload.damageTicksLost() > 0) {
            beginDownedTimerDamage(payload.damageTicksLost());
        } else if (!downed || !wasDowned) {
            clearDownedTimerDamage();
        }

        if (!downed) {
            clearReviveOverlay();
            return;
        }

        if (reviveProgress > 0.0F) {
            displayedReviveProgress = reviveProgress;
            displayedReviverName = reviverName;
            reviveProgressSyncNanos = System.nanoTime();
            reviveOverlayFadeTicks = 0;
            return;
        }

        if (hadReviveOverlay && currentDisplayedReviveProgress > 0.0F) {
            displayedReviveProgress = currentDisplayedReviveProgress;
            displayedReviverName = displayedReviverName.isBlank() ? reviverName : displayedReviverName;
            reviveOverlayFadeTicks = REVIVE_OVERLAY_FADE_TICKS;
            return;
        }

        clearReviveOverlay();
    }

    public static int ticksRemaining() {
        return ticksRemaining;
    }

    public static float displayedTicksRemaining() {
        if (!downed) {
            return 0.0F;
        }

        long now = System.nanoTime();
        if (shouldFreezeDisplayedTimer()) {
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
        return Mth.clamp(ticksRemaining - elapsedTicks, 0.0F, maxTicks);
    }

    public static int maxTicks() {
        return maxTicks;
    }

    public static float downedTimerDamageStartTicks() {
        return displayedTicksRemaining();
    }

    public static float downedTimerDamageEndTicks() {
        return Mth.clamp(displayedTicksRemaining() + displayedDownedTimerDamageLostTicks(), 0.0F, maxTicks);
    }

    public static boolean hasDownedTimerDamage() {
        return displayedDownedTimerDamageLostTicks() > 0.5F;
    }

    public static float downedTimerDamageFlashStrength() {
        if (downedTimerDamageLostTicks <= 0.0F) {
            return 0.0F;
        }

        long elapsedNanos = downedTimerDamageElapsedNanos();
        if (elapsedNanos <= SIMPLE_TIMER_DAMAGE_FLASH_HOLD_NANOS) {
            return 1.0F;
        }

        float progress = Mth.clamp(
                (elapsedNanos - SIMPLE_TIMER_DAMAGE_FLASH_HOLD_NANOS) / (float) SIMPLE_TIMER_DAMAGE_FLASH_FADE_NANOS,
                0.0F,
                1.0F);
        return 1.0F - smoothstep(progress);
    }

    public static boolean giveUpAvailable() {
        return giveUpAvailable;
    }

    public static float reviveProgress() {
        return reviveProgress;
    }

    public static String reviverName() {
        return reviverName;
    }

    public static int cooldownSeconds() {
        return cooldownSeconds;
    }

    public static boolean forceCrawlingPose() {
        return forceCrawlingPose;
    }

    public static int revivedFlashTicks() {
        return revivedFlashTicks;
    }

    public static void tickClient() {
        if (shouldFreezeForLocalPause()) {
            return;
        }

        if (revivedFlashTicks > 0) {
            revivedFlashTicks--;
        }

        if (reviveOverlayFadeTicks > 0 && reviveProgress <= 0.0F) {
            reviveOverlayFadeTicks--;
            if (reviveOverlayFadeTicks <= 0) {
                clearReviveOverlay();
            }
        }
    }

    public static boolean hasReviveOverlay() {
        return displayedReviveProgress() > 0.0F;
    }

    public static float displayedReviveProgress() {
        if (reviveProgress > 0.0F) {
            int requiredTicks = reviveRequiredTicks();
            if (requiredTicks <= 0) {
                return 1.0F;
            }

            long elapsedNanos = Math.max(0L, System.nanoTime() - reviveProgressSyncNanos);
            float elapsedTicks = elapsedNanos / 50_000_000.0F;
            return Mth.clamp(reviveProgress + elapsedTicks / requiredTicks, 0.0F, 1.0F);
        }

        return displayedReviveProgress;
    }

    public static float reviveOverlayAlpha() {
        return reviveProgress > 0.0F ? 1.0F : reviveOverlayFadeTicks / (float) REVIVE_OVERLAY_FADE_TICKS;
    }

    public static String displayedReviverName() {
        return reviveProgress > 0.0F ? reviverName : displayedReviverName;
    }

    public static float reviveTicksRemaining() {
        return Math.max(0.0F, (1.0F - displayedReviveProgress()) * reviveRequiredTicks());
    }

    private static int reviveRequiredTicks() {
        return (int) Math.ceil(SecondWindConfig.REVIVE_CHANNEL_SECONDS.get() * 20.0D);
    }

    private static void clearReviveOverlay() {
        reviveProgress = 0.0F;
        displayedReviveProgress = 0.0F;
        reviverName = "";
        displayedReviverName = "";
        reviveOverlayFadeTicks = 0;
        reviveProgressSyncNanos = 0L;
    }

    private static void beginDownedTimerDamage(int damageTicksLost) {
        if (maxTicks <= 0 || damageTicksLost <= 0) {
            clearDownedTimerDamage();
            return;
        }

        float currentTicks = Mth.clamp(ticksRemaining, 0, maxTicks);
        float previousTicks = Mth.clamp(ticksRemaining + damageTicksLost, currentTicks, maxTicks);
        float trailingTicks = downedTimerDamageEndTicks();
        downedTimerDamageLostTicks = Math.max(previousTicks, trailingTicks) - currentTicks;
        downedTimerDamageStartNanos = System.nanoTime();
        downedTimerDamagePausedElapsedNanos = 0L;
        downedTimerDamagePauseStartedNanos = -1L;
    }

    private static void clearDownedTimerDamage() {
        downedTimerDamageLostTicks = 0.0F;
        downedTimerDamageStartNanos = 0L;
        downedTimerDamagePausedElapsedNanos = 0L;
        downedTimerDamagePauseStartedNanos = -1L;
    }

    private static float displayedDownedTimerDamageLostTicks() {
        if (downedTimerDamageLostTicks <= 0.0F || downedTimerDamageStartNanos <= 0L) {
            return 0.0F;
        }

        long elapsedNanos = downedTimerDamageElapsedNanos();
        float progress = Mth.clamp(
                elapsedNanos / (float) DOWNED_TIMER_DAMAGE_CATCHUP_NANOS,
                0.0F,
                1.0F);
        float remaining = 1.0F - easeOutCubic(progress);
        return downedTimerDamageLostTicks * remaining;
    }

    private static long downedTimerDamageElapsedNanos() {
        long now = System.nanoTime();
        if (shouldFreezeDisplayedTimer()) {
            if (downedTimerDamagePauseStartedNanos < 0L) {
                downedTimerDamagePauseStartedNanos = now;
            }
        } else if (downedTimerDamagePauseStartedNanos >= 0L) {
            downedTimerDamagePausedElapsedNanos += now - downedTimerDamagePauseStartedNanos;
            downedTimerDamagePauseStartedNanos = -1L;
        }

        long effectiveNow = downedTimerDamagePauseStartedNanos >= 0L ? downedTimerDamagePauseStartedNanos : now;
        return Math.max(0L, effectiveNow - downedTimerDamageStartNanos - downedTimerDamagePausedElapsedNanos);
    }

    private static float easeOutCubic(float progress) {
        float inverse = 1.0F - progress;
        return 1.0F - inverse * inverse * inverse;
    }

    private static float smoothstep(float progress) {
        return progress * progress * (3.0F - 2.0F * progress);
    }

    private static boolean shouldFreezeDisplayedTimer() {
        return timerPaused || shouldFreezeForLocalPause();
    }

    public static boolean shouldFreezeUiAnimations() {
        return shouldFreezeForLocalPause();
    }

    private static boolean shouldFreezeForLocalPause() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.isPaused()
                && minecraft.hasSingleplayerServer()
                && minecraft.getSingleplayerServer() != null
                && !minecraft.getSingleplayerServer().isPublished();
    }
}
