package com.jvn.secondwind.client.shader;

import com.jvn.secondwind.SecondWindMod;
import com.jvn.secondwind.config.SecondWindConfig;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import team.lodestar.lodestone.systems.postprocess.PostProcessor;

public final class SecondWindDownedPostProcessor extends PostProcessor {
    public static final SecondWindDownedPostProcessor INSTANCE = new SecondWindDownedPostProcessor();

    private float blend;
    private float urgency;
    private float vignetteStrength;
    private float desaturationStrength;
    private float tintStrength;
    private float bloomStrength;
    private float pulseStrength;

    private SecondWindDownedPostProcessor() {
        setActive(false);
    }

    @Override
    public ResourceLocation getPostChainLocation() {
        return ResourceLocation.fromNamespaceAndPath(SecondWindMod.MOD_ID, "downed_post");
    }

    public void updateState(float blend, float urgency) {
        this.blend = blend;
        this.urgency = urgency;
        this.vignetteStrength = blend * (SecondWindConfig.ENABLE_DOWNED_VIGNETTE.get() ? 0.78F + urgency * 0.18F : 0.0F);
        this.desaturationStrength = blend * (SecondWindConfig.ENABLE_DESATURATION.get() ? 0.72F + urgency * 0.16F : 0.0F);
        this.tintStrength = blend * (SecondWindConfig.ENABLE_DESATURATION.get() ? 0.46F + urgency * 0.32F : 0.0F);
        this.bloomStrength = blend * (SecondWindConfig.ENABLE_DOWNED_BLOOM.get() ? 0.14F + urgency * 0.22F : 0.0F);
        this.pulseStrength = blend * (0.015F + urgency * 0.035F);
        setActive(blend > 0.02F && (vignetteStrength > 0.0F || desaturationStrength > 0.0F || bloomStrength > 0.0F));
    }

    @Override
    public void beforeProcess(Matrix4f viewModelMatrix) {
        if (effects == null) {
            return;
        }

        for (EffectInstance effect : effects) {
            effect.safeGetUniform("DownedBlend").set(blend);
            effect.safeGetUniform("Urgency").set(urgency);
            effect.safeGetUniform("PulseStrength").set(pulseStrength);
            effect.safeGetUniform("VignetteStrength").set(vignetteStrength);
            effect.safeGetUniform("DesaturationStrength").set(desaturationStrength);
            effect.safeGetUniform("TintStrength").set(tintStrength);
            effect.safeGetUniform("BloomStrength").set(bloomStrength);
        }
    }

    @Override
    public void afterProcess() {
    }
}
