package com.jvn.secondwind.config;

import java.util.Locale;
import net.minecraft.network.chat.Component;

public enum CooldownMode {
    NONE,
    TIMED,
    MC_DAY,
    ON_SLEEP;

    public Component getTranslatedName() {
        return Component.translatable("secondwind.configuration.cooldownMode." + name().toLowerCase(Locale.ROOT));
    }
}
