package com.jvn.secondwind;

import com.mojang.logging.LogUtils;
import com.jvn.secondwind.config.SecondWindConfig;
import com.jvn.secondwind.network.SecondWindNetworking;
import com.jvn.secondwind.state.SecondWindData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(SecondWindMod.MOD_ID)
public final class SecondWindMod {
    public static final String MOD_ID = "secondwind";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SecondWindMod(IEventBus modEventBus, ModContainer modContainer) {
        SecondWindData.ATTACHMENT_TYPES.register(modEventBus);
        modEventBus.addListener(SecondWindNetworking::registerPayloads);
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, SecondWindConfig.SPEC);
    }
}
