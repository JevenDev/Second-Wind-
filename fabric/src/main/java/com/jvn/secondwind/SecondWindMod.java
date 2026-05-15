package com.jvn.secondwind;

import com.jvn.secondwind.advancement.SecondWindCriteria;
import com.jvn.secondwind.config.SecondWindConfig;
import com.jvn.secondwind.event.SecondWindCommands;
import com.jvn.secondwind.event.SecondWindServerEvents;
import com.jvn.secondwind.item.SecondWindItems;
import com.jvn.secondwind.network.SecondWindNetworking;
import net.fabricmc.api.ModInitializer;

public final class SecondWindMod implements ModInitializer {
    public static final String MOD_ID = SecondWindCommon.MOD_ID;
    public static final org.slf4j.Logger LOGGER = SecondWindCommon.LOGGER;

    @Override
    public void onInitialize() {
        SecondWindCommon.init();
        SecondWindConfig.load();
        SecondWindItems.register();
        SecondWindCriteria.register();
        SecondWindNetworking.registerPayloads();
        SecondWindCommands.register();
        SecondWindServerEvents.register();
    }
}
