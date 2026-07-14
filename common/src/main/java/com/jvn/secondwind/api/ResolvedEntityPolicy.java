package com.jvn.secondwind.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** A durable snapshot of the behavior selected when an entity becomes downed. */
public record ResolvedEntityPolicy(
        ResourceLocation definitionId,
        EntityBehaviorDefinition.Lifecycle.Type lifecycle,
        ResourceLocation adapter,
        int timerTicks,
        int minimumTimerTicks,
        int penaltyPerDownTicks,
        EntityBehaviorDefinition.Downed.DamageMode damageMode,
        int damageCooldownTicks,
        boolean disableAi,
        boolean blockHealing,
        boolean reviveEnabled,
        int reviveChannelTicks,
        double reviveDistance,
        float reviveHealth,
        int regenerationTicks,
        int invulnerabilityTicks,
        int cooldownTicks,
        boolean showTimer,
        boolean announce,
        String downedMessageTranslationKey,
        String downedMessageFallback,
        String downedMessageText,
        ResourceLocation pose) {

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Definition", definitionId.toString());
        tag.putString("Lifecycle", lifecycle.name());
        if (adapter != null) tag.putString("Adapter", adapter.toString());
        tag.putInt("Timer", timerTicks);
        tag.putInt("MinimumTimer", minimumTimerTicks);
        tag.putInt("Penalty", penaltyPerDownTicks);
        tag.putString("DamageMode", damageMode.name());
        tag.putInt("DamageCooldown", damageCooldownTicks);
        tag.putBoolean("DisableAi", disableAi);
        tag.putBoolean("BlockHealing", blockHealing);
        tag.putBoolean("ReviveEnabled", reviveEnabled);
        tag.putInt("ReviveChannel", reviveChannelTicks);
        tag.putDouble("ReviveDistance", reviveDistance);
        tag.putFloat("ReviveHealth", reviveHealth);
        tag.putInt("Regeneration", regenerationTicks);
        tag.putInt("Invulnerability", invulnerabilityTicks);
        tag.putInt("Cooldown", cooldownTicks);
        tag.putBoolean("ShowTimer", showTimer);
        tag.putBoolean("Announce", announce);
        if (downedMessageTranslationKey != null) tag.putString("DownedMessageTranslationKey", downedMessageTranslationKey);
        if (downedMessageFallback != null) tag.putString("DownedMessageFallback", downedMessageFallback);
        if (downedMessageText != null) tag.putString("DownedMessageText", downedMessageText);
        tag.putString("Pose", pose.toString());
        return tag;
    }

    public static ResolvedEntityPolicy load(CompoundTag tag) {
        return new ResolvedEntityPolicy(
                ResourceLocation.parse(tag.getString("Definition")),
                EntityBehaviorDefinition.Lifecycle.Type.valueOf(tag.getString("Lifecycle")),
                tag.contains("Adapter") ? ResourceLocation.parse(tag.getString("Adapter")) : null,
                tag.getInt("Timer"),
                tag.getInt("MinimumTimer"),
                tag.getInt("Penalty"),
                EntityBehaviorDefinition.Downed.DamageMode.valueOf(tag.getString("DamageMode")),
                tag.getInt("DamageCooldown"),
                tag.getBoolean("DisableAi"),
                tag.getBoolean("BlockHealing"),
                tag.getBoolean("ReviveEnabled"),
                tag.getInt("ReviveChannel"),
                tag.getDouble("ReviveDistance"),
                tag.getFloat("ReviveHealth"),
                tag.getInt("Regeneration"),
                tag.getInt("Invulnerability"),
                tag.getInt("Cooldown"),
                tag.getBoolean("ShowTimer"),
                tag.getBoolean("Announce"),
                tag.contains("DownedMessageText") || tag.contains("DownedMessageLiteral") ? null
                        : (tag.contains("DownedMessageTranslationKey") ? tag.getString("DownedMessageTranslationKey") : null),
                tag.contains("DownedMessageFallback") ? tag.getString("DownedMessageFallback") : null,
                tag.contains("DownedMessageText") ? tag.getString("DownedMessageText")
                        : (tag.contains("DownedMessageLiteral") ? tag.getString("DownedMessageLiteral")
                        : (tag.contains("DownedMessageTranslationKey") ? null : "%1$s was downed!")),
                tag.contains("Pose") ? ResourceLocation.parse(tag.getString("Pose")) : ResourceLocation.fromNamespaceAndPath("secondwind", "sideways"));
    }
}
