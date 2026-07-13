package com.jvn.secondwind.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Lets another mod retain ownership of an entity's downed state while Second Wind
 * supplies tracking and player revive channels.
 */
public interface ExternalDownedEntityAdapter {
    boolean isDowned(LivingEntity entity);

    default boolean canRevive(ServerPlayer reviver, LivingEntity entity) {
        return isDowned(entity);
    }

    /** Returns true only when the owning mod actually recovered the entity. */
    boolean revive(ServerPlayer reviver, LivingEntity entity);
}
