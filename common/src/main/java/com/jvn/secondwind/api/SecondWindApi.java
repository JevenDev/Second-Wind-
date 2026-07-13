package com.jvn.secondwind.api;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public final class SecondWindApi {
    private static final Map<ResourceLocation, ExternalDownedEntityAdapter> EXTERNAL_ADAPTERS = new ConcurrentHashMap<>();
    private static volatile EntityRuntime runtime = EntityRuntime.NOOP;

    private SecondWindApi() {
    }

    public static void registerExternalAdapter(ResourceLocation id, ExternalDownedEntityAdapter adapter) {
        if (id == null || adapter == null) {
            throw new IllegalArgumentException("Second Wind external adapter id and implementation are required");
        }
        ExternalDownedEntityAdapter previous = EXTERNAL_ADAPTERS.putIfAbsent(id, adapter);
        if (previous != null && previous != adapter) {
            throw new IllegalStateException("Second Wind external adapter already registered: " + id);
        }
    }

    public static Optional<ExternalDownedEntityAdapter> externalAdapter(ResourceLocation id) {
        return Optional.ofNullable(EXTERNAL_ADAPTERS.get(id));
    }

    public static boolean hasExternalAdapter(ResourceLocation id) {
        return EXTERNAL_ADAPTERS.containsKey(id);
    }

    public static void notifyExternalStateChanged(LivingEntity entity) {
        if (entity != null && !entity.level().isClientSide()) {
            runtime.notifyExternalStateChanged(entity);
        }
    }

    public static Optional<ResourceLocation> resolvePresentationPose(LivingEntity entity) {
        return EntityBehaviorManager.resolve(entity).flatMap(definition -> definition.selectPose(entity));
    }

    public static void installRuntime(EntityRuntime implementation) {
        runtime = implementation == null ? EntityRuntime.NOOP : implementation;
    }

    public interface EntityRuntime {
        EntityRuntime NOOP = entity -> {
        };

        void notifyExternalStateChanged(LivingEntity entity);
    }
}
