package com.jvn.secondwind.state;

import com.jvn.secondwind.SecondWindMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.ResourceLocation;

public final class SecondWindData {
    public static final AttachmentType<SecondWindPlayerState> PLAYER_STATE =
            AttachmentRegistry.create(ResourceLocation.fromNamespaceAndPath(SecondWindMod.MOD_ID, "player_state"),
                    builder -> builder
                            .initializer(SecondWindPlayerState::new)
                            .persistent(SecondWindPlayerState.CODEC));

    public static final AttachmentType<SecondWindEntityState> ENTITY_STATE =
            AttachmentRegistry.create(ResourceLocation.fromNamespaceAndPath(SecondWindMod.MOD_ID, "entity_state"),
                    builder -> builder
                            .initializer(SecondWindEntityState::new)
                            .persistent(SecondWindEntityState.CODEC));

    private SecondWindData() {
    }
}
