package com.jvn.secondwind.state;

import com.jvn.secondwind.api.ResolvedEntityPolicy;
import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

public final class SecondWindEntityState {
    public static final Codec<SecondWindEntityState> CODEC = CompoundTag.CODEC.xmap(SecondWindEntityState::fromTag, SecondWindEntityState::saveTag);
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
        downed = false; forcedDeathFlow = false; ticksRemaining = 0; maxTicks = 0; lastDamageGameTime = 0L;
        capturedFlags = false; policy = null; originalDeathMessage = ""; clearReviveChannel();
    }

    private CompoundTag saveTag() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Downed", downed); tag.putBoolean("ForcedDeathFlow", forcedDeathFlow);
        tag.putInt("TicksRemaining", ticksRemaining); tag.putInt("MaxTicks", maxTicks);
        tag.putInt("DownCount", downCount); tag.putInt("CooldownTicks", cooldownTicks);
        tag.putLong("LastDamageGameTime", lastDamageGameTime);
        if (reviveChannelReviver != null) tag.putUUID("Reviver", reviveChannelReviver);
        tag.putInt("ReviveChannelTicks", reviveChannelTicks); tag.putLong("ReviveLastHold", reviveChannelLastHoldGameTime);
        tag.putBoolean("CapturedFlags", capturedFlags); tag.putBoolean("PreviousNoAi", previousNoAi);
        tag.putBoolean("PreviousCanPickUpLoot", previousCanPickUpLoot);
        tag.putString("PreviousPose", previousPose);
        if (policy != null) tag.put("Policy", policy.save());
        if (!originalDeathMessage.isBlank()) tag.putString("OriginalDeathMessage", originalDeathMessage);
        return tag;
    }

    private static SecondWindEntityState fromTag(CompoundTag tag) {
        SecondWindEntityState state = new SecondWindEntityState();
        state.downed = tag.getBoolean("Downed"); state.forcedDeathFlow = tag.getBoolean("ForcedDeathFlow");
        state.ticksRemaining = tag.getInt("TicksRemaining"); state.maxTicks = tag.getInt("MaxTicks");
        state.downCount = tag.getInt("DownCount"); state.cooldownTicks = tag.getInt("CooldownTicks");
        state.lastDamageGameTime = tag.getLong("LastDamageGameTime");
        state.reviveChannelReviver = tag.hasUUID("Reviver") ? tag.getUUID("Reviver") : null;
        state.reviveChannelTicks = tag.getInt("ReviveChannelTicks"); state.reviveChannelLastHoldGameTime = tag.getLong("ReviveLastHold");
        state.capturedFlags = tag.getBoolean("CapturedFlags"); state.previousNoAi = tag.getBoolean("PreviousNoAi");
        state.previousCanPickUpLoot = tag.getBoolean("PreviousCanPickUpLoot");
        state.previousPose = tag.contains("PreviousPose") ? tag.getString("PreviousPose") : "standing";
        state.policy = tag.contains("Policy") ? ResolvedEntityPolicy.load(tag.getCompound("Policy")) : null;
        state.originalDeathMessage = tag.getString("OriginalDeathMessage");
        return state;
    }
}
