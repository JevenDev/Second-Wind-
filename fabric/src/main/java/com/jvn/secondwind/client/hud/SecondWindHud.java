package com.jvn.secondwind.client.hud;

import com.jvn.secondwind.SecondWindMod;
import com.jvn.secondwind.client.ClientSecondWindState;
import com.jvn.secondwind.client.SecondWindClient;
import com.jvn.secondwind.config.SecondWindConfig;
import com.jvn.toucanlib.client.ToucanColors;
import com.jvn.toucanlib.client.ToucanHudText;
import com.jvn.toucanlib.util.ToucanResourceLocations;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

import java.util.Locale;

public final class SecondWindHud {
    private static final ResourceLocation LAST_STAND_BAR_BASE = ToucanResourceLocations.id(
            SecondWindMod.MOD_ID,
            "gui/hud/last_stand_bar_base.png");
    private static final ResourceLocation LAST_STAND_BAR_FILL = ToucanResourceLocations.id(
            SecondWindMod.MOD_ID,
            "gui/hud/last_stand_bar_fill.png");
    private static final ResourceLocation LAST_STAND_OVERLAY = ToucanResourceLocations.id(
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

    public static void register() {
        HudRenderCallback.EVENT.register(SecondWindHud::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        renderHudLayer(graphics, deltaTracker);
        renderCrosshairStatus(graphics);
        if (!ClientSecondWindState.isDowned()) {
            renderReviveFlash(graphics);
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

        if (SecondWindConfig.USE_SIMPLE_DOWNED_TIMER.get()) {
            return;
        }

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int x = width / 2 - LAST_STAND_WIDTH / 2;
        int y = height - 22 - LAST_STAND_HEIGHT - LAST_STAND_HOTBAR_GAP;

        renderLastStandTimer(graphics, x, y);
    }

    private static void renderCrosshairStatus(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        Font font = minecraft.font;
        int centerX = minecraft.getWindow().getGuiScaledWidth() / 2;
        int centerY = minecraft.getWindow().getGuiScaledHeight() / 2;
        boolean reviveOverlayVisible = renderReviveStatus(graphics, font, centerX, centerY);

        if (ClientSecondWindState.isDowned()) {
            renderSimpleDownedTimer(graphics, font, centerX, centerY, reviveOverlayVisible);
            int giveUpTimerY = centerY + (reviveOverlayVisible ? CROSSHAIR_SECONDARY_TIMER_OFFSET_Y : CROSSHAIR_PRIMARY_TIMER_OFFSET_Y);
            renderGiveUpCountdown(graphics, font, centerX, giveUpTimerY);
        }
    }

    private static void renderReviveFlash(GuiGraphics graphics) {
        int ticks = ClientSecondWindState.revivedFlashTicks();
        if (ticks <= 0) {
            return;
        }

        SecondWindReviveFlashEffect.render(graphics, ticks);
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

    private static boolean renderSimpleDownedTimer(GuiGraphics graphics, Font font, int centerX, int centerY, boolean reviveOverlayVisible) {
        if (!SecondWindConfig.USE_SIMPLE_DOWNED_TIMER.get()) {
            return false;
        }

        float displayedTicksRemaining = ClientSecondWindState.displayedTicksRemaining();
        int timerY = centerY + (reviveOverlayVisible ? CROSSHAIR_PRIMARY_TIMER_OFFSET_Y : CROSSHAIR_LABEL_OFFSET_Y);
        drawOutlinedCenteredString(
                graphics,
                font,
                formatTimerLabel(displayedTicksRemaining),
                centerX,
                timerY,
                timerTextColor(displayedTicksRemaining),
                TIMER_OUTLINE_COLOR);
        return true;
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
        int textColor = ToucanColors.withAlpha(REVIVE_TEXT_COLOR, alpha);
        int outlineColor = ToucanColors.withAlpha(TIMER_OUTLINE_COLOR, alpha);
        int progressPercent = Math.round(ClientSecondWindState.displayedReviveProgress() * 100.0F);
        String reviverName = ClientSecondWindState.displayedReviverName();
        String statusText = reviverName == null || reviverName.isBlank()
                ? Component.translatable("hud.secondwind.being_revived", progressPercent).getString()
                : Component.translatable("hud.secondwind.being_revived_by", reviverName, progressPercent).getString();

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
        int textColor = ToucanColors.withAlpha(REVIVE_TEXT_COLOR, alpha);
        int outlineColor = ToucanColors.withAlpha(TIMER_OUTLINE_COLOR, alpha);
        int progressPercent = Math.round(SecondWindClient.reviveProgress() * 100.0F);
        String targetName = SecondWindClient.reviveTargetName();
        String statusText = targetName == null || targetName.isBlank()
                ? Component.translatable("hud.secondwind.reviving", progressPercent).getString()
                : Component.translatable("hud.secondwind.reviving_target", targetName, progressPercent).getString();

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
        ToucanHudText.drawOutlinedString(graphics, font, text, textX, textY, color, outlineColor);
    }

    private static void renderGiveUpCountdown(GuiGraphics graphics, Font font, int centerX, int centerY) {
        if (!ClientSecondWindState.giveUpAvailable() || !SecondWindClient.isHoldingGiveUp()) {
            return;
        }

        String timerLabel = String.format(Locale.ROOT, "%.1fs", SecondWindClient.giveUpHoldSecondsRemaining());
        drawOutlinedCenteredString(graphics, font, timerLabel, centerX, centerY, 0xFFFF4040, TIMER_OUTLINE_COLOR);
    }
}
