package com.jvn.secondwind;

import com.jvn.secondwind.advancement.SecondWindCriteria;
import com.jvn.secondwind.common.SecondWindCommon;
import com.jvn.toucanlib.client.ToucanClientOnly;
import com.jvn.secondwind.config.SecondWindConfig;
import com.jvn.secondwind.item.SecondWindItems;
import com.jvn.secondwind.network.SecondWindNetworking;
import com.jvn.secondwind.state.SecondWindData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(SecondWindMod.MOD_ID)
public final class SecondWindMod {
    public static final String MOD_ID = SecondWindCommon.MOD_ID;
    public static final Logger LOGGER = SecondWindCommon.LOGGER;

    public SecondWindMod(IEventBus modEventBus, ModContainer modContainer) {
        SecondWindCommon.init();
        SecondWindCriteria.TRIGGER_TYPES.register(modEventBus);
        SecondWindData.ATTACHMENT_TYPES.register(modEventBus);
        SecondWindItems.register(modEventBus);
        modEventBus.addListener(SecondWindNetworking::registerPayloads);
        modContainer.registerConfig(ModConfig.Type.COMMON, SecondWindConfig.SPEC);
        ToucanClientOnly.run(() -> {
            com.jvn.secondwind.client.SecondWindClient.register(modEventBus);
            com.jvn.secondwind.client.SecondWindConfigScreens.register(modContainer);
        });
    }
}
