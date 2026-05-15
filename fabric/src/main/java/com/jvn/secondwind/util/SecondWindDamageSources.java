package com.jvn.secondwind.util;

import com.jvn.secondwind.state.FailureReason;
import com.jvn.secondwind.state.SecondWindPlayerState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.jvn.secondwind.config.SecondWindConfig;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageTypes;

public final class SecondWindDamageSources {
    private SecondWindDamageSources() {
    }

    public static boolean canTriggerSecondWind(DamageSource source) {
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD) && !SecondWindConfig.ALLOW_VOID_SECOND_WIND.get()) {
            return false;
        }
        if (source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.GENERIC)) {
            return false;
        }
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    public static DamageSource failureSource(ServerPlayer player, SecondWindPlayerState state, FailureReason reason) {
        DamageSource originalSource = state.getOriginalDownedDamageSource();
        String originalMessage = state.getOriginalDownedDeathMessage();
        return new DamageSource(player.damageSources().genericKill().typeHolder()) {
            @Override
            public Component getLocalizedDeathMessage(LivingEntity livingEntity) {
                if (reason != FailureReason.GIVE_UP) {
                    return originalSource != null
                            ? originalSource.getLocalizedDeathMessage(livingEntity)
                            : originalMessage != null && !originalMessage.isBlank()
                                    ? Component.literal(originalMessage)
                            : super.getLocalizedDeathMessage(livingEntity);
                }

                String originalReason = originalReasonText(livingEntity, originalSource);
                if (originalReason == null || originalReason.isBlank()) {
                    return Component.translatable("death.attack.secondwind.easy_way_out", livingEntity.getDisplayName());
                }

                return Component.translatable(
                        "death.attack.secondwind.easy_way_out.because",
                        livingEntity.getDisplayName(),
                        Component.literal(originalReason));
            }
        };
    }

    private static String originalReasonText(LivingEntity victim, DamageSource originalSource) {
        if (originalSource == null) {
            return null;
        }

        String deathMessage = originalSource.getLocalizedDeathMessage(victim).getString();
        String displayName = victim.getDisplayName().getString();
        if (deathMessage.startsWith(displayName + " ")) {
            return normalizeReasonFragment(deathMessage.substring(displayName.length() + 1));
        }

        String name = victim.getName().getString();
        if (deathMessage.startsWith(name + " ")) {
            return normalizeReasonFragment(deathMessage.substring(name.length() + 1));
        }

        return normalizeReasonFragment(deathMessage);
    }

    private static String normalizeReasonFragment(String fragment) {
        if (fragment.startsWith("was ")) {
            return "they were " + fragment.substring(4);
        }
        return "they " + fragment;
    }
}
