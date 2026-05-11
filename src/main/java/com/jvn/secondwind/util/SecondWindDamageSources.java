package com.jvn.secondwind.util;

import com.jvn.secondwind.config.SecondWindConfig;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
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
}
