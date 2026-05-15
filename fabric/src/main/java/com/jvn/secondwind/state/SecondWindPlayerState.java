package com.jvn.secondwind.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;

public final class SecondWindPlayerState {
    public static final Codec<SecondWindPlayerState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.BOOL.optionalFieldOf("pendingUnsafeExitCooldown", false).forGetter(SecondWindPlayerState::hasPendingUnsafeExitCooldown),
                    Codec.STRING.optionalFieldOf("originalDownedDeathMessage", "").forGetter(state -> state.originalDownedDeathMessage == null ? "" : state.originalDownedDeathMessage),
                    Codec.INT.optionalFieldOf("downPenaltyCount", 0).forGetter(SecondWindPlayerState::getDownPenaltyCount),
                    Codec.LONG.optionalFieldOf("cooldownExpiresGameTime", 0L).forGetter(SecondWindPlayerState::getCooldownExpiresGameTime),
                    Codec.LONG.optionalFieldOf("cooldownExpiresEpochMillis", 0L).forGetter(SecondWindPlayerState::getCooldownExpiresEpochMillis),
                    Codec.LONG.optionalFieldOf("lastMcDayUsed", -1L).forGetter(SecondWindPlayerState::getLastMcDayUsed),
                    Codec.BOOL.optionalFieldOf("consumedToday", false).forGetter(SecondWindPlayerState::hasConsumedToday),
                    Codec.BOOL.optionalFieldOf("consumedSinceSleep", false).forGetter(SecondWindPlayerState::hasConsumedSinceSleep))
            .apply(instance, SecondWindPlayerState::new));

    private boolean downed;
    private int downedTicksRemaining;
    private int downedMaxTicks;
    private long downedStartGameTime;
    private long lastDownedDamageGameTime;
    private UUID downedByPlayer;
    private int downPenaltyCount;
    private long cooldownExpiresGameTime;
    private long lastMcDayUsed = -1L;
    private boolean consumedToday;
    private boolean consumedSinceSleep;
    private boolean forcedDeathFlow;
    private boolean pendingUnsafeExitCooldown;
    private long cooldownExpiresEpochMillis;
    private UUID reviveChannelReviver;
    private int reviveChannelTicks;
    private int reviveChannelRequiredTicks;
    private long reviveChannelLastHoldGameTime;
    private transient DamageSource originalDownedDamageSource;
    private String originalDownedDeathMessage;

    public SecondWindPlayerState() {
    }

    private SecondWindPlayerState(
            boolean pendingUnsafeExitCooldown,
            String originalDownedDeathMessage,
            int downPenaltyCount,
            long cooldownExpiresGameTime,
            long cooldownExpiresEpochMillis,
            long lastMcDayUsed,
            boolean consumedToday,
            boolean consumedSinceSleep) {
        this.pendingUnsafeExitCooldown = pendingUnsafeExitCooldown;
        this.originalDownedDeathMessage = originalDownedDeathMessage == null || originalDownedDeathMessage.isBlank()
                ? null
                : originalDownedDeathMessage;
        this.downPenaltyCount = Math.max(0, downPenaltyCount);
        this.cooldownExpiresGameTime = Math.max(0L, cooldownExpiresGameTime);
        this.cooldownExpiresEpochMillis = Math.max(0L, cooldownExpiresEpochMillis);
        this.lastMcDayUsed = lastMcDayUsed;
        this.consumedToday = consumedToday;
        this.consumedSinceSleep = consumedSinceSleep;
    }

    public boolean isDowned() {
        return downed;
    }

    public void setDowned(boolean downed) {
        this.downed = downed;
    }

    public int getDownedTicksRemaining() {
        return downedTicksRemaining;
    }

    public void setDownedTicksRemaining(int downedTicksRemaining) {
        this.downedTicksRemaining = Math.max(0, downedTicksRemaining);
    }

    public int getDownedMaxTicks() {
        return downedMaxTicks;
    }

    public void setDownedMaxTicks(int downedMaxTicks) {
        this.downedMaxTicks = Math.max(0, downedMaxTicks);
    }

    public long getDownedStartGameTime() {
        return downedStartGameTime;
    }

    public void setDownedStartGameTime(long downedStartGameTime) {
        this.downedStartGameTime = downedStartGameTime;
    }

    public Optional<UUID> getDownedByPlayer() {
        return Optional.ofNullable(downedByPlayer);
    }

    public void setDownedByPlayer(UUID downedByPlayer) {
        this.downedByPlayer = downedByPlayer;
    }

    public long getLastDownedDamageGameTime() {
        return lastDownedDamageGameTime;
    }

    public void setLastDownedDamageGameTime(long lastDownedDamageGameTime) {
        this.lastDownedDamageGameTime = Math.max(0L, lastDownedDamageGameTime);
    }

    public int getDownPenaltyCount() {
        return downPenaltyCount;
    }

    public void setDownPenaltyCount(int downPenaltyCount) {
        this.downPenaltyCount = Math.max(0, downPenaltyCount);
    }

    public void incrementDownPenaltyCount() {
        this.downPenaltyCount++;
    }

    public long getCooldownExpiresGameTime() {
        return cooldownExpiresGameTime;
    }

    public void setCooldownExpiresGameTime(long cooldownExpiresGameTime) {
        this.cooldownExpiresGameTime = Math.max(0L, cooldownExpiresGameTime);
    }

    public long getCooldownExpiresEpochMillis() {
        return cooldownExpiresEpochMillis;
    }

    public void setCooldownExpiresEpochMillis(long cooldownExpiresEpochMillis) {
        this.cooldownExpiresEpochMillis = Math.max(0L, cooldownExpiresEpochMillis);
    }

    public long getLastMcDayUsed() {
        return lastMcDayUsed;
    }

    public void setLastMcDayUsed(long lastMcDayUsed) {
        this.lastMcDayUsed = lastMcDayUsed;
    }

    public boolean hasConsumedToday() {
        return consumedToday;
    }

    public void setConsumedToday(boolean consumedToday) {
        this.consumedToday = consumedToday;
    }

    public boolean hasConsumedSinceSleep() {
        return consumedSinceSleep;
    }

    public void setConsumedSinceSleep(boolean consumedSinceSleep) {
        this.consumedSinceSleep = consumedSinceSleep;
    }

    public boolean isForcedDeathFlow() {
        return forcedDeathFlow;
    }

    public void setForcedDeathFlow(boolean forcedDeathFlow) {
        this.forcedDeathFlow = forcedDeathFlow;
    }

    public boolean hasPendingUnsafeExitCooldown() {
        return pendingUnsafeExitCooldown;
    }

    public void setPendingUnsafeExitCooldown(boolean pendingUnsafeExitCooldown) {
        this.pendingUnsafeExitCooldown = pendingUnsafeExitCooldown;
    }

    public Optional<UUID> getReviveChannelReviver() {
        return Optional.ofNullable(reviveChannelReviver);
    }

    public void setReviveChannel(UUID reviveChannelReviver, int reviveChannelRequiredTicks) {
        this.reviveChannelReviver = reviveChannelReviver;
        this.reviveChannelTicks = 0;
        this.reviveChannelRequiredTicks = Math.max(0, reviveChannelRequiredTicks);
    }

    public void clearReviveChannel() {
        this.reviveChannelReviver = null;
        this.reviveChannelTicks = 0;
        this.reviveChannelRequiredTicks = 0;
        this.reviveChannelLastHoldGameTime = 0L;
    }

    public int getReviveChannelTicks() {
        return reviveChannelTicks;
    }

    public void setReviveChannelTicks(int reviveChannelTicks) {
        this.reviveChannelTicks = Math.max(0, reviveChannelTicks);
    }

    public int getReviveChannelRequiredTicks() {
        return reviveChannelRequiredTicks;
    }

    public void setReviveChannelRequiredTicks(int reviveChannelRequiredTicks) {
        this.reviveChannelRequiredTicks = Math.max(0, reviveChannelRequiredTicks);
    }

    public long getReviveChannelLastHoldGameTime() {
        return reviveChannelLastHoldGameTime;
    }

    public void setReviveChannelLastHoldGameTime(long reviveChannelLastHoldGameTime) {
        this.reviveChannelLastHoldGameTime = Math.max(0L, reviveChannelLastHoldGameTime);
    }

    public float getReviveChannelProgress() {
        if (reviveChannelRequiredTicks <= 0) {
            return reviveChannelReviver == null ? 0.0F : 1.0F;
        }
        return Math.min(1.0F, reviveChannelTicks / (float) reviveChannelRequiredTicks);
    }

    public DamageSource getOriginalDownedDamageSource() {
        return originalDownedDamageSource;
    }

    public void setOriginalDownedDamageSource(DamageSource originalDownedDamageSource) {
        this.originalDownedDamageSource = originalDownedDamageSource;
    }

    public String getOriginalDownedDeathMessage() {
        return originalDownedDeathMessage;
    }

    public void setOriginalDownedDeathMessage(String originalDownedDeathMessage) {
        this.originalDownedDeathMessage = originalDownedDeathMessage;
    }

    public void clearDownedRuntime() {
        downed = false;
        downedTicksRemaining = 0;
        downedMaxTicks = 0;
        downedStartGameTime = 0L;
        lastDownedDamageGameTime = 0L;
        downedByPlayer = null;
        forcedDeathFlow = false;
        originalDownedDamageSource = null;
        originalDownedDeathMessage = null;
        clearReviveChannel();
    }

    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        boolean unsafeExit = downed || pendingUnsafeExitCooldown;
        tag.putBoolean("PendingUnsafeExitCooldown", unsafeExit);
        if (unsafeExit && originalDownedDeathMessage != null && !originalDownedDeathMessage.isBlank()) {
            tag.putString("OriginalDownedDeathMessage", originalDownedDeathMessage);
        }
        tag.putInt("DownPenaltyCount", downPenaltyCount);
        tag.putLong("CooldownExpiresGameTime", cooldownExpiresGameTime);
        tag.putLong("CooldownExpiresEpochMillis", cooldownExpiresEpochMillis);
        tag.putLong("LastMcDayUsed", lastMcDayUsed);
        tag.putBoolean("ConsumedToday", consumedToday);
        tag.putBoolean("ConsumedSinceSleep", consumedSinceSleep);
        return tag;
    }

    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        clearDownedRuntime();
        pendingUnsafeExitCooldown = tag.getBoolean("PendingUnsafeExitCooldown");
        originalDownedDeathMessage = tag.contains("OriginalDownedDeathMessage")
            ? tag.getString("OriginalDownedDeathMessage")
            : null;
        downPenaltyCount = tag.getInt("DownPenaltyCount");
        cooldownExpiresGameTime = tag.getLong("CooldownExpiresGameTime");
        cooldownExpiresEpochMillis = tag.getLong("CooldownExpiresEpochMillis");
        lastMcDayUsed = tag.contains("LastMcDayUsed") ? tag.getLong("LastMcDayUsed") : -1L;
        consumedToday = tag.getBoolean("ConsumedToday");
        consumedSinceSleep = tag.getBoolean("ConsumedSinceSleep");
    }
}
