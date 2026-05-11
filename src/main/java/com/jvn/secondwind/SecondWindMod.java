package com.jvn.secondwind;

import com.mojang.logging.LogUtils;
import com.jvn.secondwind.config.SecondWindConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(SecondWindMod.MOD_ID)
public final class SecondWindMod {
    public static final String MOD_ID = "secondwind";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SecondWindMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, SecondWindConfig.SPEC);
    }
}
