package com.jvn.secondwind.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.jvn.secondwind.client.SecondWindConfigScreens;

public final class SecondWindModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SecondWindConfigScreens::create;
    }
}
