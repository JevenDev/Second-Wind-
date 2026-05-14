package com.jvn.secondwind.client.hud;

import com.jvn.secondwind.SecondWindMod;
import com.jvn.secondwind.client.ClientSecondWindState;
import com.jvn.secondwind.client.SecondWindClient;
import com.jvn.toucanlib.client.toucanColors;
import com.jvn.toucanlib.client.toucanHudText;
import com.jvn.toucanlib.neoforge.client.toucanGuiLayers;
import com.jvn.toucanlib.util.toucanResourceLocations;
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
    private static final ResourceLocation LAST_STAND_BAR_BASE = toucanResourceLocations.id(
            SecondWindMod.MOD_ID,
            "gui/hud/last_stand_bar_base.png");
    private static final ResourceLocation LAST_STAND_BAR_FILL = toucanResourceLocations.id(
            SecondWindMod.MOD_ID,
            "gui/hud/last_stand_bar_fill.png");
    private static final ResourceLocation LAST_STAND_OVERLAY = toucanResourceLocations.id(
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
    private static final int REVIVER_LABEL_OFFSET_Y = -18;
    private static final int CROSSHAIR_LABEL_OFFSET_Y = -18;
    private static final int CROSSHAIR_PRIMARY_TIMER_OFFSET_Y = 12;
    private static final int CROSSHAIR_SECONDARY_TIMER_OFFSET_Y = 26;
    private static final int REVIVE_TEXT_COLOR = 0xFF61D394;
    public static final int TIMER_OUTLINE_COLOR = 0xFF000000;

    private SecondWindHud() {
    }

    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        toucanGuiLayers.registerAbove(
                event,
                VanillaGuiLayers.CHAT,
                SecondWindMod.MOD_ID,
                "last_stand_layer",
                SecondWindHud::renderHudLayer);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        renderCrosshairStatus(event);
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
    }

    private static void renderCrosshairStatus(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int centerX = minecraft.getWindow().getGuiScaledWidth() / 2;
        int centerY = minecraft.getWindow().getGuiScaledHeight() / 2;
        boolean reviveOverlayVisible = renderReviveStatus(graphics, font, centerX, centerY);

        if (ClientSecondWindState.isDowned()) {
            int giveUpTimerY = centerY + (reviveOverlayVisible ? CROSSHAIR_SECONDARY_TIMER_OFFSET_Y : CROSSHAIR_PRIMARY_TIMER_OFFSET_Y);
            renderGiveUpCountdown(graphics, font, centerX, giveUpTimerY);
        }
    }

    private static void renderReviveFlash(RenderGuiEvent.Post event) {
        int ticks = ClientSecondWindState.revivedFlashTicks();
        if (ticks <= 0) {
            return;
        }

        SecondWindReviveFlashEffect.render(event.getGuiGraphics(), ticks);
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

    private static boolean renderReviveStatus(GuiGraphics graphics, Font font, int centerX, int centerY) {
        if (ClientSecondWindState.isDowned()) {
            return renderDownedReviveStatus(graphics, font, centerX, centerY);
        }

        return renderReviverStatus(graphics, font, centerX, centerY);
    }

    private static boolean renderDownedReviveStatus(GuiGraphics graphics, Font font, int centerX, int centerY) {
        if (!ClientSecondWindState.hasReviveOverlay()) {
            return false;
        }

        float alpha = ClientSecondWindState.reviveOverlayAlpha();
        int textColor = toucanColors.withAlpha(REVIVE_TEXT_COLOR, alpha);
        int outlineColor = toucanColors.withAlpha(TIMER_OUTLINE_COLOR, alpha);
        int progressPercent = Math.round(ClientSecondWindState.displayedReviveProgress() * 100.0F);
        String reviverName = ClientSecondWindState.displayedReviverName();
        String statusText = reviverName == null || reviverName.isBlank()
                ? "Being revived " + progressPercent + "%"
                : "Being revived by " + reviverName + " " + progressPercent + "%";

        drawOutlinedCenteredString(
                graphics,
                font,
                statusText,
                centerX,
                centerY + CROSSHAIR_LABEL_OFFSET_Y,
                textColor,
                outlineColor);
        return true;
    }

    private static boolean renderReviverStatus(GuiGraphics graphics, Font font, int centerX, int centerY) {
        if (!SecondWindClient.hasReviveOverlay()) {
            return false;
        }

        float alpha = SecondWindClient.reviveOverlayAlpha();
        int textColor = toucanColors.withAlpha(REVIVE_TEXT_COLOR, alpha);
        int outlineColor = toucanColors.withAlpha(TIMER_OUTLINE_COLOR, alpha);
        int progressPercent = Math.round(SecondWindClient.reviveProgress() * 100.0F);
        String targetName = SecondWindClient.reviveTargetName();
        String statusText = targetName == null || targetName.isBlank()
                ? "Reviving " + progressPercent + "%"
                : "Reviving " + targetName + " " + progressPercent + "%";

        drawOutlinedCenteredString(
                graphics,
                font,
                statusText,
                centerX,
            centerY + REVIVER_LABEL_OFFSET_Y,
                textColor,
                outlineColor);
        return true;
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
        toucanHudText.drawOutlinedString(graphics, font, text, textX, textY, color, outlineColor);
    }

    private static void renderGiveUpCountdown(GuiGraphics graphics, Font font, int centerX, int centerY) {
        if (!ClientSecondWindState.giveUpAvailable() || !SecondWindClient.isHoldingGiveUp()) {
            return;
        }

        String timerLabel = String.format(Locale.ROOT, "%.1fs", SecondWindClient.giveUpHoldSecondsRemaining());
        drawOutlinedCenteredString(graphics, font, timerLabel, centerX, centerY, 0xFFFF4040, TIMER_OUTLINE_COLOR);
    }
}
