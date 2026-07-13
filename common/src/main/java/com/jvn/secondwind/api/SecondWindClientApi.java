package com.jvn.secondwind.api;

import java.util.Optional;
import java.util.function.Function;
import net.minecraft.world.entity.Entity;

public final class SecondWindClientApi {
    private static volatile Function<Entity, Optional<TrackedDownedEntityState>> lookup = entity -> Optional.empty();

    private SecondWindClientApi() {
    }

    public static Optional<TrackedDownedEntityState> trackedState(Entity entity) {
        return entity == null ? Optional.empty() : lookup.apply(entity);
    }

    public static void installLookup(Function<Entity, Optional<TrackedDownedEntityState>> implementation) {
        lookup = implementation == null ? entity -> Optional.empty() : implementation;
    }
}
