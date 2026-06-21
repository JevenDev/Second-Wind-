package com.jvn.secondwind.client;

import com.jvn.secondwind.SecondWindMod;
import com.jvn.secondwind.config.SecondWindConfig;
import io.wispforest.owo.config.ui.ConfigScreen;
import io.wispforest.owo.config.ui.ConfigScreenProviders;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class SecondWindConfigScreens {
    private SecondWindConfigScreens() {
    }

    public static void register(ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, SecondWindConfigScreens::create);
    }

    private static Screen create(ModContainer modContainer, Screen parent) {
        var provider = ConfigScreenProviders.get(SecondWindMod.MOD_ID);
        return provider == null ? ConfigScreen.create(SecondWindConfig.CONFIG, parent) : provider.apply(parent);
    }
}
