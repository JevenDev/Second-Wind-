package com.jvn.secondwind.client;

import com.jvn.secondwind.SecondWindMod;
import com.jvn.secondwind.client.hud.SecondWindHud;
import com.jvn.secondwind.network.SecondWindNetworking;
import com.jvn.secondwind.client.shader.SecondWindPostEffects;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class SecondWindClient {
    private static final int GIVE_UP_HOLD_TICKS = 30;
    private static KeyMapping giveUpKey;
    private static int giveUpHeldTicks;
    private static boolean giveUpSent;
    private static boolean localDownedPoseApplied;

    private SecondWindClient() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(SecondWindClient::registerKeys);
        modEventBus.addListener(SecondWindClient::onClientSetup);
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

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(SecondWindPostEffects::register);
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
                return;
            }

            syncLocalDownedPose(minecraft);

            if (!ClientSecondWindState.isDowned()) {
                resetGiveUpHold();
                sendReviveHoldIfNeeded(minecraft);
                return;
            }

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

    private static void sendReviveHoldIfNeeded(Minecraft minecraft) {
        if (minecraft.screen != null
                || !minecraft.options.keyUse.isDown()
                || !(minecraft.hitResult instanceof EntityHitResult entityHitResult)
                || !(entityHitResult.getEntity() instanceof Player targetPlayer)
                || targetPlayer == minecraft.player) {
            return;
        }

        SecondWindNetworking.sendReviveHoldRequest(targetPlayer.getId());
    }

    private static void resetGiveUpHold() {
        giveUpHeldTicks = 0;
        giveUpSent = false;
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
}
