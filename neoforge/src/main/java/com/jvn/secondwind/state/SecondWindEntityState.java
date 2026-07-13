package com.jvn.secondwind.state;

import com.jvn.secondwind.api.ResolvedEntityPolicy;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

public final class SecondWindEntityState implements INBTSerializable<CompoundTag> {
    private boolean downed;
    private boolean forcedDeathFlow;
    private int ticksRemaining;
    private int maxTicks;
    private int downCount;
    private int cooldownTicks;
    private long lastDamageGameTime;
    private UUID reviveChannelReviver;
    private int reviveChannelTicks;
    private long reviveChannelLastHoldGameTime;
    private boolean capturedFlags;
    private boolean previousNoAi;
    private boolean previousCanPickUpLoot;
    private String previousPose = "standing";
    private ResolvedEntityPolicy policy;
    private String originalDeathMessage = "";

    public boolean isDowned() { return downed; }
    public void setDowned(boolean value) { downed = value; }
    public boolean isForcedDeathFlow() { return forcedDeathFlow; }
    public void setForcedDeathFlow(boolean value) { forcedDeathFlow = value; }
    public int ticksRemaining() { return ticksRemaining; }
    public void setTicksRemaining(int value) { ticksRemaining = Math.max(0, value); }
    public int maxTicks() { return maxTicks; }
    public void setMaxTicks(int value) { maxTicks = Math.max(0, value); }
    public int downCount() { return downCount; }
    public void incrementDownCount() { downCount++; }
    public int cooldownTicks() { return cooldownTicks; }
    public void setCooldownTicks(int value) { cooldownTicks = Math.max(0, value); }
    public long lastDamageGameTime() { return lastDamageGameTime; }
    public void setLastDamageGameTime(long value) { lastDamageGameTime = Math.max(0L, value); }
    public Optional<UUID> reviveChannelReviver() { return Optional.ofNullable(reviveChannelReviver); }
    public int reviveChannelTicks() { return reviveChannelTicks; }
    public long reviveChannelLastHoldGameTime() { return reviveChannelLastHoldGameTime; }
    public void beginReviveChannel(UUID reviver, long gameTime) { reviveChannelReviver = reviver; reviveChannelTicks = 0; reviveChannelLastHoldGameTime = gameTime; }
    public void refreshReviveChannel(long gameTime) { reviveChannelLastHoldGameTime = gameTime; }
    public void advanceReviveChannel() { reviveChannelTicks++; }
    public void clearReviveChannel() { reviveChannelReviver = null; reviveChannelTicks = 0; reviveChannelLastHoldGameTime = 0L; }
    public boolean capturedFlags() { return capturedFlags; }
    public void captureFlags(boolean noAi, boolean pickup, String pose) { capturedFlags = true; previousNoAi = noAi; previousCanPickUpLoot = pickup; previousPose = pose; }
    public boolean previousNoAi() { return previousNoAi; }
    public boolean previousCanPickUpLoot() { return previousCanPickUpLoot; }
    public String previousPose() { return previousPose; }
    public ResolvedEntityPolicy policy() { return policy; }
    public void setPolicy(ResolvedEntityPolicy value) { policy = value; }
    public String originalDeathMessage() { return originalDeathMessage; }
    public void setOriginalDeathMessage(String value) { originalDeathMessage = value == null ? "" : value; }

    public void clearDownedRuntime() {
        downed = false;
        forcedDeathFlow = false;
        ticksRemaining = 0;
        maxTicks = 0;
        lastDamageGameTime = 0L;
        capturedFlags = false;
        policy = null;
        originalDeathMessage = "";
        clearReviveChannel();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return saveTag();
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        loadTag(tag);
    }

    private CompoundTag saveTag() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Downed", downed);
        tag.putBoolean("ForcedDeathFlow", forcedDeathFlow);
        tag.putInt("TicksRemaining", ticksRemaining);
        tag.putInt("MaxTicks", maxTicks);
        tag.putInt("DownCount", downCount);
        tag.putInt("CooldownTicks", cooldownTicks);
        tag.putLong("LastDamageGameTime", lastDamageGameTime);
        if (reviveChannelReviver != null) tag.putUUID("Reviver", reviveChannelReviver);
        tag.putInt("ReviveChannelTicks", reviveChannelTicks);
        tag.putLong("ReviveLastHold", reviveChannelLastHoldGameTime);
        tag.putBoolean("CapturedFlags", capturedFlags);
        tag.putBoolean("PreviousNoAi", previousNoAi);
        tag.putBoolean("PreviousCanPickUpLoot", previousCanPickUpLoot);
        tag.putString("PreviousPose", previousPose);
        if (policy != null) tag.put("Policy", policy.save());
        if (!originalDeathMessage.isBlank()) tag.putString("OriginalDeathMessage", originalDeathMessage);
        return tag;
    }

    private void loadTag(CompoundTag tag) {
        downed = tag.getBoolean("Downed");
        forcedDeathFlow = tag.getBoolean("ForcedDeathFlow");
        ticksRemaining = tag.getInt("TicksRemaining");
        maxTicks = tag.getInt("MaxTicks");
        downCount = tag.getInt("DownCount");
        cooldownTicks = tag.getInt("CooldownTicks");
        lastDamageGameTime = tag.getLong("LastDamageGameTime");
        reviveChannelReviver = tag.hasUUID("Reviver") ? tag.getUUID("Reviver") : null;
        reviveChannelTicks = tag.getInt("ReviveChannelTicks");
        reviveChannelLastHoldGameTime = tag.getLong("ReviveLastHold");
        capturedFlags = tag.getBoolean("CapturedFlags");
        previousNoAi = tag.getBoolean("PreviousNoAi");
        previousCanPickUpLoot = tag.getBoolean("PreviousCanPickUpLoot");
        previousPose = tag.contains("PreviousPose") ? tag.getString("PreviousPose") : "standing";
        policy = tag.contains("Policy") ? ResolvedEntityPolicy.load(tag.getCompound("Policy")) : null;
        originalDeathMessage = tag.getString("OriginalDeathMessage");
    }
}
