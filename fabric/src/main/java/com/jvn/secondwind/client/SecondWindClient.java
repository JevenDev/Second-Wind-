package com.jvn.secondwind.client;

import com.jvn.secondwind.SecondWindMod;
import com.jvn.secondwind.config.SecondWindConfig;
import com.jvn.secondwind.item.SecondWindItems;
import com.jvn.secondwind.network.SecondWindNetworking;
import com.jvn.secondwind.client.shader.SecondWindPostEffects;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import org.lwjgl.glfw.GLFW;

public final class SecondWindClient implements ClientModInitializer {
    private static final int GIVE_UP_HOLD_TICKS = 30;
    private static final int REVIVE_OVERLAY_FADE_TICKS = 6;
    private static final KeyMapping GIVE_UP_KEY = new KeyMapping(
            "key." + SecondWindMod.MOD_ID + ".give_up",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories." + SecondWindMod.MOD_ID);
    private static int giveUpHeldTicks;
    private static boolean giveUpSent;
    private static boolean localDownedPoseApplied;
    private static int activeReviveTargetId = -1;
    private static int reviveHeldTicks;
    private static int reviveRequiredTicks;
    private static int reviveDisplayHeldTicks;
    private static String reviveDisplayTargetName = "";
    private static int reviveFadeTicks;

    public SecondWindClient() {
    }

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(GIVE_UP_KEY);
        SecondWindNetworking.registerClientPayloads();
        SecondWindPostEffects.register();
        com.jvn.secondwind.client.hud.SecondWindHud.register();
        com.jvn.secondwind.client.render.SecondWindPlayerTimers.register();
        ClientTickEvents.END_CLIENT_TICK.register(SecondWindClient::onClientTick);
    }

    private static void onClientTick(Minecraft minecraft) {
        ClientSecondWindState.tickClient();
        ClientTrackedDownedPlayers.tickClient();
        SecondWindPostEffects.tickClient();
            if (minecraft.player == null) {
                localDownedPoseApplied = false;
                resetGiveUpHold();
                clearReviveHoldOverlay();
                return;
            }

            syncLocalDownedPose(minecraft);

            if (!ClientSecondWindState.isDowned()) {
                resetGiveUpHold();
                updateReviveHoldOverlay(minecraft);
                return;
            }

            clearReviveHoldOverlay();

            minecraft.player.setSprinting(false);
            minecraft.options.keySprint.setDown(false);

            if (GIVE_UP_KEY.isDown()) {
                giveUpHeldTicks++;
                if (giveUpHeldTicks >= GIVE_UP_HOLD_TICKS && !giveUpSent) {
                    SecondWindNetworking.sendGiveUpRequest();
                    giveUpSent = true;
                }
            } else {
                resetGiveUpHold();
            }
    }

    private static void syncLocalDownedPose(Minecraft minecraft) {
        if (ClientSecondWindState.isDowned()) {
            minecraft.player.setPose(Pose.SWIMMING);
            localDownedPoseApplied = true;
            return;
        }

        if (localDownedPoseApplied) {
            minecraft.player.setPose(Pose.STANDING);
            localDownedPoseApplied = false;
        }
    }

    private static void updateReviveHoldOverlay(Minecraft minecraft) {
        LivingEntity targetEntity = currentReviveTarget(minecraft);
        if (targetEntity == null) {
            releaseReviveHoldOverlay(true);
            tickReviveHoldFade();
            return;
        }

        if (activeReviveTargetId != targetEntity.getId()) {
            releaseReviveHoldOverlay(false);
            activeReviveTargetId = targetEntity.getId();
            reviveHeldTicks = 0;
        }

        reviveRequiredTicks = ClientTrackedDownedPlayers.reviveChannelTicks(targetEntity.getId());
        if (reviveRequiredTicks <= 0) {
            clearReviveHoldOverlay();
            return;
        }

        reviveHeldTicks = Mth.clamp(reviveHeldTicks + 1, 0, reviveRequiredTicks);
        reviveDisplayHeldTicks = reviveHeldTicks;
        reviveDisplayTargetName = targetEntity.getName().getString();
        reviveFadeTicks = 0;
        SecondWindNetworking.sendReviveHoldRequest(targetEntity.getId());
    }

    private static LivingEntity currentReviveTarget(Minecraft minecraft) {
        if (minecraft.screen != null
                || !minecraft.options.keyUse.isDown()
                || !(minecraft.hitResult instanceof EntityHitResult entityHitResult)
                || !(entityHitResult.getEntity() instanceof LivingEntity targetEntity)
                || targetEntity == minecraft.player
                || !ClientTrackedDownedPlayers.isDowned(targetEntity.getId())
                || !ClientTrackedDownedPlayers.reviveEnabled(targetEntity.getId())) {
            return null;
        }

        double maxDistance = ClientTrackedDownedPlayers.reviveDistance(targetEntity.getId());
        return minecraft.player.distanceToSqr(targetEntity) <= maxDistance * maxDistance ? targetEntity : null;
    }

    private static void releaseReviveHoldOverlay(boolean fade) {
        if (activeReviveTargetId != -1 && fade && reviveDisplayHeldTicks > 0) {
            reviveFadeTicks = REVIVE_OVERLAY_FADE_TICKS;
        }

        activeReviveTargetId = -1;
        reviveHeldTicks = 0;
    }

    private static void tickReviveHoldFade() {
        if (activeReviveTargetId != -1 || reviveFadeTicks <= 0) {
            return;
        }

        reviveFadeTicks--;
        if (reviveFadeTicks <= 0) {
            clearReviveHoldOverlay();
        }
    }

    private static void clearReviveHoldOverlay() {
        activeReviveTargetId = -1;
        reviveHeldTicks = 0;
        reviveRequiredTicks = 0;
        reviveDisplayHeldTicks = 0;
        reviveDisplayTargetName = "";
        reviveFadeTicks = 0;
    }

    private static void resetGiveUpHold() {
        giveUpHeldTicks = 0;
        giveUpSent = false;
    }

    public static void playReviveItemActivation() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameRenderer == null) {
            return;
        }

        minecraft.gameRenderer.displayItemActivation(new ItemStack(SecondWindItems.SECOND_WIND_POP));
    }

    public static Component giveUpKeyName() {
        return GIVE_UP_KEY.getTranslatedKeyMessage();
    }

    public static float giveUpHoldSecondsRemaining() {
        return Math.max(0.0F, (GIVE_UP_HOLD_TICKS - giveUpHeldTicks) / 20.0F);
    }

    public static boolean isHoldingGiveUp() {
        return giveUpHeldTicks > 0 && !giveUpSent;
    }

    public static boolean hasReviveOverlay() {
        return reviveDisplayHeldTicks > 0 && !reviveDisplayTargetName.isBlank();
    }

    public static float reviveOverlayAlpha() {
        return activeReviveTargetId != -1 ? 1.0F : reviveFadeTicks / (float) REVIVE_OVERLAY_FADE_TICKS;
    }

    public static float reviveProgress() {
        if (reviveRequiredTicks <= 0) {
            return 0.0F;
        }
        return Mth.clamp(reviveDisplayHeldTicks / (float) reviveRequiredTicks, 0.0F, 1.0F);
    }

    public static float reviveHoldTicksRemaining() {
        return Math.max(0.0F, reviveRequiredTicks - reviveDisplayHeldTicks);
    }

    public static String reviveTargetName() {
        return reviveDisplayTargetName;
    }
}
