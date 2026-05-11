package com.jvn.secondwind.util;

import com.jvn.secondwind.config.SecondWindConfig;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;

public final class SecondWindEntityRules {
    private SecondWindEntityRules() {
    }

    public static Optional<ServerPlayer> findCreditedPlayer(DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return Optional.of(player);
        }
        if (source.getDirectEntity() instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer player) {
            return Optional.of(player);
        }
        if (SecondWindConfig.ALLOW_PET_KILLS.get() && source.getEntity() instanceof OwnableEntity ownable
                && ownable.getOwner() instanceof ServerPlayer player) {
            return Optional.of(player);
        }
        if (SecondWindConfig.ALLOW_PET_KILLS.get() && source.getEntity() instanceof TamableAnimal animal
                && animal.getOwner() instanceof ServerPlayer player) {
            return Optional.of(player);
        }
        return Optional.empty();
    }

    public static boolean isValidReviveTarget(LivingEntity target, ServerPlayer downedPlayer) {
        if (target == downedPlayer || target instanceof ArmorStand || target instanceof Villager) {
            return false;
        }
        if (target instanceof Player) {
            return SecondWindConfig.ALLOW_PLAYER_KILLS.get();
        }
        if (target instanceof Enemy) {
            return true;
        }
        if (target instanceof NeutralMob && target instanceof Mob mob) {
            return mob.getTarget() == downedPlayer;
        }
        if (target instanceof Mob mob && mob.getTarget() == downedPlayer) {
            return true;
        }
        return SecondWindConfig.ALLOW_PASSIVE_KILLS.get();
    }

    public static boolean isServerPlayerEntity(Entity entity) {
        return entity instanceof ServerPlayer;
    }
}
