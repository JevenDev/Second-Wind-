package com.jvn.secondwind.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SecondWindConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue DOWNED_TIMER_SECONDS;
    public static final ModConfigSpec.IntValue MINIMUM_DOWNED_TIMER_SECONDS;
    public static final ModConfigSpec.IntValue TIMER_PENALTY_PER_DOWN;
    public static final ModConfigSpec.IntValue DOWNED_SLOWNESS_LEVEL;
    public static final ModConfigSpec.BooleanValue DOWNED_DAMAGE_REDUCES_TIMER;
    public static final ModConfigSpec.BooleanValue DOWNED_DAMAGE_REGISTERS;
    public static final ModConfigSpec.BooleanValue BLOCK_HEALING_WHILE_DOWNED;
    public static final ModConfigSpec.BooleanValue BLOCK_EATING_WHILE_DOWNED;
    public static final ModConfigSpec.IntValue REVIVE_HEALTH_HALF_HEARTS;
    public static final ModConfigSpec.IntValue REVIVE_REGENERATION_SECONDS;
    public static final ModConfigSpec.IntValue POST_REVIVE_INVULNERABILITY_SECONDS;
    public static final ModConfigSpec.EnumValue<CooldownMode> COOLDOWN_MODE;
    public static final ModConfigSpec.IntValue COOLDOWN_DURATION_SECONDS;
    public static final ModConfigSpec.BooleanValue RESET_COOLDOWN_ON_DEATH;
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
    public static final ModConfigSpec.BooleanValue ENABLE_DOWNED_BLOOM;
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
        DOWNED_SLOWNESS_LEVEL = BUILDER
                .comment("Vanilla Slowness level applied while downed. 0 disables it, 3 is Slowness III.")
                .defineInRange("downedSlownessLevel", 3, 0, 6);
        DOWNED_DAMAGE_REDUCES_TIMER = BUILDER
                .comment("When a downed player is hit, reduce their remaining downed timer based on the incoming damage amount.")
                .define("downedDamageReducesTimer", true);
        DOWNED_DAMAGE_REGISTERS = BUILDER
                .comment("Allow hits against downed players to continue applying normal Minecraft damage in addition to any timer reduction.")
                .define("downedDamageRegisters", false);
        BLOCK_HEALING_WHILE_DOWNED = BUILDER
                .comment("Prevent healing from restoring health while the player is downed.")
                .define("blockHealingWhileDowned", true);
        BLOCK_EATING_WHILE_DOWNED = BUILDER
                .comment("Prevent downed players from eating food items until they are revived or die.")
                .define("blockEatingWhileDowned", true);
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
        RESET_COOLDOWN_ON_DEATH = BUILDER
                .comment("Reset Second Wind cooldown and repeated-down penalty when the player dies and respawns.")
                .define("resetCooldownOnDeath", true);

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
                .comment("Show a desaturated red screen tint while downed when supported.")
                .define("enableDesaturation", true);
        ENABLE_DOWNED_BLOOM = BUILDER
                .comment("Apply a soft bloom-style blur while downed when supported.")
                .define("enableDownedBloom", true);
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
