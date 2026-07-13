package com.jvn.secondwind.event;

import com.jvn.secondwind.SecondWindMod;
import com.jvn.secondwind.advancement.SecondWindCriteria;
import com.jvn.secondwind.config.SecondWindConfig;
import com.jvn.secondwind.network.SecondWindNetworking;
import com.jvn.secondwind.state.FailureReason;
import com.jvn.secondwind.state.ReviveReason;
import com.jvn.secondwind.state.SecondWindPlayerState;
import com.jvn.secondwind.state.SecondWindService;
import com.jvn.secondwind.state.SecondWindEntityService;
import com.jvn.secondwind.network.SecondWindNetworking;
import com.jvn.secondwind.util.SecondWindEntityRules;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

@EventBusSubscriber(modid = SecondWindMod.MOD_ID)
public final class SecondWindServerEvents {
    private SecondWindServerEvents() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            if (SecondWindEntityService.tryDownFromDeath(event.getEntity(), event.getSource())) {
                event.setCanceled(true);
                return;
            }
            handleKillToRevive(event);
            return;
        }

        SecondWindPlayerState state = SecondWindService.getState(player);
        if (state.isForcedDeathFlow() || state.isDowned()) {
            triggerFinishHim(event, player, state);
            handleKillToRevive(event);
            return;
        }

        if (!SecondWindService.canTriggerSecondWind(player, event.getSource())) {
            handleKillToRevive(event);
            return;
        }

        event.setCanceled(true);
        SecondWindService.down(player, event.getSource());
        handleDowningToRevive(player, event.getSource());
    }

    private static void handleKillToRevive(LivingDeathEvent event) {
        Optional<ServerPlayer> creditedPlayer = SecondWindEntityRules.findCreditedPlayer(event.getSource());
        if (creditedPlayer.isEmpty()) {
            return;
        }

        ServerPlayer player = creditedPlayer.get();
        if (!SecondWindService.isDowned(player) || !SecondWindEntityRules.isValidReviveTarget(event.getEntity(), player)) {
            return;
        }

        SecondWindService.revive(player, ReviveReason.KILL);
    }

    private static void handleDowningToRevive(ServerPlayer target, net.minecraft.world.damagesource.DamageSource damageSource) {
        Optional<ServerPlayer> creditedPlayer = SecondWindEntityRules.findCreditedPlayer(damageSource);
        if (creditedPlayer.isEmpty()) {
            return;
        }

        ServerPlayer player = creditedPlayer.get();
        if (!SecondWindService.isDowned(player) || !SecondWindEntityRules.isValidReviveTarget(target, player)) {
            return;
        }

        SecondWindService.revive(player, ReviveReason.KILL);
    }

    private static void triggerFinishHim(LivingDeathEvent event, ServerPlayer player, SecondWindPlayerState state) {
        if (!state.isDowned()) {
            return;
        }

        SecondWindEntityRules.findCreditedPlayer(event.getSource())
                .filter(attacker -> attacker != player)
                .ifPresent(SecondWindCriteria::triggerFinishHim);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SecondWindService.handleUnsafeExitIfNeeded(player);
            SecondWindNetworking.syncToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && SecondWindService.isDowned(player)) {
            SecondWindService.markUnsafeExitWhileDowned(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!event.isEndConquered() && SecondWindConfig.RESET_COOLDOWN_ON_DEATH.get()) {
                SecondWindService.resetCooldownAfterDeath(player);
            }
            SecondWindService.getState(player).setForcedDeathFlow(false);
            SecondWindNetworking.syncToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SecondWindService.enforceDownedMovement(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SecondWindService.tickDowned(player);
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity living && !(living instanceof ServerPlayer)) {
            SecondWindEntityService.tick(living);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer reviver)
                || !(event.getTarget() instanceof LivingEntity downedEntity)
                || event.getLevel().isClientSide()) {
            return;
        }

        if (SecondWindEntityService.canPlayerRevive(reviver, downedEntity)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            if (SecondWindEntityService.handleIncomingDamage(event.getEntity(), event.getSource(), event.getAmount())
                    == SecondWindEntityService.DamageResult.CANCEL) {
                event.setCanceled(true);
            }
            return;
        }

        if (SecondWindService.isDowned(player)) {
            SecondWindService.DownedDamageTimerResult timerResult = SecondWindService.DownedDamageTimerResult.NONE;
            if (SecondWindConfig.DOWNED_DAMAGE_REDUCES_TIMER.get()) {
                timerResult = SecondWindService.applyDownedDamageToTimer(player, event.getSource(), event.getAmount());
            }

            boolean damageRegisters = SecondWindConfig.DOWNED_DAMAGE_REGISTERS.get();
            if (timerResult.timerExpired() || !damageRegisters) {
                if (timerResult.timerReduced() && !timerResult.timerExpired() && !damageRegisters) {
                    SecondWindService.applyCanceledDownedDamageFeedback(player, event.getSource());
                }
                event.setCanceled(true);
            }
        }

        if (SecondWindConfig.REVIVE_INTERRUPT_ON_DAMAGE.get()) {
            SecondWindService.interruptReviveChannelsFor(player);
            SecondWindEntityService.interruptReviveChannelsFor(player);
        }
    }

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (SecondWindConfig.BLOCK_HEALING_WHILE_DOWNED.get()
                && event.getEntity() instanceof ServerPlayer player
                && SecondWindService.isDowned(player)) {
            event.setCanceled(true);
        } else if (SecondWindEntityService.shouldBlockHealing(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingEntityUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (!SecondWindConfig.BLOCK_EATING_WHILE_DOWNED.get()
                || !(event.getEntity() instanceof ServerPlayer player)
                || !SecondWindService.isDowned(player)) {
            return;
        }

        if (event.getItem().getFoodProperties(player) != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.wakeImmediately()) {
            SecondWindService.resetCooldownForSleep(player);
            SecondWindNetworking.syncToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity living && !(living instanceof ServerPlayer) && !event.getLevel().isClientSide()) {
            SecondWindEntityService.onLoaded(living);
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity living && !(living instanceof ServerPlayer) && !event.getLevel().isClientSide()) {
            SecondWindEntityService.onUnloaded(living);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof LivingEntity living
                && !(living instanceof ServerPlayer)) {
            SecondWindEntityService.notifyExternalStateChanged(living);
            SecondWindNetworking.sendTrackedEntity(player, living);
        }
    }
}
