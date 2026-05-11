package com.jvn.secondwind.client;

import com.jvn.secondwind.SecondWindMod;
import com.jvn.secondwind.network.SecondWindNetworking;
import com.jvn.secondwind.client.shader.SecondWindPostEffects;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
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

    private SecondWindClient() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(SecondWindClient::registerKeys);
        modEventBus.addListener(SecondWindClient::onClientSetup);
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
            SecondWindPostEffects.tickClient();
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || giveUpKey == null || !ClientSecondWindState.isDowned()) {
                resetGiveUpHold();
                return;
            }

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

    private static void resetGiveUpHold() {
        giveUpHeldTicks = 0;
        giveUpSent = false;
    }

    public static Component giveUpKeyName() {
        return giveUpKey == null ? Component.literal("R") : giveUpKey.getTranslatedKeyMessage();
    }
}
