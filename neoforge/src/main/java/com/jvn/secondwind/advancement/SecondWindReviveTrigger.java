package com.jvn.secondwind.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

public final class SecondWindReviveTrigger extends SimpleCriterionTrigger<SecondWindReviveTrigger.TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, ReviveEvent event) {
        trigger(player, instance -> instance.matches(event));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, ReviveEvent event)
            implements SimpleCriterionTrigger.SimpleInstance {
        private static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                        ReviveEvent.CODEC.fieldOf("event").forGetter(TriggerInstance::event))
                .apply(instance, TriggerInstance::new));

        public static Criterion<TriggerInstance> hasEvent(ReviveEvent event) {
            return SecondWindCriteria.REVIVE.get().createCriterion(new TriggerInstance(Optional.empty(), event));
        }

        public boolean matches(ReviveEvent event) {
            return this.event == event;
        }
    }

    public enum ReviveEvent {
        JUST_IN_TIME,
        KILL_TO_COME_BACK,
        REVIVE_PLAYER,
        REVIVED_BY_PLAYER,
        FINISH_HIM,
        DOWN_AND_REVIVE;

        private static final Codec<ReviveEvent> CODEC = Codec.STRING.comapFlatMap(
                name -> byName(name)
                        .map(DataResult::success)
                        .orElseGet(() -> DataResult.error(() -> "Unknown Second Wind revive event: " + name)),
                ReviveEvent::serializedName);

        private String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        private static Optional<ReviveEvent> byName(String name) {
            for (ReviveEvent event : values()) {
                if (event.serializedName().equals(name)) {
                    return Optional.of(event);
                }
            }
            return Optional.empty();
        }
    }
}