package com.jvn.secondwind.advancement;

import com.jvn.secondwind.SecondWindMod;
import com.jvn.secondwind.state.ReviveReason;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class SecondWindCriteria {
    public static final SecondWindReviveTrigger REVIVE = new SecondWindReviveTrigger();

    private SecondWindCriteria() {
    }

    public static void register() {
        Registry.register(BuiltInRegistries.TRIGGER_TYPES, ResourceLocation.fromNamespaceAndPath(SecondWindMod.MOD_ID, "revive"), REVIVE);
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
            REVIVE.trigger(revivedPlayer, SecondWindReviveTrigger.ReviveEvent.JUST_IN_TIME);
        }

        switch (reason) {
            case KILL -> REVIVE.trigger(revivedPlayer, SecondWindReviveTrigger.ReviveEvent.KILL_TO_COME_BACK);
            case PLAYER_REVIVE -> {
                REVIVE.trigger(revivedPlayer, SecondWindReviveTrigger.ReviveEvent.REVIVED_BY_PLAYER);
                if (reviver != null && reviver != revivedPlayer) {
                    if (isLastSecondRevive) {
                        REVIVE.trigger(reviver, SecondWindReviveTrigger.ReviveEvent.JUST_IN_TIME);
                    }
                    REVIVE.trigger(reviver, SecondWindReviveTrigger.ReviveEvent.REVIVE_PLAYER);
                    if (downer != null && downer.getUUID().equals(reviver.getUUID())) {
                        REVIVE.trigger(reviver, SecondWindReviveTrigger.ReviveEvent.DOWN_AND_REVIVE);
                    }
                }
            }
            case ADMIN -> {
            }
        }
    }

    public static void triggerFinishHim(ServerPlayer player) {
        REVIVE.trigger(player, SecondWindReviveTrigger.ReviveEvent.FINISH_HIM);
    }
}
