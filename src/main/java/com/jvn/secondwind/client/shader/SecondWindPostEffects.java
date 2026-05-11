package com.jvn.secondwind.client.shader;

import com.jvn.secondwind.client.ClientSecondWindState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import team.lodestar.lodestone.systems.postprocess.PostProcessHandler;

public final class SecondWindPostEffects {
    private static float downedBlend;

    private SecondWindPostEffects() {
    }

    public static void register() {
        // Lodestone applies its default uniforms to every pass, so the registered processor
        // owns a companion copy pass instead of relying on vanilla blit.
        PostProcessHandler.addInstance(SecondWindDownedPostProcessor.INSTANCE);
    }

    public static void tickClient() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean shouldRender = minecraft.level != null && minecraft.player != null && ClientSecondWindState.isDowned();
        float targetBlend = shouldRender ? 1.0F : 0.0F;
        float step = shouldRender ? 0.08F : 0.12F;
        downedBlend = Mth.approach(downedBlend, targetBlend, step);

        if (downedBlend < 0.001F) {
            downedBlend = 0.0F;
        }

        float urgency = 0.0F;
        if (shouldRender && ClientSecondWindState.maxTicks() > 0) {
            urgency = 1.0F - (float) ClientSecondWindState.ticksRemaining() / (float) ClientSecondWindState.maxTicks();
        }

        SecondWindDownedPostProcessor.INSTANCE.updateState(downedBlend, Mth.clamp(urgency, 0.0F, 1.0F));
    }
}
