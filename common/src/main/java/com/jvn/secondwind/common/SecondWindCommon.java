package com.jvn.secondwind.common;

import com.mojang.logging.LogUtils;
import com.jvn.secondwind.api.EntityBehaviorManager;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public final class SecondWindCommon {
    public static final String MOD_ID = "secondwind";
    public static final Logger LOGGER = LogUtils.getLogger();

    private SecondWindCommon() {
    }

    public static void init() {
        LOGGER.info("{} common scaffolding loaded.", MOD_ID);
        EntityBehaviorManager.register();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
