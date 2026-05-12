package com.jvn.secondwind.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import team.lodestar.lodestone.modules.core.easing.Easing;

public final class SecondWindReviveFlashEffect {
    public static final int DURATION_TICKS = 56;

    private static final Component TITLE = Component.literal("SECOND WIND");
    private static final float TARGET_Y_FRACTION = 0.24F;
    private static final int TITLE_COLOR = 0xFFFFF3C9;
    private static final int TITLE_SHADOW_COLOR = 0xCC140C08;
    private static final int TITLE_OUTLINE_COLOR = 0xFF120B08;
    private static final int TITLE_GLOW_COLOR = 0xFFE7AF41;
    private static final int TITLE_HOT_GLOW_COLOR = 0xFFFFE79A;

    private SecondWindReviveFlashEffect() {
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
        float progress = 1.0F - ticksRemaining / (float) DURATION_TICKS;
        float intro = Easing.CUBIC_OUT.ease(Mth.clamp(progress / 0.16F, 0.0F, 1.0F));
        float settle = Easing.SINE_OUT.ease(Mth.clamp(progress / 0.26F, 0.0F, 1.0F));
        float outro = Easing.SINE_IN.ease(Mth.clamp((progress - 0.62F) / 0.38F, 0.0F, 1.0F));
        float visibility = 1.0F - outro;
        float pulse = 1.0F + 0.03F * Mth.sin(progress * 15.0F) * visibility;
        float scale = (3.15F + 0.36F * (1.0F - settle) + 0.08F * visibility) * pulse;
        int centerX = width / 2;
        int centerY = Math.round(height * TARGET_Y_FRACTION);
        int introDrop = Math.round((1.0F - intro) * 20.0F);
        int outroLift = Math.round(outro * 12.0F);
        int y = centerY - Math.round(font.lineHeight * scale / 2.0F) - introDrop - outroLift;
        float alpha = Mth.clamp(intro * visibility, 0.0F, 1.0F);
        float glowAlpha = alpha * (0.16F + 0.10F * (1.0F - settle));
        float hotGlowAlpha = alpha * (0.08F + 0.06F * Mth.sin(progress * 10.0F + 0.6F));

        drawCenteredScaledString(graphics, font, TITLE, centerX + 2, y + 3, scale + 0.05F, applyAlpha(TITLE_SHADOW_COLOR, alpha * 0.72F));
        drawCenteredScaledString(graphics, font, TITLE, centerX, y, scale + 0.24F, applyAlpha(TITLE_GLOW_COLOR, glowAlpha));
        drawCenteredScaledString(graphics, font, TITLE, centerX, y, scale + 0.11F, applyAlpha(TITLE_HOT_GLOW_COLOR, hotGlowAlpha));
        drawCenteredScaledOutlinedString(
                graphics,
                font,
                TITLE,
                centerX,
                y,
                scale,
                applyAlpha(TITLE_COLOR, alpha),
                applyAlpha(TITLE_OUTLINE_COLOR, alpha));
    }

    private static void drawCenteredScaledString(
            GuiGraphics graphics,
            Font font,
            Component text,
            int centerX,
            int topY,
            float scale,
            int color) {
        String visualText = text.getString();
        float textWidth = font.width(visualText);

        graphics.pose().pushPose();
        graphics.pose().translate(centerX - textWidth * scale / 2.0F, topY, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, visualText, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private static void drawCenteredScaledOutlinedString(
            GuiGraphics graphics,
            Font font,
            Component text,
            int centerX,
            int topY,
            float scale,
            int color,
            int outlineColor) {
        String visualText = text.getString();
        float textWidth = font.width(visualText);
        int outlineOffset = 1;

        graphics.pose().pushPose();
        graphics.pose().translate(centerX - textWidth * scale / 2.0F, topY, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, visualText, -outlineOffset, 0, outlineColor, false);
        graphics.drawString(font, visualText, outlineOffset, 0, outlineColor, false);
        graphics.drawString(font, visualText, 0, -outlineOffset, outlineColor, false);
        graphics.drawString(font, visualText, 0, outlineOffset, outlineColor, false);
        graphics.drawString(font, visualText, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private static int applyAlpha(int color, float alpha) {
        int alphaChannel = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
        return alphaChannel << 24 | color & 0x00FFFFFF;
    }
}