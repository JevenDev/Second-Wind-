package com.jvn.secondwind.api;

import java.util.OptionalDouble;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Optional companion contract for external downed-state adapters that want Second Wind
 * to apply selected revive outcomes after the owning adapter releases the entity.
 * Adapters that do not implement this interface retain full ownership of revival.
 */
public interface ExternalReviveControl {
    /**
     * Selects the entity's health after a successful revive. The configured value comes
     * from the resolved entity-behavior policy and may be replaced with dynamic state.
     */
    default OptionalDouble reviveHealthOverride(
            ServerPlayer reviver,
            LivingEntity entity,
            float configuredHealth) {
        return OptionalDouble.of(configuredHealth);
    }

    /** Whether Second Wind should apply the policy's configured Regeneration effect. */
    default boolean applyConfiguredRegeneration(
            ServerPlayer reviver,
            LivingEntity entity,
            int configuredTicks) {
        return configuredTicks > 0;
    }
}