package com.jvn.secondwind.advancement;

import com.jvn.secondwind.SecondWindMod;
import com.jvn.secondwind.state.ReviveReason;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SecondWindCriteria {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGER_TYPES =
            DeferredRegister.create(Registries.TRIGGER_TYPE, SecondWindMod.MOD_ID);

    public static final DeferredHolder<CriterionTrigger<?>, SecondWindReviveTrigger> REVIVE =
            TRIGGER_TYPES.register("revive", SecondWindReviveTrigger::new);

    private SecondWindCriteria() {
    }

    public static void triggerRevive(
            ServerPlayer revivedPlayer,
            ReviveReason reason,
            int remainingTicks,
            int lastSecondThresholdTicks,
            ServerPlayer reviver,
            ServerPlayer downer) {
        boolean isLastSecondRevive = remainingTicks > 0 && remainingTicks <= lastSecondThresholdTicks;
        if (reason == ReviveReason.ADMIN) {
            return;
        }

        if (isLastSecondRevive) {
            REVIVE.get().trigger(revivedPlayer, SecondWindReviveTrigger.ReviveEvent.JUST_IN_TIME);
        }

        switch (reason) {
            case KILL -> REVIVE.get().trigger(revivedPlayer, SecondWindReviveTrigger.ReviveEvent.KILL_TO_COME_BACK);
            case PLAYER_REVIVE -> {
                REVIVE.get().trigger(revivedPlayer, SecondWindReviveTrigger.ReviveEvent.REVIVED_BY_PLAYER);
                if (reviver != null && reviver != revivedPlayer) {
                    if (isLastSecondRevive) {
                        REVIVE.get().trigger(reviver, SecondWindReviveTrigger.ReviveEvent.JUST_IN_TIME);
                    }
                    REVIVE.get().trigger(reviver, SecondWindReviveTrigger.ReviveEvent.REVIVE_PLAYER);
                    if (downer != null && downer.getUUID().equals(reviver.getUUID())) {
                        REVIVE.get().trigger(reviver, SecondWindReviveTrigger.ReviveEvent.DOWN_AND_REVIVE);
                    }
                }
            }
            case ADMIN -> {
            }
        }
    }

    public static void triggerFinishHim(ServerPlayer player) {
        REVIVE.get().trigger(player, SecondWindReviveTrigger.ReviveEvent.FINISH_HIM);
    }
}