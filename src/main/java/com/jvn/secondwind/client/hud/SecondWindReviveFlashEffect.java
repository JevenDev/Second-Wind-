package com.jvn.secondwind.client.hud;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import team.lodestar.lodestone.modules.core.easing.Easing;

public final class SecondWindReviveFlashEffect {
    public static final int DURATION_TICKS = 56;

    private static final int VANILLA_ACTIVATION_TICKS = 40;
    private static final Component TITLE = Component.literal("SECOND WIND");
    private static final float TARGET_Y_FRACTION = 0.24F;
    private static final int TITLE_COLOR = 0xFFFFF3C9;
    private static final int TITLE_SHADOW_COLOR = 0xCC140C08;
    private static final int TITLE_OUTLINE_COLOR = 0xFF120B08;
    private static final int TITLE_GLOW_COLOR = 0xFFE7AF41;
    private static final int TITLE_HOT_GLOW_COLOR = 0xFFFFE79A;
    private static final RandomSource RANDOM = RandomSource.create();

    private static float activationOffsetX;
    private static float activationOffsetY;

    private SecondWindReviveFlashEffect() {
    }

    public static void beginActivation() {
        activationOffsetX = (RANDOM.nextFloat() - 0.5F) * 1.6F;
        activationOffsetY = (RANDOM.nextFloat() - 0.5F) * 1.2F;
    }

    public static void render(GuiGraphics graphics, int ticksRemaining) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        if (ticksRemaining <= 0) {
            return;
        }

        Font font = minecraft.font;
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        float partialTick = minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        float elapsedTicks = Mth.clamp(DURATION_TICKS - ticksRemaining + partialTick, 0.0F, DURATION_TICKS);
        float progress = elapsedTicks / (float) DURATION_TICKS;
        float intro = Easing.CUBIC_OUT.ease(Mth.clamp(progress / 0.16F, 0.0F, 1.0F));
        float settle = Easing.SINE_OUT.ease(Mth.clamp(progress / 0.26F, 0.0F, 1.0F));
        float outro = Easing.SINE_IN.ease(Mth.clamp((progress - 0.62F) / 0.38F, 0.0F, 1.0F));
        float visibility = 1.0F - outro;
        float pulse = 1.0F + 0.03F * Mth.sin(progress * 15.0F) * visibility;
        float baseScale = (3.0F + 0.24F * (1.0F - settle) + 0.06F * visibility) * pulse;
        float topCenterX = width / 2.0F;
        float topCenterY = height * TARGET_Y_FRACTION;
        float introDrop = (1.0F - intro) * 20.0F;
        float outroLift = outro * 12.0F;
        float topY = topCenterY - font.lineHeight * baseScale / 2.0F - introDrop - outroLift;

        float activationProgress = Mth.clamp(elapsedTicks / VANILLA_ACTIVATION_TICKS, 0.0F, 1.0F);
        float activationCurve = vanillaActivationCurve(activationProgress);
        float activationAngle = activationCurve * (float) Math.PI;
        float activationBlend = 1.0F - Easing.SINE_IN.ease(Mth.clamp((elapsedTicks - 20.0F) / 18.0F, 0.0F, 1.0F));
        float vanillaScaleMultiplier = 1.0F + 0.5F * Mth.sin(activationAngle) + 0.12F * (1.0F - activationProgress);
        float scale = baseScale * Mth.lerp(activationBlend, 1.0F, vanillaScaleMultiplier);
        float popCenterX = width / 2.0F + activationOffsetX * width * 0.13F * activationBlend;
        float popCenterY = height / 2.0F + activationOffsetY * height * 0.08F * activationBlend - 18.0F * Mth.sin(activationAngle) * activationBlend;
        float centerX = Mth.lerp(activationBlend, topCenterX, popCenterX);
        float centerY = Mth.lerp(activationBlend, topY + font.lineHeight * baseScale / 2.0F, popCenterY);
        float rotation = activationBlend * (activationOffsetX * 9.0F + 3.0F * Mth.cos(activationProgress * 8.0F));
        float alpha = Mth.clamp(intro * visibility, 0.0F, 1.0F);
        float glowAlpha = alpha * (0.16F + 0.10F * (1.0F - settle));
        float hotGlowAlpha = alpha * (0.08F + 0.06F * Mth.sin(progress * 10.0F + 0.6F));

        drawCenteredScaledString(graphics, font, TITLE, centerX + 2.0F, centerY + 3.0F, scale + 0.05F, rotation, applyAlpha(TITLE_SHADOW_COLOR, alpha * 0.72F));
        drawCenteredScaledString(graphics, font, TITLE, centerX, centerY, scale + 0.24F, rotation, applyAlpha(TITLE_GLOW_COLOR, glowAlpha));
        drawCenteredScaledString(graphics, font, TITLE, centerX, centerY, scale + 0.11F, rotation, applyAlpha(TITLE_HOT_GLOW_COLOR, hotGlowAlpha));
        drawCenteredScaledOutlinedString(
                graphics,
                font,
                TITLE,
                centerX,
                centerY,
                scale,
                rotation,
                applyAlpha(TITLE_COLOR, alpha),
                applyAlpha(TITLE_OUTLINE_COLOR, alpha));
    }

    private static void drawCenteredScaledString(
            GuiGraphics graphics,
            Font font,
            Component text,
            float centerX,
            float centerY,
            float scale,
            float rotationDegrees,
            int color) {
        String visualText = text.getString();
        float textWidth = font.width(visualText);
        float textHeight = font.lineHeight;

        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(rotationDegrees));
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, visualText, -textWidth / 2.0F, -textHeight / 2.0F, color, false);
        graphics.pose().popPose();
    }

    private static void drawCenteredScaledOutlinedString(
            GuiGraphics graphics,
            Font font,
            Component text,
            float centerX,
            float centerY,
            float scale,
            float rotationDegrees,
            int color,
            int outlineColor) {
        String visualText = text.getString();
        float textWidth = font.width(visualText);
        float textHeight = font.lineHeight;
        int outlineOffset = 1;

        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(rotationDegrees));
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, visualText, -textWidth / 2.0F - outlineOffset, -textHeight / 2.0F, outlineColor, false);
        graphics.drawString(font, visualText, -textWidth / 2.0F + outlineOffset, -textHeight / 2.0F, outlineColor, false);
        graphics.drawString(font, visualText, -textWidth / 2.0F, -textHeight / 2.0F - outlineOffset, outlineColor, false);
        graphics.drawString(font, visualText, -textWidth / 2.0F, -textHeight / 2.0F + outlineOffset, outlineColor, false);
        graphics.drawString(font, visualText, -textWidth / 2.0F, -textHeight / 2.0F, color, false);
        graphics.pose().popPose();
    }

    private static float vanillaActivationCurve(float progress) {
        float squared = progress * progress;
        float cubed = squared * progress;
        float fourth = cubed * progress;
        float fifth = fourth * progress;
        return 10.25F * fifth - 24.95F * fourth + 25.5F * cubed - 13.8F * squared + 4.0F * progress;
    }

    private static int applyAlpha(int color, float alpha) {
        int alphaChannel = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
        return alphaChannel << 24 | color & 0x00FFFFFF;
    }
}