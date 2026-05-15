package com.jvn.secondwind;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class SecondWindCommon {
    public static final String MOD_ID = "secondwind";
    public static final Logger LOGGER = LogUtils.getLogger();

    private SecondWindCommon() {
    }

    public static void init() {
        LOGGER.info("{} common scaffolding loaded.", MOD_ID);
    }
}
