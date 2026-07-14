package com.jvn.secondwind.mixin.client;

import com.jvn.secondwind.api.SecondWindClientApi;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
abstract class LivingEntityRendererMixin {
    @Inject(method = "setupRotations", at = @At("TAIL"))
    private void secondwind$applyDownedPose(
            LivingEntity entity,
            PoseStack poseStack,
            float bob,
            float yBodyRot,
            float partialTick,
            float scale,
            CallbackInfo callback) {
        SecondWindClientApi.applyTrackedPose(entity, poseStack, partialTick);
    }

    @Inject(method = "getOverlayCoords", at = @At("HEAD"), cancellable = true)
    private static void secondwind$retainDownedDamageOverlay(
            LivingEntity entity,
            float whiteOverlayProgress,
            CallbackInfoReturnable<Integer> callback) {
        if (SecondWindClientApi.trackedState(entity).filter(state -> state.downed()).isPresent()) {
            callback.setReturnValue(OverlayTexture.pack(OverlayTexture.u(whiteOverlayProgress), OverlayTexture.v(true)));
        }
    }
}
