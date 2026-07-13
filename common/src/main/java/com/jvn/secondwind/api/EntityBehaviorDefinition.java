package com.jvn.secondwind.api;

import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public record EntityBehaviorDefinition(
        ResourceLocation id,
        Target target,
        int priority,
        Lifecycle lifecycle,
        Downed downed,
        Revive revive,
        Presentation presentation) {

    public boolean matches(LivingEntity entity) {
        return target.matches(entity.getType());
    }

    public Optional<ResourceLocation> selectPose(LivingEntity entity) {
        List<ResourceLocation> poses = presentation.poses();
        return poses.isEmpty() ? Optional.empty() : Optional.of(poses.get(Math.floorMod(entity.getUUID().hashCode(), poses.size())));
    }

    public record Target(ResourceLocation entityType, TagKey<EntityType<?>> tag) {
        public boolean exact() {
            return entityType != null;
        }

        public boolean matches(EntityType<?> type) {
            return entityType != null ? EntityType.getKey(type).equals(entityType) : tag != null && type.is(tag);
        }
    }

    public record Lifecycle(Type type, ResourceLocation adapter) {
        public enum Type {
            MANAGED,
            EXTERNAL
        }
    }

    public record Downed(
            Integer timerTicks,
            Integer minimumTimerTicks,
            Integer penaltyPerDownTicks,
            DamageMode damageMode,
            Integer damageCooldownTicks,
            Boolean disableAi,
            Boolean blockHealing) {
        public enum DamageMode {
            IGNORE,
            REDUCE_TIMER,
            NORMAL,
            NORMAL_AND_REDUCE_TIMER
        }
    }

    public record Revive(
            Boolean enabled,
            Integer channelTicks,
            Double distance,
            Float health,
            Integer regenerationTicks,
            Integer invulnerabilityTicks,
            Integer cooldownTicks) {
    }

    public record Presentation(boolean showTimer, boolean announce, List<ResourceLocation> poses) {
        public Presentation {
            poses = List.copyOf(poses);
        }
    }
}
