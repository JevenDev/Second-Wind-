package com.jvn.secondwind.client;

import com.jvn.secondwind.SecondWindMod;
import com.jvn.secondwind.client.hud.SecondWindHud;
import com.jvn.secondwind.config.SecondWindConfig;
import com.jvn.secondwind.item.SecondWindItems;
import com.jvn.secondwind.network.SecondWindNetworking;
import com.jvn.secondwind.client.shader.SecondWindPostEffects;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class SecondWindClient {
    private static final int GIVE_UP_HOLD_TICKS = 30;
    private static final int REVIVE_OVERLAY_FADE_TICKS = 6;
    private static KeyMapping giveUpKey;
    private static int giveUpHeldTicks;
    private static boolean giveUpSent;
    private static boolean localDownedPoseApplied;
    private static int activeReviveTargetId = -1;
    private static int reviveHeldTicks;
    private static int reviveRequiredTicks;
    private static int reviveDisplayHeldTicks;
    private static String reviveDisplayTargetName = "";
    private static int reviveFadeTicks;

    private SecondWindClient() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(SecondWindClient::registerKeys);
        modEventBus.addListener(SecondWindPostEffects::registerReloadListeners);
        modEventBus.addListener(SecondWindHud::registerGuiLayers);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        giveUpKey = new KeyMapping(
                "key.secondwind.give_up",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "key.categories.secondwind");
        event.register(giveUpKey);
    }

    @EventBusSubscriber(modid = SecondWindMod.MOD_ID, value = Dist.CLIENT)
    public static final class GameEvents {
        private GameEvents() {
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            ClientSecondWindState.tickClient();
            ClientTrackedDownedPlayers.tickClient();
            SecondWindPostEffects.tickClient();
            Minecraft minecraft = Minecraft.getInstance();
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

            if (giveUpKey == null) {
                resetGiveUpHold();
                return;
            }

            minecraft.player.setSprinting(false);
            minecraft.options.keySprint.setDown(false);

            if (giveUpKey.isDown()) {
                giveUpHeldTicks++;
                if (giveUpHeldTicks >= GIVE_UP_HOLD_TICKS && !giveUpSent) {
                    SecondWindNetworking.sendGiveUpRequest();
                    giveUpSent = true;
                }
            } else {
                resetGiveUpHold();
            }
        }
    }

    private static void syncLocalDownedPose(Minecraft minecraft) {
        if (ClientSecondWindState.isDowned()) {
            minecraft.player.setForcedPose(Pose.SWIMMING);
            localDownedPoseApplied = true;
            return;
        }

        if (localDownedPoseApplied) {
            minecraft.player.setForcedPose(null);
            localDownedPoseApplied = false;
        }
    }

    private static void updateReviveHoldOverlay(Minecraft minecraft) {
        Player targetPlayer = currentReviveTarget(minecraft);
        if (targetPlayer == null) {
            releaseReviveHoldOverlay(true);
            tickReviveHoldFade();
            return;
        }

        if (activeReviveTargetId != targetPlayer.getId()) {
            releaseReviveHoldOverlay(false);
            activeReviveTargetId = targetPlayer.getId();
            reviveHeldTicks = 0;
        }

        reviveRequiredTicks = reviveRequiredTicks();
        if (reviveRequiredTicks <= 0) {
            clearReviveHoldOverlay();
            return;
        }

        reviveHeldTicks = Mth.clamp(reviveHeldTicks + 1, 0, reviveRequiredTicks);
        reviveDisplayHeldTicks = reviveHeldTicks;
        reviveDisplayTargetName = targetPlayer.getName().getString();
        reviveFadeTicks = 0;
        SecondWindNetworking.sendReviveHoldRequest(targetPlayer.getId());
    }

    private static Player currentReviveTarget(Minecraft minecraft) {
        if (minecraft.screen != null
                || !SecondWindConfig.MULTIPLAYER_REVIVE.get()
                || !minecraft.options.keyUse.isDown()
                || !(minecraft.hitResult instanceof EntityHitResult entityHitResult)
                || !(entityHitResult.getEntity() instanceof Player targetPlayer)
                || targetPlayer == minecraft.player
                || !ClientTrackedDownedPlayers.isDowned(targetPlayer.getId())) {
            return null;
        }

        double maxDistance = SecondWindConfig.REVIVE_DISTANCE.get();
        return minecraft.player.distanceToSqr(targetPlayer) <= maxDistance * maxDistance ? targetPlayer : null;
    }

    private static int reviveRequiredTicks() {
        return (int) Math.ceil(SecondWindConfig.REVIVE_CHANNEL_SECONDS.get() * 20.0D);
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

        minecraft.gameRenderer.displayItemActivation(SecondWindItems.SECOND_WIND_POP.toStack());
    }

    public static Component giveUpKeyName() {
        return giveUpKey == null ? Component.literal("R") : giveUpKey.getTranslatedKeyMessage();
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
