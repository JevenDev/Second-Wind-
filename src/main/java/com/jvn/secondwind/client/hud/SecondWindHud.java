package com.jvn.secondwind.client.hud;

import com.jvn.secondwind.SecondWindMod;
import com.jvn.secondwind.client.ClientSecondWindState;
import com.jvn.secondwind.client.SecondWindClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.Locale;

@EventBusSubscriber(modid = SecondWindMod.MOD_ID, value = Dist.CLIENT)
public final class SecondWindHud {
    private static final ResourceLocation LAST_STAND_LAYER = ResourceLocation.fromNamespaceAndPath(
            SecondWindMod.MOD_ID,
            "last_stand_layer");
    private static final ResourceLocation LAST_STAND_BAR_BASE = ResourceLocation.fromNamespaceAndPath(
            SecondWindMod.MOD_ID,
            "gui/hud/last_stand_bar_base.png");
    private static final ResourceLocation LAST_STAND_BAR_FILL = ResourceLocation.fromNamespaceAndPath(
            SecondWindMod.MOD_ID,
            "gui/hud/last_stand_bar_fill.png");
    private static final ResourceLocation LAST_STAND_OVERLAY = ResourceLocation.fromNamespaceAndPath(
            SecondWindMod.MOD_ID,
            "gui/hud/last_stand_overlay.png");
    private static final int LAST_STAND_WIDTH = 168;
    private static final int LAST_STAND_HEIGHT = 31;
    private static final int LAST_STAND_FILL_LEFT = 17;
    private static final int LAST_STAND_FILL_WIDTH = 150;
    private static final int LAST_STAND_HOTBAR_GAP = 32;
    private static final int LAST_STAND_TIMER_CENTER_X = 153;
    private static final int LAST_STAND_TIMER_CENTER_Y = 5;
    private static final int LAST_STAND_TIMER_RED_TICKS = 100;
    public static final int TIMER_OUTLINE_COLOR = 0xFF000000;

    private SecondWindHud() {
    }

    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CHAT, LAST_STAND_LAYER, SecondWindHud::renderHudLayer);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!ClientSecondWindState.isDowned()) {
            renderReviveFlash(event);
        }
    }

    public static void renderHudLayer(GuiGraphics graphics, DeltaTracker partialTick) {
        if (!ClientSecondWindState.isDowned()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int x = width / 2 - LAST_STAND_WIDTH / 2;
        int y = height - 22 - LAST_STAND_HEIGHT - LAST_STAND_HOTBAR_GAP;

        renderLastStandTimer(graphics, x, y);
        renderBeingRevived(graphics, minecraft.font, width / 2, height / 2 - 18);
        renderGiveUpCountdown(graphics, minecraft.font, width / 2, height / 2 + 12);
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

    private static void renderLastStandTimer(GuiGraphics graphics, int x, int y) {
        Font font = Minecraft.getInstance().font;
        float displayedTicksRemaining = ClientSecondWindState.displayedTicksRemaining();
        float timerProgress = ClientSecondWindState.maxTicks() <= 0
                ? 0.0F
                : Mth.clamp(displayedTicksRemaining / ClientSecondWindState.maxTicks(), 0.0F, 1.0F);
        int fillWidth = Mth.clamp(Mth.floor(timerProgress * LAST_STAND_FILL_WIDTH), 0, LAST_STAND_FILL_WIDTH);
        String timerLabel = formatTimerLabel(displayedTicksRemaining);
        int timerColor = timerTextColor(displayedTicksRemaining);

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 0.0F);
        graphics.blit(LAST_STAND_BAR_BASE, x, y, 0.0F, 0.0F, LAST_STAND_WIDTH, LAST_STAND_HEIGHT, LAST_STAND_WIDTH, LAST_STAND_HEIGHT);
        if (fillWidth > 0) {
            graphics.blit(
                    LAST_STAND_BAR_FILL,
                    x + LAST_STAND_FILL_LEFT,
                    y,
                    LAST_STAND_FILL_LEFT,
                    0.0F,
                    fillWidth,
                    LAST_STAND_HEIGHT,
                    LAST_STAND_WIDTH,
                    LAST_STAND_HEIGHT);
        }
        graphics.blit(LAST_STAND_OVERLAY, x, y, 0.0F, 0.0F, LAST_STAND_WIDTH, LAST_STAND_HEIGHT, LAST_STAND_WIDTH, LAST_STAND_HEIGHT);
        drawOutlinedCenteredString(
                graphics,
                font,
                timerLabel,
                x + LAST_STAND_TIMER_CENTER_X,
                y + LAST_STAND_TIMER_CENTER_Y,
                timerColor,
                TIMER_OUTLINE_COLOR);
        graphics.pose().popPose();
    }

    public static String formatTimerLabel(float displayedTicksRemaining) {
        return String.format(Locale.ROOT, "%.1fs", displayedTicksRemaining / 20.0F);
    }

    public static int timerTextColor(float displayedTicksRemaining) {
        return displayedTicksRemaining <= LAST_STAND_TIMER_RED_TICKS ? 0xFFFF4040 : 0xFFFFFFFF;
    }

    private static void drawOutlinedCenteredString(
            GuiGraphics graphics,
            Font font,
            String text,
            int centerX,
            int centerY,
            int color,
            int outlineColor) {
        int textX = centerX - font.width(text) / 2;
        int textY = centerY - font.lineHeight / 2;
        graphics.drawString(font, text, textX - 1, textY, outlineColor, false);
        graphics.drawString(font, text, textX + 1, textY, outlineColor, false);
        graphics.drawString(font, text, textX, textY - 1, outlineColor, false);
        graphics.drawString(font, text, textX, textY + 1, outlineColor, false);
        graphics.drawString(font, text, textX, textY, color, false);
    }

    private static void renderGiveUpCountdown(GuiGraphics graphics, Font font, int centerX, int centerY) {
        if (!ClientSecondWindState.giveUpAvailable() || !SecondWindClient.isHoldingGiveUp()) {
            return;
        }

        String timerLabel = String.format(Locale.ROOT, "%.1fs", SecondWindClient.giveUpHoldSecondsRemaining());
        drawOutlinedCenteredString(graphics, font, timerLabel, centerX, centerY, 0xFFFF4040, TIMER_OUTLINE_COLOR);
    }

    private static void renderBeingRevived(GuiGraphics graphics, Font font, int centerX, int centerY) {
        if (ClientSecondWindState.reviveProgress() <= 0.0F) {
            return;
        }

        String reviverName = ClientSecondWindState.reviverName();
        String text = reviverName == null || reviverName.isBlank()
                ? "Being revived!"
                : "Being revived by " + reviverName + "!";
        drawOutlinedCenteredString(graphics, font, text, centerX, centerY, 0xFFFFFFFF, TIMER_OUTLINE_COLOR);
    }
}
