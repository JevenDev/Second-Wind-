package com.jvn.secondwind.api;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class SecondWindClientApi {
    public static final ResourceLocation SIDEWAYS_POSE = SecondWindApi.SIDEWAYS_POSE;
    public static final ResourceLocation UPRIGHT_POSE = SecondWindApi.UPRIGHT_POSE;
    public static final ResourceLocation SWIMMING_POSE = SecondWindApi.SWIMMING_POSE;
    private static final DownedPoseRenderer SIDEWAYS_RENDERER =
            (entity, poseStack, partialTick) -> poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
    private static final Map<ResourceLocation, DownedPoseRenderer> POSE_RENDERERS = new ConcurrentHashMap<>();
    private static volatile Function<Entity, Optional<TrackedDownedEntityState>> lookup = entity -> Optional.empty();

    static {
        POSE_RENDERERS.put(SIDEWAYS_POSE, SIDEWAYS_RENDERER);
        POSE_RENDERERS.put(UPRIGHT_POSE, (entity, poseStack, partialTick) -> {});
        POSE_RENDERERS.put(SWIMMING_POSE, (entity, poseStack, partialTick) -> {});
        POSE_RENDERERS.put(SecondWindApi.LEGACY_CRAWL_POSE, (entity, poseStack, partialTick) -> {});
    }

    private SecondWindClientApi() {
    }

    public static Optional<TrackedDownedEntityState> trackedState(Entity entity) {
        return entity == null ? Optional.empty() : lookup.apply(entity);
    }

    public static void installLookup(Function<Entity, Optional<TrackedDownedEntityState>> implementation) {
        lookup = implementation == null ? entity -> Optional.empty() : implementation;
    }

    /** Registers the model transform used for a datapack presentation pose ID. Call from client initialization. */
    public static void registerPoseRenderer(ResourceLocation id, DownedPoseRenderer renderer) {
        if (id == null || renderer == null) {
            throw new IllegalArgumentException("Second Wind pose id and renderer are required");
        }
        DownedPoseRenderer previous = POSE_RENDERERS.putIfAbsent(id, renderer);
        if (previous != null && previous != renderer) {
            throw new IllegalStateException("Second Wind pose renderer already registered: " + id);
        }
    }

    public static void applyTrackedPose(LivingEntity entity, PoseStack poseStack, float partialTick) {
        trackedState(entity).filter(TrackedDownedEntityState::downed).ifPresent(state -> {
            DownedPoseRenderer renderer = POSE_RENDERERS.get(state.pose());
            if (renderer != null) renderer.apply(entity, poseStack, partialTick);
        });
    }

    @FunctionalInterface
    public interface DownedPoseRenderer {
        void apply(LivingEntity entity, PoseStack poseStack, float partialTick);
    }
}
