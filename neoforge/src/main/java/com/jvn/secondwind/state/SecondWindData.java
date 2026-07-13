package com.jvn.secondwind.state;

import com.jvn.secondwind.SecondWindMod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class SecondWindData {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, SecondWindMod.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<SecondWindPlayerState>> PLAYER_STATE =
            ATTACHMENT_TYPES.register("player_state", () -> AttachmentType
                    .serializable(SecondWindPlayerState::new)
                    .copyOnDeath()
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<SecondWindEntityState>> ENTITY_STATE =
            ATTACHMENT_TYPES.register("entity_state", () -> AttachmentType
                    .serializable(SecondWindEntityState::new)
                    .build());

    private SecondWindData() {
    }
}
