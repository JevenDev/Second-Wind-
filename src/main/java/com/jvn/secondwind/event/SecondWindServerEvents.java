package com.jvn.secondwind.event;

import com.jvn.secondwind.SecondWindMod;
import com.jvn.secondwind.config.SecondWindConfig;
import com.jvn.secondwind.network.SecondWindNetworking;
import com.jvn.secondwind.state.FailureReason;
import com.jvn.secondwind.state.ReviveReason;
import com.jvn.secondwind.state.SecondWindPlayerState;
import com.jvn.secondwind.state.SecondWindService;
import com.jvn.secondwind.util.SecondWindEntityRules;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
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

@EventBusSubscriber(modid = SecondWindMod.MOD_ID)
public final class SecondWindServerEvents {
    private static final float DOWNED_SAFE_HEALTH = 1.0F;

    private SecondWindServerEvents() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            handleKillToRevive(event);
            return;
        }

        SecondWindPlayerState state = SecondWindService.getState(player);
        if (state.isForcedDeathFlow() || state.isDowned()) {
            handleKillToRevive(event);
            return;
        }

        if (!SecondWindService.canTriggerSecondWind(player, event.getSource())) {
            handleKillToRevive(event);
            return;
        }

        event.setCanceled(true);
        SecondWindService.enterDowned(player, event.getSource());
        player.setHealth(DOWNED_SAFE_HEALTH);
        player.fallDistance = 0.0F;
        player.setDeltaMovement(player.getDeltaMovement().multiply(0.15D, 0.0D, 0.15D));

        if (SecondWindConfig.ENABLE_SOUNDS.get()) {
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.8F, 0.6F);
        }

        SecondWindNetworking.syncToPlayer(player);
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
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer reviver)
                || !(event.getTarget() instanceof ServerPlayer downedPlayer)
                || event.getLevel().isClientSide()) {
            return;
        }

        if (SecondWindService.canPlayerRevive(reviver, downedPlayer)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (SecondWindService.isDowned(player)) {
            boolean timerExpired = false;
            if (SecondWindConfig.DOWNED_DAMAGE_REDUCES_TIMER.get()) {
                timerExpired = SecondWindService.applyDownedDamageToTimer(player, event.getAmount());
            }

            if (timerExpired || !SecondWindConfig.DOWNED_DAMAGE_REGISTERS.get()) {
                event.setCanceled(true);
            }
        }

        if (SecondWindConfig.REVIVE_INTERRUPT_ON_DAMAGE.get()) {
            SecondWindService.interruptReviveChannelsFor(player);
        }
    }

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (SecondWindConfig.BLOCK_HEALING_WHILE_DOWNED.get()
                && event.getEntity() instanceof ServerPlayer player
                && SecondWindService.isDowned(player)) {
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
}
