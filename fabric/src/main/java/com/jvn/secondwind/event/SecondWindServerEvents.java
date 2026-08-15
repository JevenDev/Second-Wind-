package com.jvn.secondwind.event;

import com.jvn.secondwind.advancement.SecondWindCriteria;
import com.jvn.secondwind.config.SecondWindConfig;
import com.jvn.secondwind.network.SecondWindNetworking;
import com.jvn.secondwind.state.FailureReason;
import com.jvn.secondwind.state.ReviveReason;
import com.jvn.secondwind.state.SecondWindPlayerState;
import com.jvn.secondwind.state.SecondWindService;
import com.jvn.secondwind.state.SecondWindEntityService;
import com.jvn.secondwind.util.SecondWindEntityRules;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class SecondWindServerEvents {
    private static final Map<UUID, Long> SLEEP_START_DAYS = new ConcurrentHashMap<>();

    private SecondWindServerEvents() {
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DEATH.register(SecondWindServerEvents::allowDeath);
        ServerLivingEntityEvents.AFTER_DEATH.register(SecondWindServerEvents::afterDeath);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(SecondWindServerEvents::allowDamage);
        ServerPlayerEvents.JOIN.register(player -> {
            SecondWindService.handleUnsafeExitIfNeeded(player);
            SecondWindNetworking.syncToPlayer(player);
        });
        ServerPlayerEvents.LEAVE.register(player -> {
            SLEEP_START_DAYS.remove(player.getUUID());
            if (SecondWindService.isDowned(player)) {
                SecondWindService.markUnsafeExitWhileDowned(player);
            }
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive && SecondWindConfig.RESET_COOLDOWN_ON_DEATH.get()) {
                SecondWindService.resetCooldownAfterDeath(newPlayer);
            }
            SecondWindService.getState(newPlayer).setForcedDeathFlow(false);
            SecondWindNetworking.syncToPlayer(newPlayer);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(player -> {
            SecondWindService.enforceDownedMovement(player);
            SecondWindService.tickDowned(player);
        }));
        ServerTickEvents.END_SERVER_TICK.register(server -> SecondWindEntityService.tickActive());
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof LivingEntity living && !(living instanceof ServerPlayer)) SecondWindEntityService.onLoaded(living);
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof LivingEntity living && !(living instanceof ServerPlayer)) SecondWindEntityService.onUnloaded(living);
        });
        EntityTrackingEvents.START_TRACKING.register((entity, player) -> {
            if (entity instanceof LivingEntity living && !(living instanceof ServerPlayer)) {
                SecondWindEntityService.notifyExternalStateChanged(living);
                SecondWindNetworking.sendTrackedEntity(player, living);
            }
        });
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (!level.isClientSide()
                    && player instanceof ServerPlayer reviver
                    && entity instanceof LivingEntity downedEntity
                    && SecondWindEntityService.canPlayerRevive(reviver, downedEntity)) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
        UseItemCallback.EVENT.register(SecondWindServerEvents::useItem);
        EntitySleepEvents.START_SLEEPING.register((entity, sleepingPos) -> {
            if (entity instanceof ServerPlayer player) {
                SLEEP_START_DAYS.put(player.getUUID(), currentMcDay(player));
            }
        });
        EntitySleepEvents.STOP_SLEEPING.register((entity, sleepingPos) -> {
            if (entity instanceof ServerPlayer player) {
                Long startDay = SLEEP_START_DAYS.remove(player.getUUID());
                if (startDay != null && currentMcDay(player) > startDay) {
                    SecondWindService.resetCooldownForSleep(player);
                    SecondWindNetworking.syncToPlayer(player);
                }
            }
        });
    }

    private static long currentMcDay(ServerPlayer player) {
        return player.serverLevel().getDayTime() / SecondWindService.TICKS_PER_MC_DAY;
    }

    private static InteractionResultHolder<ItemStack> useItem(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level level, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()
                && SecondWindConfig.BLOCK_EATING_WHILE_DOWNED.get()
                && player instanceof ServerPlayer serverPlayer
                && SecondWindService.isDowned(serverPlayer)
                && stack.get(DataComponents.FOOD) != null) {
            return InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    private static boolean allowDeath(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof ServerPlayer player)) {
            return !SecondWindEntityService.tryDownFromDeath(entity, source);
        }

        SecondWindPlayerState state = SecondWindService.getState(player);
        if (state.isForcedDeathFlow() || state.isDowned()) {
            triggerFinishHim(source, player, state);
            return true;
        }

        if (!SecondWindService.canTriggerSecondWind(player, source)) {
            return true;
        }

        SecondWindService.down(player, source);
        handleDowningToRevive(player, source);
        return false;
    }

    private static void afterDeath(LivingEntity entity, DamageSource source) {
        if (entity instanceof ServerPlayer player && SecondWindService.isDowned(player)) {
            SecondWindService.failDowned(player, FailureReason.INVALID_STATE);
        }

        handleKillToRevive(entity, source);
    }

    private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof ServerPlayer player)) {
            return SecondWindEntityService.handleIncomingDamage(entity, source, amount) != SecondWindEntityService.DamageResult.CANCEL;
        }

        if (SecondWindService.isDowned(player)) {
            SecondWindService.DownedDamageTimerResult timerResult = SecondWindService.DownedDamageTimerResult.NONE;
            if (SecondWindConfig.DOWNED_DAMAGE_REDUCES_TIMER.get()) {
                timerResult = SecondWindService.applyDownedDamageToTimer(player, source, amount);
            }

            boolean damageRegisters = SecondWindConfig.DOWNED_DAMAGE_REGISTERS.get();
            if (timerResult.timerExpired() || !damageRegisters) {
                if (timerResult.timerReduced() && !timerResult.timerExpired() && !damageRegisters) {
                    SecondWindService.applyCanceledDownedDamageFeedback(player, source);
                }
                return false;
            }
        }

        if (SecondWindConfig.REVIVE_INTERRUPT_ON_DAMAGE.get()) {
            SecondWindService.interruptReviveChannelsFor(player);
            SecondWindEntityService.interruptReviveChannelsFor(player);
        }
        return true;
    }

    private static void handleKillToRevive(LivingEntity target, DamageSource source) {
        Optional<ServerPlayer> creditedPlayer = SecondWindEntityRules.findCreditedPlayer(source);
        if (creditedPlayer.isEmpty()) {
            return;
        }

        ServerPlayer player = creditedPlayer.get();
        if (!SecondWindService.isDowned(player) || !SecondWindEntityRules.isValidReviveTarget(target, player)) {
            return;
        }

        SecondWindService.revive(player, ReviveReason.KILL);
    }

    private static void handleDowningToRevive(ServerPlayer target, DamageSource source) {
        Optional<ServerPlayer> creditedPlayer = SecondWindEntityRules.findCreditedPlayer(source);
        if (creditedPlayer.isEmpty()) {
            return;
        }

        ServerPlayer player = creditedPlayer.get();
        if (!SecondWindService.isDowned(player) || !SecondWindEntityRules.isValidReviveTarget(target, player)) {
            return;
        }

        SecondWindService.revive(player, ReviveReason.KILL);
    }

    private static void triggerFinishHim(DamageSource source, ServerPlayer player, SecondWindPlayerState state) {
        if (!state.isDowned()) {
            return;
        }

        SecondWindEntityRules.findCreditedPlayer(source)
                .filter(attacker -> attacker != player)
                .ifPresent(SecondWindCriteria::triggerFinishHim);
    }
}
