package com.jvn.secondwind.state;

import com.jvn.secondwind.api.EntityBehaviorDefinition;
import com.jvn.secondwind.api.EntityBehaviorManager;
import com.jvn.secondwind.api.AnnouncementMessage;
import com.jvn.secondwind.api.ExternalDownedEntityAdapter;
import com.jvn.secondwind.api.ResolvedEntityPolicy;
import com.jvn.secondwind.api.SecondWindApi;
import com.jvn.secondwind.config.CooldownMode;
import com.jvn.secondwind.config.SecondWindConfig;
import com.jvn.secondwind.network.SecondWindNetworking;
import com.jvn.secondwind.util.SecondWindDamageSources;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

public final class SecondWindEntityService {
    private static final int REVIVE_HOLD_GRACE_TICKS = 2;
    private static final Map<UUID, LivingEntity> ACTIVE = new ConcurrentHashMap<>();

    private SecondWindEntityService() {}

    public static void init() { SecondWindApi.installRuntime(SecondWindEntityService::notifyExternalStateChanged); }

    public static SecondWindEntityState getState(LivingEntity entity) {
        return ((AttachmentTarget) entity).getAttachedOrCreate(SecondWindData.ENTITY_STATE);
    }

    public static boolean isDowned(LivingEntity entity) {
        return entity instanceof ServerPlayer player ? SecondWindService.isDowned(player) : getState(entity).isDowned();
    }

    public static boolean tryDownFromDeath(LivingEntity entity, DamageSource source) {
        if (entity instanceof Player || entity.level().isClientSide()) return false;
        SecondWindEntityState state = getState(entity);
        if (state.isDowned() || state.isForcedDeathFlow() || state.cooldownTicks() > 0) return false;
        EntityBehaviorDefinition definition = EntityBehaviorManager.resolve(entity).orElse(null);
        if (definition == null || definition.lifecycle().type() != EntityBehaviorDefinition.Lifecycle.Type.MANAGED
                || !SecondWindDamageSources.canTriggerSecondWind(source)) return false;
        ResolvedEntityPolicy policy = resolvePolicy(entity, definition);
        int timer = Math.max(policy.minimumTimerTicks(), policy.timerTicks() - state.downCount() * policy.penaltyPerDownTicks());
        state.setPolicy(policy); state.setDowned(true); state.setMaxTicks(timer); state.setTicksRemaining(timer);
        state.setOriginalDeathMessage(source.getLocalizedDeathMessage(entity).getString());
        if (entity instanceof Mob mob) state.captureFlags(mob.isNoAi(), mob.canPickUpLoot(), entity.getPose().name());
        else state.captureFlags(false, false, entity.getPose().name());
        entity.setHealth(1.0F); entity.fallDistance = 0.0F; entity.stopUsingItem();
        if (entity.isPassenger()) entity.stopRiding();
        enforceManagedDowned(entity, state); ACTIVE.put(entity.getUUID(), entity); SecondWindNetworking.syncTrackedEntity(entity);
        announceEntityDowned(entity, policy);
        return true;
    }

    public static DamageResult handleIncomingDamage(LivingEntity entity, DamageSource source, float amount) {
        if (entity instanceof Player) return DamageResult.PASS;
        SecondWindEntityState state = getState(entity); ResolvedEntityPolicy policy = state.policy();
        if (!state.isDowned() || policy == null || policy.lifecycle() == EntityBehaviorDefinition.Lifecycle.Type.EXTERNAL) return DamageResult.PASS;
        boolean reduce = policy.damageMode() == EntityBehaviorDefinition.Downed.DamageMode.REDUCE_TIMER
                || policy.damageMode() == EntityBehaviorDefinition.Downed.DamageMode.NORMAL_AND_REDUCE_TIMER;
        if (reduce && amount > 0.0F) {
            long now = entity.level().getGameTime();
            if (policy.damageCooldownTicks() == 0 || state.lastDamageGameTime() == 0L || now - state.lastDamageGameTime() >= policy.damageCooldownTicks()) {
                state.setLastDamageGameTime(now); state.setTicksRemaining(state.ticksRemaining() - Math.max(1, Math.round(amount * 20.0F)));
                if (state.ticksRemaining() <= 0) { failAndKill(entity, source); return DamageResult.CANCEL; }
                SecondWindNetworking.syncTrackedEntity(entity);
            }
        }
        return policy.damageMode() == EntityBehaviorDefinition.Downed.DamageMode.IGNORE
                || policy.damageMode() == EntityBehaviorDefinition.Downed.DamageMode.REDUCE_TIMER ? DamageResult.CANCEL : DamageResult.PASS;
    }

    public static boolean shouldBlockHealing(LivingEntity entity) {
        if (entity instanceof Player) return false;
        SecondWindEntityState state = getState(entity);
        return state.isDowned() && state.policy() != null && state.policy().lifecycle() == EntityBehaviorDefinition.Lifecycle.Type.MANAGED
                && state.policy().blockHealing();
    }

    public static void tick(LivingEntity entity) {
        if (entity instanceof Player || entity.level().isClientSide()) return;
        SecondWindEntityState state = getState(entity);
        if (!state.isDowned()) {
            if (state.cooldownTicks() > 0) state.setCooldownTicks(state.cooldownTicks() - 1);
            if (state.cooldownTicks() <= 0) ACTIVE.remove(entity.getUUID());
            return;
        }
        ACTIVE.put(entity.getUUID(), entity); ResolvedEntityPolicy policy = state.policy();
        if (policy == null) { state.clearDownedRuntime(); ACTIVE.remove(entity.getUUID()); return; }
        if (policy.lifecycle() == EntityBehaviorDefinition.Lifecycle.Type.EXTERNAL) {
            ExternalDownedEntityAdapter adapter = SecondWindApi.externalAdapter(policy.adapter()).orElse(null);
            if (adapter == null || !adapter.isDowned(entity)) { clearExternalState(entity, state); return; }
        } else enforceManagedDowned(entity, state);
        if (tickReviveChannel(entity, state, policy)) return;
        if (policy.lifecycle() == EntityBehaviorDefinition.Lifecycle.Type.MANAGED) {
            state.setTicksRemaining(state.ticksRemaining() - 1);
            if (state.ticksRemaining() <= 0) failAndKill(entity, entity.damageSources().generic());
            else if (state.ticksRemaining() % 20 == 0 || state.ticksRemaining() <= 60) SecondWindNetworking.syncTrackedEntity(entity);
        }
    }

    public static boolean canPlayerRevive(ServerPlayer reviver, LivingEntity target) {
        if (target instanceof ServerPlayer player) return SecondWindService.canPlayerRevive(reviver, player);
        if (reviver == target || reviver.isCreative() || reviver.isSpectator() || SecondWindService.isDowned(reviver)) return false;
        SecondWindEntityState state = getState(target); ResolvedEntityPolicy policy = state.policy();
        if (!state.isDowned() || policy == null || !policy.reviveEnabled() || reviver.distanceToSqr(target) > policy.reviveDistance() * policy.reviveDistance()) return false;
        return policy.lifecycle() != EntityBehaviorDefinition.Lifecycle.Type.EXTERNAL
                || SecondWindApi.externalAdapter(policy.adapter()).map(adapter -> adapter.canRevive(reviver, target)).orElse(false);
    }

    public static boolean refreshReviveChannel(ServerPlayer reviver, LivingEntity target) {
        if (target instanceof ServerPlayer player) return SecondWindService.refreshReviveChannel(reviver, player);
        if (!canPlayerRevive(reviver, target)) return false;
        SecondWindEntityState state = getState(target); ResolvedEntityPolicy policy = state.policy(); long now = target.level().getGameTime();
        if (policy.reviveChannelTicks() <= 0) return completeRevive(reviver, target, state, policy);
        if (state.reviveChannelReviver().filter(reviver.getUUID()::equals).isEmpty()) {
            if (state.reviveChannelReviver().isPresent() && state.reviveChannelLastHoldGameTime() >= now - REVIVE_HOLD_GRACE_TICKS) return false;
            state.beginReviveChannel(reviver.getUUID(), now); SecondWindNetworking.syncTrackedEntity(target);
        } else state.refreshReviveChannel(now);
        return true;
    }

    public static void interruptReviveChannelsFor(ServerPlayer player) {
        for (LivingEntity target : ACTIVE.values()) {
            SecondWindEntityState state = getState(target);
            if (state.reviveChannelReviver().filter(player.getUUID()::equals).isPresent()) {
                state.clearReviveChannel(); SecondWindNetworking.syncTrackedEntity(target);
            }
        }
    }

    public static void notifyExternalStateChanged(LivingEntity entity) {
        if (entity instanceof Player || entity.level().isClientSide()) return;
        EntityBehaviorDefinition definition = EntityBehaviorManager.resolve(entity).orElse(null);
        if (definition == null || definition.lifecycle().type() != EntityBehaviorDefinition.Lifecycle.Type.EXTERNAL) return;
        ExternalDownedEntityAdapter adapter = SecondWindApi.externalAdapter(definition.lifecycle().adapter()).orElse(null);
        if (adapter == null) return;
        SecondWindEntityState state = getState(entity);
        if (adapter.isDowned(entity)) {
            boolean newlyDowned = !state.isDowned() || state.policy() == null
                    || state.policy().lifecycle() != EntityBehaviorDefinition.Lifecycle.Type.EXTERNAL;
            if (newlyDowned) {
                state.setPolicy(resolvePolicy(entity, definition)); state.setDowned(true); state.setTicksRemaining(0); state.setMaxTicks(0);
                announceEntityDowned(entity, state.policy());
            }
            ACTIVE.put(entity.getUUID(), entity);
        } else if (state.isDowned() && state.policy() != null && state.policy().lifecycle() == EntityBehaviorDefinition.Lifecycle.Type.EXTERNAL) {
            state.clearDownedRuntime(); ACTIVE.remove(entity.getUUID());
        }
        SecondWindNetworking.syncTrackedEntity(entity);
    }

    public static void onLoaded(LivingEntity entity) {
        if (entity instanceof Player || entity.level().isClientSide()) return;
        SecondWindEntityState state = getState(entity);
        if (state.isDowned() || state.cooldownTicks() > 0) ACTIVE.put(entity.getUUID(), entity);
        if (state.isDowned() && state.policy() != null && state.policy().lifecycle() == EntityBehaviorDefinition.Lifecycle.Type.MANAGED) enforceManagedDowned(entity, state);
        notifyExternalStateChanged(entity);
    }

    public static void onUnloaded(LivingEntity entity) { ACTIVE.remove(entity.getUUID()); }

    public static void tickActive() {
        for (LivingEntity entity : java.util.List.copyOf(ACTIVE.values())) {
            if (!entity.isRemoved()) tick(entity);
        }
    }

    private static boolean tickReviveChannel(LivingEntity target, SecondWindEntityState state, ResolvedEntityPolicy policy) {
        if (state.reviveChannelReviver().isEmpty()) return false;
        ServerPlayer reviver = target.getServer() == null ? null : target.getServer().getPlayerList().getPlayer(state.reviveChannelReviver().get());
        long now = target.level().getGameTime();
        if (reviver == null || !canPlayerRevive(reviver, target) || state.reviveChannelLastHoldGameTime() < now - REVIVE_HOLD_GRACE_TICKS) {
            state.clearReviveChannel(); SecondWindNetworking.syncTrackedEntity(target); return false;
        }
        state.advanceReviveChannel();
        if (state.reviveChannelTicks() >= policy.reviveChannelTicks()) { completeRevive(reviver, target, state, policy); return true; }
        if (state.reviveChannelTicks() % 5 == 0) SecondWindNetworking.syncTrackedEntity(target);
        return true;
    }

    private static boolean completeRevive(ServerPlayer reviver, LivingEntity target, SecondWindEntityState state, ResolvedEntityPolicy policy) {
        if (policy.lifecycle() == EntityBehaviorDefinition.Lifecycle.Type.EXTERNAL) {
            boolean recovered = SecondWindApi.externalAdapter(policy.adapter()).map(adapter -> adapter.revive(reviver, target)).orElse(false);
            if (!recovered) return false;
            clearExternalState(target, state);
        } else {
            restoreManagedState(target, state); state.clearDownedRuntime(); state.incrementDownCount(); state.setCooldownTicks(policy.cooldownTicks());
            target.setHealth(Math.min(target.getMaxHealth(), Math.max(1.0F, policy.reviveHealth())));
            if (policy.regenerationTicks() > 0) target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, policy.regenerationTicks(), 1));
            target.invulnerableTime = policy.invulnerabilityTicks();
            if (state.cooldownTicks() <= 0) ACTIVE.remove(target.getUUID());
            SecondWindNetworking.syncTrackedEntity(target);
        }
        reviver.displayClientMessage(Component.translatable("hud.secondwind.revived"), true); return true;
    }

    private static void clearExternalState(LivingEntity target, SecondWindEntityState state) {
        state.clearDownedRuntime(); ACTIVE.remove(target.getUUID()); SecondWindNetworking.syncTrackedEntity(target);
    }

    private static void failAndKill(LivingEntity entity, DamageSource source) {
        SecondWindEntityState state = getState(entity); ResolvedEntityPolicy policy = state.policy(); restoreManagedState(entity, state);
        state.clearDownedRuntime(); state.incrementDownCount(); state.setCooldownTicks(policy == null ? 0 : policy.cooldownTicks()); state.setForcedDeathFlow(true);
        ACTIVE.remove(entity.getUUID()); entity.setHealth(1.0F); entity.invulnerableTime = 0;
        if (!entity.hurt(source, Float.MAX_VALUE)) entity.kill();
        state.setForcedDeathFlow(false); SecondWindNetworking.syncTrackedEntity(entity);
    }

    private static void enforceManagedDowned(LivingEntity entity, SecondWindEntityState state) {
        ResolvedEntityPolicy policy = state.policy();
        if (policy != null && policy.blockHealing()) entity.setHealth(1.0F); else entity.setHealth(Math.max(1.0F, entity.getHealth()));
        entity.stopUsingItem(); entity.fallDistance = 0.0F;
        if (policy != null && SecondWindApi.usesVanillaSwimmingPose(policy.pose())) entity.setPose(Pose.SWIMMING);
        if (entity instanceof Mob mob && policy != null && policy.disableAi()) { mob.getNavigation().stop(); mob.setTarget(null); mob.setNoAi(true); mob.setCanPickUpLoot(false); }
    }

    private static void restoreManagedState(LivingEntity entity, SecondWindEntityState state) {
        if (!state.capturedFlags()) return;
        if (entity instanceof Mob mob) { mob.setNoAi(state.previousNoAi()); mob.setCanPickUpLoot(state.previousCanPickUpLoot()); mob.setTarget(null); }
        try { entity.setPose(Pose.valueOf(state.previousPose())); } catch (IllegalArgumentException ignored) { entity.setPose(Pose.STANDING); }
    }

    private static ResolvedEntityPolicy resolvePolicy(LivingEntity entity, EntityBehaviorDefinition definition) {
        EntityBehaviorDefinition.Downed downed = definition.downed(); EntityBehaviorDefinition.Revive revive = definition.revive();
        EntityBehaviorDefinition.Downed.DamageMode defaultDamage = SecondWindConfig.DOWNED_DAMAGE_REDUCES_TIMER.get()
                ? (SecondWindConfig.DOWNED_DAMAGE_REGISTERS.get() ? EntityBehaviorDefinition.Downed.DamageMode.NORMAL_AND_REDUCE_TIMER : EntityBehaviorDefinition.Downed.DamageMode.REDUCE_TIMER)
                : (SecondWindConfig.DOWNED_DAMAGE_REGISTERS.get() ? EntityBehaviorDefinition.Downed.DamageMode.NORMAL : EntityBehaviorDefinition.Downed.DamageMode.IGNORE);
        ResourceLocation pose = definition.selectPose(entity).orElse(ResourceLocation.fromNamespaceAndPath("secondwind", "sideways"));
        AnnouncementMessage message = definition.presentation().downedMessage();
        return new ResolvedEntityPolicy(definition.id(), definition.lifecycle().type(), definition.lifecycle().adapter(),
                value(downed.timerTicks(), SecondWindConfig.DOWNED_TIMER_SECONDS.get() * 20), value(downed.minimumTimerTicks(), SecondWindConfig.MINIMUM_DOWNED_TIMER_SECONDS.get() * 20),
                value(downed.penaltyPerDownTicks(), SecondWindConfig.TIMER_PENALTY_PER_DOWN.get() * 20), downed.damageMode() == null ? defaultDamage : downed.damageMode(),
                value(downed.damageCooldownTicks(), SecondWindConfig.DOWNED_DAMAGE_COOLDOWN_TICKS.get()), value(downed.disableAi(), true),
                value(downed.blockHealing(), SecondWindConfig.BLOCK_HEALING_WHILE_DOWNED.get()), value(revive.enabled(), SecondWindConfig.MULTIPLAYER_REVIVE.get()),
                value(revive.channelTicks(), (int) Math.ceil(SecondWindConfig.REVIVE_CHANNEL_SECONDS.get() * 20.0D)), value(revive.distance(), SecondWindConfig.REVIVE_DISTANCE.get()),
                value(revive.health(), SecondWindConfig.REVIVE_HEALTH_HALF_HEARTS.get().floatValue()), value(revive.regenerationTicks(), SecondWindConfig.REVIVE_REGENERATION_SECONDS.get() * 20),
                value(revive.invulnerabilityTicks(), SecondWindConfig.POST_REVIVE_INVULNERABILITY_SECONDS.get() * 20),
                value(revive.cooldownTicks(), SecondWindConfig.COOLDOWN_MODE.get() == CooldownMode.NONE ? 0 : SecondWindConfig.COOLDOWN_DURATION_SECONDS.get() * 20),
                definition.presentation().showTimer(), definition.presentation().announce(),
                message.translationKey(), message.fallback(), message.text(), pose);
    }

    private static void announceEntityDowned(LivingEntity entity, ResolvedEntityPolicy policy) {
        if (policy == null || !policy.announce() || !SecondWindConfig.ENABLE_CHAT_MESSAGES.get() || entity.getServer() == null) return;
        AnnouncementMessage definition = new AnnouncementMessage(
                policy.downedMessageTranslationKey(), policy.downedMessageFallback(), policy.downedMessageText());
        MutableComponent message = definition.render(entity.getDisplayName(), SecondWindConfig.LOCALIZE_CHAT_MESSAGES.get());
        entity.getServer().getPlayerList().broadcastSystemMessage(message.withStyle(ChatFormatting.RED), false);
    }

    private static int value(Integer value, int fallback) { return value == null ? fallback : value; }
    private static boolean value(Boolean value, boolean fallback) { return value == null ? fallback : value; }
    private static double value(Double value, double fallback) { return value == null ? fallback : value; }
    private static float value(Float value, float fallback) { return value == null ? fallback : value; }
    public enum DamageResult { PASS, CANCEL }
}
