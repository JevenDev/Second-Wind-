package com.jvn.secondwind.client;

import com.jvn.secondwind.config.SecondWindConfig;
import io.wispforest.owo.config.ui.ConfigScreen;
import net.minecraft.client.gui.screens.Screen;

public final class SecondWindConfigScreens {
    private SecondWindConfigScreens() {
    }

    public static Screen create(Screen parent) {
        return ConfigScreen.create(SecondWindConfig.CONFIG, parent);
    }
}
