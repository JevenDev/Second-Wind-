package com.jvn.secondwind.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SecondWindConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue DOWNED_TIMER_SECONDS;
    public static final ModConfigSpec.IntValue MINIMUM_DOWNED_TIMER_SECONDS;
    public static final ModConfigSpec.IntValue TIMER_PENALTY_PER_DOWN;
    public static final ModConfigSpec.DoubleValue DOWNED_MOVEMENT_MULTIPLIER;
    public static final ModConfigSpec.IntValue REVIVE_HEALTH_HALF_HEARTS;
    public static final ModConfigSpec.IntValue REVIVE_REGENERATION_SECONDS;
    public static final ModConfigSpec.IntValue POST_REVIVE_INVULNERABILITY_SECONDS;
    public static final ModConfigSpec.EnumValue<CooldownMode> COOLDOWN_MODE;
    public static final ModConfigSpec.IntValue COOLDOWN_DURATION_SECONDS;
    public static final ModConfigSpec.BooleanValue MULTIPLAYER_REVIVE;
    public static final ModConfigSpec.DoubleValue REVIVE_CHANNEL_SECONDS;
    public static final ModConfigSpec.DoubleValue REVIVE_DISTANCE;
    public static final ModConfigSpec.BooleanValue REVIVE_INTERRUPT_ON_DAMAGE;
    public static final ModConfigSpec.BooleanValue ALLOW_PASSIVE_KILLS;
    public static final ModConfigSpec.BooleanValue ALLOW_PLAYER_KILLS;
    public static final ModConfigSpec.BooleanValue ALLOW_PET_KILLS;
    public static final ModConfigSpec.BooleanValue ALLOW_VOID_SECOND_WIND;
    public static final ModConfigSpec.BooleanValue ENABLE_DOWNED_VIGNETTE;
    public static final ModConfigSpec.BooleanValue ENABLE_DESATURATION;
    public static final ModConfigSpec.BooleanValue ENABLE_SOUNDS;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("secondWind");

        DOWNED_TIMER_SECONDS = BUILDER
                .comment("Base number of seconds a player remains downed before dying.")
                .defineInRange("downedTimerSeconds", 10, 3, 60);
        MINIMUM_DOWNED_TIMER_SECONDS = BUILDER
                .comment("Minimum downed timer after repeated-down penalties are applied.")
                .defineInRange("minimumDownedTimerSeconds", 3, 1, 30);
        TIMER_PENALTY_PER_DOWN = BUILDER
                .comment("Seconds removed from the downed timer for each current penalty count.")
                .defineInRange("timerPenaltyPerDown", 2, 0, 10);
        DOWNED_MOVEMENT_MULTIPLIER = BUILDER
                .comment("Movement multiplier while downed. 0 prevents horizontal movement, 1 is normal speed.")
                .defineInRange("downedMovementMultiplier", 0.25D, 0.0D, 1.0D);
        REVIVE_HEALTH_HALF_HEARTS = BUILDER
                .comment("Health restored on revive, measured in half-hearts.")
                .defineInRange("reviveHealthHalfHearts", 12, 1, 40);
        REVIVE_REGENERATION_SECONDS = BUILDER
                .comment("Seconds of Regeneration II granted after revive.")
                .defineInRange("reviveRegenerationSeconds", 3, 0, 30);
        POST_REVIVE_INVULNERABILITY_SECONDS = BUILDER
                .comment("Seconds of brief invulnerability granted after revive.")
                .defineInRange("postReviveInvulnerabilitySeconds", 2, 0, 10);

        COOLDOWN_MODE = BUILDER
                .comment("How Second Wind cooldown is tracked.")
                .defineEnum("cooldownMode", CooldownMode.TIMED);
        COOLDOWN_DURATION_SECONDS = BUILDER
                .comment("Timed cooldown duration in seconds when cooldownMode is TIMED.")
                .defineInRange("cooldownDurationSeconds", 300, 0, 86400);

        BUILDER.push("multiplayerRevive");
        MULTIPLAYER_REVIVE = BUILDER
                .comment("Allow another player to revive a downed player.")
                .define("multiplayerRevive", true);
        REVIVE_CHANNEL_SECONDS = BUILDER
                .comment("Seconds another player must channel interaction to revive a downed player.")
                .defineInRange("reviveChannelSeconds", 2.0D, 0.0D, 10.0D);
        REVIVE_DISTANCE = BUILDER
                .comment("Maximum distance between reviver and downed player during revive channel.")
                .defineInRange("reviveDistance", 2.5D, 1.0D, 8.0D);
        REVIVE_INTERRUPT_ON_DAMAGE = BUILDER
                .comment("Interrupt multiplayer revive channels when the reviver or downed player takes damage.")
                .define("reviveInterruptOnDamage", true);
        BUILDER.pop();

        BUILDER.push("killRules");
        ALLOW_PASSIVE_KILLS = BUILDER
                .comment("Allow passive mob kills to revive a downed player.")
                .define("allowPassiveKills", false);
        ALLOW_PLAYER_KILLS = BUILDER
                .comment("Allow player kills to revive a downed player.")
                .define("allowPlayerKills", true);
        ALLOW_PET_KILLS = BUILDER
                .comment("Allow owned pet kills to count for their downed owner.")
                .define("allowPetKills", false);
        ALLOW_VOID_SECOND_WIND = BUILDER
                .comment("Allow Second Wind to trigger from void or out-of-world damage.")
                .define("allowVoidSecondWind", false);
        BUILDER.pop();

        BUILDER.push("clientFeedback");
        ENABLE_DOWNED_VIGNETTE = BUILDER
                .comment("Show a dark vignette while downed.")
                .define("enableDownedVignette", true);
        ENABLE_DESATURATION = BUILDER
                .comment("Show desaturation-style feedback while downed when supported.")
                .define("enableDesaturation", true);
        ENABLE_SOUNDS = BUILDER
                .comment("Play Second Wind sounds when available.")
                .define("enableSounds", true);
        BUILDER.pop();

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private SecondWindConfig() {
    }
}
