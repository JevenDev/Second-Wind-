package com.jvn.secondwind.client;

import com.jvn.toucanlib.neoforge.config.toucanConfigScreens;
import java.util.function.BiFunction;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

public final class SecondWindConfigScreens {
    private SecondWindConfigScreens() {
    }

    public static void register(ModContainer modContainer) {
        toucanConfigScreens.register(modContainer, (BiFunction<ModContainer, Screen, Screen>) ConfigurationScreen::new);
    }
}
