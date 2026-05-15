package com.jvn.secondwind.fabric;

import com.jvn.secondwind.SecondWindCommon;
import net.fabricmc.api.ModInitializer;

public final class SecondWindFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SecondWindCommon.init();
        SecondWindCommon.LOGGER.info("Second Wind Fabric module is scaffolded only; gameplay is not implemented yet.");
    }
}
