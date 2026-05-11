package com.jvn.secondwind.client.hud;

import com.jvn.secondwind.SecondWindMod;
import com.jvn.secondwind.client.ClientSecondWindState;
import com.jvn.secondwind.client.SecondWindClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = SecondWindMod.MOD_ID, value = Dist.CLIENT)
public final class SecondWindHud {
    private SecondWindHud() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!ClientSecondWindState.isDowned()) {
            renderReviveFlash(event);
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int centerX = width / 2;
        int centerY = height / 2 - 44;
        int seconds = (int) Math.ceil(ClientSecondWindState.ticksRemaining() / 20.0D);
        int urgency = ClientSecondWindState.ticksRemaining() <= 60 ? 0xFFFF4040 : 0xFFFFF2D0;

        graphics.fill(centerX - 112, centerY - 12, centerX + 112, centerY + 70, 0x99000000);
        graphics.drawCenteredString(font, Component.literal("FIGHT FOR YOUR LIFE"), centerX, centerY, 0xFFFF4040);
        graphics.drawCenteredString(font, Component.literal(seconds + "s"), centerX, centerY + 16, urgency);
        graphics.drawCenteredString(font, Component.literal("Kill an enemy to revive"), centerX, centerY + 34, 0xFFFFFFFF);
        graphics.drawCenteredString(font, Component.literal("Another player can revive you"), centerX, centerY + 46, 0xFFB7E2FF);

        if (ClientSecondWindState.reviveProgress() > 0.0F) {
            int barWidth = 140;
            int filled = Math.round(barWidth * ClientSecondWindState.reviveProgress());
            graphics.fill(centerX - barWidth / 2, centerY + 59, centerX + barWidth / 2, centerY + 63, 0xFF333333);
            graphics.fill(centerX - barWidth / 2, centerY + 59, centerX - barWidth / 2 + filled, centerY + 63, 0xFF61D394);
        } else if (ClientSecondWindState.giveUpAvailable()) {
            graphics.drawCenteredString(font, Component.literal("Hold ")
                    .append(SecondWindClient.giveUpKeyName())
                    .append(" to Give Up"), centerX, centerY + 58, 0xFFB8B8B8);
        }
    }

    private static void renderReviveFlash(RenderGuiEvent.Post event) {
        int ticks = ClientSecondWindState.revivedFlashTicks();
        if (ticks <= 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int alpha = Math.min(255, ticks * 8);
        int color = alpha << 24 | 0x61D394;
        graphics.drawCenteredString(font, Component.literal("SECOND WIND"), width / 2, height / 2 - 28, color);
    }
}
