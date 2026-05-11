package com.jvn.secondwind.event;

import com.jvn.secondwind.SecondWindMod;
import com.jvn.secondwind.config.SecondWindConfig;
import com.jvn.secondwind.network.SecondWindNetworking;
import com.jvn.secondwind.state.FailureReason;
import com.jvn.secondwind.state.SecondWindPlayerState;
import com.jvn.secondwind.state.SecondWindService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = SecondWindMod.MOD_ID)
public final class SecondWindServerEvents {
    private static final float DOWNED_SAFE_HEALTH = 1.0F;

    private SecondWindServerEvents() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        SecondWindPlayerState state = SecondWindService.getState(player);
        if (state.isForcedDeathFlow() || state.isDowned()) {
            return;
        }

        if (!SecondWindService.canTriggerSecondWind(player, event.getSource())) {
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
            SecondWindService.failDowned(player, FailureReason.LOGOUT_WHILE_DOWNED);
            SecondWindService.getState(player).setForcedDeathFlow(false);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SecondWindService.getState(player).setForcedDeathFlow(false);
            SecondWindNetworking.syncToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SecondWindService.tickDowned(player);
        }
    }
}
