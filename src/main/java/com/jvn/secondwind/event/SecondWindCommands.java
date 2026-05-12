package com.jvn.secondwind.event;

import com.jvn.secondwind.SecondWindMod;
import com.jvn.secondwind.state.ReviveReason;
import com.jvn.secondwind.state.SecondWindService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = SecondWindMod.MOD_ID)
public final class SecondWindCommands {
    private static final int ADMIN_PERMISSION_LEVEL = 2;

    private SecondWindCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("secondwind")
                .requires(source -> source.hasPermission(ADMIN_PERMISSION_LEVEL))
                .then(buildReviveCommand("revive"))
                .then(buildDownCommand("down")));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildReviveCommand(String name) {
        return Commands.literal(name)
                .executes(context -> reviveAll(context.getSource()))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> revivePlayer(
                                context.getSource(),
                                EntityArgument.getPlayer(context, "player"))));
    }

                private static LiteralArgumentBuilder<CommandSourceStack> buildDownCommand(String name) {
                return Commands.literal(name)
                    .executes(context -> downAll(context.getSource()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> downPlayer(
                            context.getSource(),
                            EntityArgument.getPlayer(context, "player"))));
                }

    private static int reviveAll(CommandSourceStack source) {
        List<ServerPlayer> downedPlayers = source.getServer().getPlayerList().getPlayers().stream()
                .filter(SecondWindService::isDowned)
                .toList();
        if (downedPlayers.isEmpty()) {
            source.sendFailure(Component.translatable("commands.secondwind.revive.all.none"));
            return 0;
        }

        downedPlayers.forEach(player -> SecondWindService.revive(player, ReviveReason.ADMIN));
        source.sendSuccess(
                () -> Component.translatable("commands.secondwind.revive.all.success", downedPlayers.size()),
                true);
        return downedPlayers.size();
    }

    private static int revivePlayer(CommandSourceStack source, ServerPlayer player) {
        if (!SecondWindService.isDowned(player)) {
            source.sendFailure(Component.translatable("commands.secondwind.revive.single.not_downed", player.getDisplayName()));
            return 0;
        }

        SecondWindService.revive(player, ReviveReason.ADMIN);
        source.sendSuccess(
                () -> Component.translatable("commands.secondwind.revive.single.success", player.getDisplayName()),
                true);
        return 1;
    }

    private static int downAll(CommandSourceStack source) {
        List<ServerPlayer> availablePlayers = source.getServer().getPlayerList().getPlayers().stream()
                .filter(SecondWindService::canEnterDownedState)
                .toList();
        if (availablePlayers.isEmpty()) {
            source.sendFailure(Component.translatable("commands.secondwind.down.all.none"));
            return 0;
        }

        availablePlayers.forEach(player -> SecondWindService.down(player, player.damageSources().generic()));
        source.sendSuccess(
                () -> Component.translatable("commands.secondwind.down.all.success", availablePlayers.size()),
                true);
        return availablePlayers.size();
    }

    private static int downPlayer(CommandSourceStack source, ServerPlayer player) {
        if (SecondWindService.isDowned(player)) {
            source.sendFailure(Component.translatable("commands.secondwind.down.single.already_downed", player.getDisplayName()));
            return 0;
        }

        if (player.isCreative() || player.isSpectator()) {
            source.sendFailure(Component.translatable("commands.secondwind.down.single.invalid_target", player.getDisplayName()));
            return 0;
        }

        SecondWindService.down(player, player.damageSources().generic());
        source.sendSuccess(
                () -> Component.translatable("commands.secondwind.down.single.success", player.getDisplayName()),
                true);
        return 1;
    }
}