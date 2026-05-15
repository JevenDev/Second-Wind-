package com.jvn.secondwind.config;

import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.TranslatableEnum;

public enum CooldownMode implements TranslatableEnum {
    NONE,
    TIMED,
    MC_DAY,
    ON_SLEEP;

    @Override
    public Component getTranslatedName() {
        return Component.translatable("secondwind.configuration.cooldownMode." + name().toLowerCase(Locale.ROOT));
    }
}
