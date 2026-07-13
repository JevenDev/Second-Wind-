package com.jvn.secondwind.config;

import com.jvn.secondwind.SecondWindMod;
import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Modmenu;
import io.wispforest.owo.config.annotation.Nest;
import io.wispforest.owo.config.annotation.RangeConstraint;
import io.wispforest.owo.config.annotation.SectionHeader;

@Modmenu(modId = SecondWindMod.MOD_ID)
@Config(name = SecondWindMod.MOD_ID, wrapperName = "SecondWindOwoConfig")
public final class SecondWindConfigModel {
    @Nest
    @SectionHeader("secondWind")
    public SecondWind secondWind = new SecondWind();

    @Nest
    @SectionHeader("multiplayerRevive")
    public MultiplayerRevive multiplayerRevive = new MultiplayerRevive();

    @Nest
    @SectionHeader("killRules")
    public KillRules killRules = new KillRules();

    @Nest
    @SectionHeader("clientFeedback")
    public ClientFeedback clientFeedback = new ClientFeedback();

    public static final class SecondWind {
        @RangeConstraint(min = 3, max = 60)
        public int downedTimerSeconds = 12;

        @RangeConstraint(min = 1, max = 30)
        public int minimumDownedTimerSeconds = 3;

        @RangeConstraint(min = 0, max = 10)
        public int timerPenaltyPerDown = 2;

        @RangeConstraint(min = 0, max = 6)
        public int downedSlownessLevel = 3;

        public boolean forceCrawlingPose = true;

        public boolean downedDamageReducesTimer = true;

        @RangeConstraint(min = 0, max = 100)
        public int downedDamageCooldownTicks = 30;

        public boolean downedDamageRegisters = false;

        public boolean downedDamagePlaysHitSound = true;

        public boolean downedDamageAppliesKnockback = true;

        public boolean blockHealingWhileDowned = true;

        public boolean blockEatingWhileDowned = true;

        @RangeConstraint(min = 1, max = 40)
        public int reviveHealthHalfHearts = 12;

        @RangeConstraint(min = 0, max = 30)
        public int reviveRegenerationSeconds = 3;

        @RangeConstraint(min = 0, max = 10)
        public int postReviveInvulnerabilitySeconds = 2;

        public CooldownMode cooldownMode = CooldownMode.TIMED;

        @RangeConstraint(min = 0, max = 86400)
        public int cooldownDurationSeconds = 300;

        public boolean resetCooldownOnDeath = true;
    }

    public static final class MultiplayerRevive {
        public boolean multiplayerRevive = true;

        @RangeConstraint(min = 0.0D, max = 10.0D, decimalPlaces = 1)
        public double reviveChannelSeconds = 2.0D;

        @RangeConstraint(min = 1.0D, max = 8.0D, decimalPlaces = 1)
        public double reviveDistance = 2.5D;

        public boolean reviveInterruptOnDamage = true;
    }

    public static final class KillRules {
        public boolean allowPassiveKills = false;

        public boolean allowPlayerKills = true;

        public boolean allowPetKills = false;

        public boolean allowVoidSecondWind = false;
    }

    public static final class ClientFeedback {
        public boolean enableDownedVignette = true;

        public boolean enableDesaturation = true;

        public boolean enableDownedBloom = true;

        public boolean enableSounds = true;

        public boolean enableChatMessages = true;

        public boolean localizeChatMessages = true;

        public boolean enableSecondWindPopup = true;

        public boolean useSimpleDownedTimer = false;
    }
}
