package com.jvn.secondwind.client.shader;

import com.jvn.secondwind.SecondWindMod;
import com.google.gson.JsonParseException;
import com.jvn.secondwind.config.SecondWindConfig;
import com.jvn.toucanlib.util.ToucanResourceLocations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.io.IOException;

@EventBusSubscriber(modid = SecondWindMod.MOD_ID, value = Dist.CLIENT)
public final class SecondWindDownedPostProcessor {
    public static final SecondWindDownedPostProcessor INSTANCE = new SecondWindDownedPostProcessor();
    private static final ResourceLocation POST_CHAIN_LOCATION =
            ToucanResourceLocations.id(SecondWindMod.MOD_ID, "shaders/post/downed_post.json");

    private float blend;
    private float urgency;
    private float vignetteStrength;
    private float desaturationStrength;
    private float tintStrength;
    private float bloomStrength;
    private float pulseStrength;
    private float time;
    private PostChain postChain;
    private boolean active;
    private int cachedWidth = -1;
    private int cachedHeight = -1;

    private SecondWindDownedPostProcessor() {
    }

    public void reload() {
        close();
        time = 0.0F;
    }

    public void updateState(float blend, float urgency) {
        this.blend = blend;
        this.urgency = urgency;
        this.vignetteStrength = blend * (SecondWindConfig.ENABLE_DOWNED_VIGNETTE.get() ? 0.78F + urgency * 0.18F : 0.0F);
        this.desaturationStrength = blend * (SecondWindConfig.ENABLE_DESATURATION.get() ? 0.72F + urgency * 0.16F : 0.0F);
        this.tintStrength = blend * (SecondWindConfig.ENABLE_DESATURATION.get() ? 0.46F + urgency * 0.32F : 0.0F);
        this.bloomStrength = 0.0F;
        this.pulseStrength = blend * (0.015F + urgency * 0.035F);
        this.active = blend > 0.02F && (vignetteStrength > 0.0F || desaturationStrength > 0.0F || bloomStrength > 0.0F);
        if (!this.active) {
            time = 0.0F;
        }
    }

    private void render(RenderLevelStageEvent event) {
        if (!active) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        ensurePostChain(minecraft);
        if (postChain == null) {
            return;
        }

        resizeIfNeeded(minecraft);

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        time += partialTick / 20.0F;

        postChain.setUniform("time", time);
        postChain.setUniform("aspectRatio", cachedHeight <= 0 ? 1.0F : (float) cachedWidth / (float) cachedHeight);
        postChain.setUniform("DownedBlend", blend);
        postChain.setUniform("Urgency", urgency);
        postChain.setUniform("PulseStrength", pulseStrength);
        postChain.setUniform("VignetteStrength", vignetteStrength);
        postChain.setUniform("DesaturationStrength", desaturationStrength);
        postChain.setUniform("TintStrength", tintStrength);
        postChain.setUniform("BloomStrength", bloomStrength);
        postChain.process(partialTick);
        minecraft.getMainRenderTarget().bindWrite(false);
    }

    private void ensurePostChain(Minecraft minecraft) {
        if (postChain != null) {
            return;
        }

        try {
            postChain = new PostChain(
                    minecraft.getTextureManager(),
                    minecraft.getResourceManager(),
                    minecraft.getMainRenderTarget(),
                    POST_CHAIN_LOCATION);
            cachedWidth = minecraft.getWindow().getWidth();
            cachedHeight = minecraft.getWindow().getHeight();
            postChain.resize(cachedWidth, cachedHeight);
        } catch (IOException | JsonParseException exception) {
            SecondWindMod.LOGGER.error("Failed to load Second Wind post-processing shader", exception);
            close();
        }
    }

    private void resizeIfNeeded(Minecraft minecraft) {
        int width = minecraft.getWindow().getWidth();
        int height = minecraft.getWindow().getHeight();
        if (width == cachedWidth && height == cachedHeight) {
            return;
        }

        cachedWidth = width;
        cachedHeight = height;
        postChain.resize(width, height);
    }

    private void close() {
        if (postChain != null) {
            postChain.close();
            postChain = null;
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            INSTANCE.render(event);
        }
    }
}
