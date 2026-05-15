package com.jvn.secondwind.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jvn.secondwind.SecondWindMod;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class SecondWindConfig {
    public static final IntValue DOWNED_TIMER_SECONDS = new IntValue(12);
    public static final IntValue MINIMUM_DOWNED_TIMER_SECONDS = new IntValue(3);
    public static final IntValue TIMER_PENALTY_PER_DOWN = new IntValue(2);
    public static final IntValue DOWNED_SLOWNESS_LEVEL = new IntValue(3);
    public static final BooleanValue DOWNED_DAMAGE_REDUCES_TIMER = new BooleanValue(true);
    public static final IntValue DOWNED_DAMAGE_COOLDOWN_TICKS = new IntValue(30);
    public static final BooleanValue DOWNED_DAMAGE_REGISTERS = new BooleanValue(false);
    public static final BooleanValue BLOCK_HEALING_WHILE_DOWNED = new BooleanValue(true);
    public static final BooleanValue BLOCK_EATING_WHILE_DOWNED = new BooleanValue(true);
    public static final IntValue REVIVE_HEALTH_HALF_HEARTS = new IntValue(12);
    public static final IntValue REVIVE_REGENERATION_SECONDS = new IntValue(3);
    public static final IntValue POST_REVIVE_INVULNERABILITY_SECONDS = new IntValue(2);
    public static final EnumValue<CooldownMode> COOLDOWN_MODE = new EnumValue<>(CooldownMode.TIMED);
    public static final IntValue COOLDOWN_DURATION_SECONDS = new IntValue(300);
    public static final BooleanValue RESET_COOLDOWN_ON_DEATH = new BooleanValue(true);
    public static final BooleanValue MULTIPLAYER_REVIVE = new BooleanValue(true);
    public static final DoubleValue REVIVE_CHANNEL_SECONDS = new DoubleValue(2.0D);
    public static final DoubleValue REVIVE_DISTANCE = new DoubleValue(2.5D);
    public static final BooleanValue REVIVE_INTERRUPT_ON_DAMAGE = new BooleanValue(true);
    public static final BooleanValue ALLOW_PASSIVE_KILLS = new BooleanValue(false);
    public static final BooleanValue ALLOW_PLAYER_KILLS = new BooleanValue(true);
    public static final BooleanValue ALLOW_PET_KILLS = new BooleanValue(false);
    public static final BooleanValue ALLOW_VOID_SECOND_WIND = new BooleanValue(false);
    public static final BooleanValue ENABLE_DOWNED_VIGNETTE = new BooleanValue(true);
    public static final BooleanValue ENABLE_DESATURATION = new BooleanValue(true);
    public static final BooleanValue ENABLE_DOWNED_BLOOM = new BooleanValue(true);
    public static final BooleanValue ENABLE_SOUNDS = new BooleanValue(true);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private SecondWindConfig() {
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("secondwind.json");
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                apply(root);
            } catch (Exception exception) {
                SecondWindMod.LOGGER.warn("Failed to read Fabric config at {}; using defaults.", path, exception);
            }
        }

        try {
            write(path);
        } catch (IOException exception) {
            SecondWindMod.LOGGER.warn("Failed to write Fabric config defaults to {}.", path, exception);
        }
    }

    private static void apply(JsonObject root) {
        DOWNED_TIMER_SECONDS.read(root, "downedTimerSeconds");
        MINIMUM_DOWNED_TIMER_SECONDS.read(root, "minimumDownedTimerSeconds");
        TIMER_PENALTY_PER_DOWN.read(root, "timerPenaltyPerDown");
        DOWNED_SLOWNESS_LEVEL.read(root, "downedSlownessLevel");
        DOWNED_DAMAGE_REDUCES_TIMER.read(root, "downedDamageReducesTimer");
        DOWNED_DAMAGE_COOLDOWN_TICKS.read(root, "downedDamageCooldownTicks");
        DOWNED_DAMAGE_REGISTERS.read(root, "downedDamageRegisters");
        BLOCK_HEALING_WHILE_DOWNED.read(root, "blockHealingWhileDowned");
        BLOCK_EATING_WHILE_DOWNED.read(root, "blockEatingWhileDowned");
        REVIVE_HEALTH_HALF_HEARTS.read(root, "reviveHealthHalfHearts");
        REVIVE_REGENERATION_SECONDS.read(root, "reviveRegenerationSeconds");
        POST_REVIVE_INVULNERABILITY_SECONDS.read(root, "postReviveInvulnerabilitySeconds");
        COOLDOWN_MODE.read(root, "cooldownMode");
        COOLDOWN_DURATION_SECONDS.read(root, "cooldownDurationSeconds");
        RESET_COOLDOWN_ON_DEATH.read(root, "resetCooldownOnDeath");
        MULTIPLAYER_REVIVE.read(root, "multiplayerRevive");
        REVIVE_CHANNEL_SECONDS.read(root, "reviveChannelSeconds");
        REVIVE_DISTANCE.read(root, "reviveDistance");
        REVIVE_INTERRUPT_ON_DAMAGE.read(root, "reviveInterruptOnDamage");
        ALLOW_PASSIVE_KILLS.read(root, "allowPassiveKills");
        ALLOW_PLAYER_KILLS.read(root, "allowPlayerKills");
        ALLOW_PET_KILLS.read(root, "allowPetKills");
        ALLOW_VOID_SECOND_WIND.read(root, "allowVoidSecondWind");
        ENABLE_DOWNED_VIGNETTE.read(root, "enableDownedVignette");
        ENABLE_DESATURATION.read(root, "enableDesaturation");
        ENABLE_DOWNED_BLOOM.read(root, "enableDownedBloom");
        ENABLE_SOUNDS.read(root, "enableSounds");
    }

    private static void write(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        JsonObject root = new JsonObject();
        DOWNED_TIMER_SECONDS.write(root, "downedTimerSeconds");
        MINIMUM_DOWNED_TIMER_SECONDS.write(root, "minimumDownedTimerSeconds");
        TIMER_PENALTY_PER_DOWN.write(root, "timerPenaltyPerDown");
        DOWNED_SLOWNESS_LEVEL.write(root, "downedSlownessLevel");
        DOWNED_DAMAGE_REDUCES_TIMER.write(root, "downedDamageReducesTimer");
        DOWNED_DAMAGE_COOLDOWN_TICKS.write(root, "downedDamageCooldownTicks");
        DOWNED_DAMAGE_REGISTERS.write(root, "downedDamageRegisters");
        BLOCK_HEALING_WHILE_DOWNED.write(root, "blockHealingWhileDowned");
        BLOCK_EATING_WHILE_DOWNED.write(root, "blockEatingWhileDowned");
        REVIVE_HEALTH_HALF_HEARTS.write(root, "reviveHealthHalfHearts");
        REVIVE_REGENERATION_SECONDS.write(root, "reviveRegenerationSeconds");
        POST_REVIVE_INVULNERABILITY_SECONDS.write(root, "postReviveInvulnerabilitySeconds");
        COOLDOWN_MODE.write(root, "cooldownMode");
        COOLDOWN_DURATION_SECONDS.write(root, "cooldownDurationSeconds");
        RESET_COOLDOWN_ON_DEATH.write(root, "resetCooldownOnDeath");
        MULTIPLAYER_REVIVE.write(root, "multiplayerRevive");
        REVIVE_CHANNEL_SECONDS.write(root, "reviveChannelSeconds");
        REVIVE_DISTANCE.write(root, "reviveDistance");
        REVIVE_INTERRUPT_ON_DAMAGE.write(root, "reviveInterruptOnDamage");
        ALLOW_PASSIVE_KILLS.write(root, "allowPassiveKills");
        ALLOW_PLAYER_KILLS.write(root, "allowPlayerKills");
        ALLOW_PET_KILLS.write(root, "allowPetKills");
        ALLOW_VOID_SECOND_WIND.write(root, "allowVoidSecondWind");
        ENABLE_DOWNED_VIGNETTE.write(root, "enableDownedVignette");
        ENABLE_DESATURATION.write(root, "enableDesaturation");
        ENABLE_DOWNED_BLOOM.write(root, "enableDownedBloom");
        ENABLE_SOUNDS.write(root, "enableSounds");
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(root, writer);
        }
    }

    public static final class IntValue {
        private int value;

        private IntValue(int value) {
            this.value = value;
        }

        public Integer get() {
            return value;
        }

        private void read(JsonObject root, String key) {
            if (root.has(key)) {
                value = root.get(key).getAsInt();
            }
        }

        private void write(JsonObject root, String key) {
            root.addProperty(key, value);
        }
    }

    public static final class DoubleValue {
        private double value;

        private DoubleValue(double value) {
            this.value = value;
        }

        public Double get() {
            return value;
        }

        private void read(JsonObject root, String key) {
            if (root.has(key)) {
                value = root.get(key).getAsDouble();
            }
        }

        private void write(JsonObject root, String key) {
            root.addProperty(key, value);
        }
    }

    public static final class BooleanValue {
        private boolean value;

        private BooleanValue(boolean value) {
            this.value = value;
        }

        public Boolean get() {
            return value;
        }

        private void read(JsonObject root, String key) {
            if (root.has(key)) {
                value = root.get(key).getAsBoolean();
            }
        }

        private void write(JsonObject root, String key) {
            root.addProperty(key, value);
        }
    }

    public static final class EnumValue<T extends Enum<T>> {
        private T value;

        private EnumValue(T value) {
            this.value = value;
        }

        public T get() {
            return value;
        }

        private void read(JsonObject root, String key) {
            if (!root.has(key)) {
                return;
            }

            String name = root.get(key).getAsString();
            for (T constant : value.getDeclaringClass().getEnumConstants()) {
                if (constant.name().equalsIgnoreCase(name)) {
                    value = constant;
                    return;
                }
            }
        }

        private void write(JsonObject root, String key) {
            root.addProperty(key, value.name());
        }
    }
}
