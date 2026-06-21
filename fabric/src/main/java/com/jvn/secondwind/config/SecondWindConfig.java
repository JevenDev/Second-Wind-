package com.jvn.secondwind.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jvn.secondwind.SecondWindMod;
import io.wispforest.owo.config.ConfigWrapper;
import io.wispforest.owo.config.Option;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

public final class SecondWindConfig {
    public static final ConfigWrapper<?> CONFIG = loadConfig();

    public static final ConfigValue<Integer> DOWNED_TIMER_SECONDS = bind("secondWind.downedTimerSeconds", Integer.class);
    public static final ConfigValue<Integer> MINIMUM_DOWNED_TIMER_SECONDS = bind("secondWind.minimumDownedTimerSeconds", Integer.class);
    public static final ConfigValue<Integer> TIMER_PENALTY_PER_DOWN = bind("secondWind.timerPenaltyPerDown", Integer.class);
    public static final ConfigValue<Integer> DOWNED_SLOWNESS_LEVEL = bind("secondWind.downedSlownessLevel", Integer.class);
    public static final ConfigValue<Boolean> DOWNED_DAMAGE_REDUCES_TIMER = bind("secondWind.downedDamageReducesTimer", Boolean.class);
    public static final ConfigValue<Integer> DOWNED_DAMAGE_COOLDOWN_TICKS = bind("secondWind.downedDamageCooldownTicks", Integer.class);
    public static final ConfigValue<Boolean> DOWNED_DAMAGE_REGISTERS = bind("secondWind.downedDamageRegisters", Boolean.class);
    public static final ConfigValue<Boolean> BLOCK_HEALING_WHILE_DOWNED = bind("secondWind.blockHealingWhileDowned", Boolean.class);
    public static final ConfigValue<Boolean> BLOCK_EATING_WHILE_DOWNED = bind("secondWind.blockEatingWhileDowned", Boolean.class);
    public static final ConfigValue<Integer> REVIVE_HEALTH_HALF_HEARTS = bind("secondWind.reviveHealthHalfHearts", Integer.class);
    public static final ConfigValue<Integer> REVIVE_REGENERATION_SECONDS = bind("secondWind.reviveRegenerationSeconds", Integer.class);
    public static final ConfigValue<Integer> POST_REVIVE_INVULNERABILITY_SECONDS = bind("secondWind.postReviveInvulnerabilitySeconds", Integer.class);
    public static final ConfigValue<CooldownMode> COOLDOWN_MODE = bind("secondWind.cooldownMode", CooldownMode.class);
    public static final ConfigValue<Integer> COOLDOWN_DURATION_SECONDS = bind("secondWind.cooldownDurationSeconds", Integer.class);
    public static final ConfigValue<Boolean> RESET_COOLDOWN_ON_DEATH = bind("secondWind.resetCooldownOnDeath", Boolean.class);
    public static final ConfigValue<Boolean> MULTIPLAYER_REVIVE = bind("multiplayerRevive.multiplayerRevive", Boolean.class);
    public static final ConfigValue<Double> REVIVE_CHANNEL_SECONDS = bind("multiplayerRevive.reviveChannelSeconds", Double.class);
    public static final ConfigValue<Double> REVIVE_DISTANCE = bind("multiplayerRevive.reviveDistance", Double.class);
    public static final ConfigValue<Boolean> REVIVE_INTERRUPT_ON_DAMAGE = bind("multiplayerRevive.reviveInterruptOnDamage", Boolean.class);
    public static final ConfigValue<Boolean> ALLOW_PASSIVE_KILLS = bind("killRules.allowPassiveKills", Boolean.class);
    public static final ConfigValue<Boolean> ALLOW_PLAYER_KILLS = bind("killRules.allowPlayerKills", Boolean.class);
    public static final ConfigValue<Boolean> ALLOW_PET_KILLS = bind("killRules.allowPetKills", Boolean.class);
    public static final ConfigValue<Boolean> ALLOW_VOID_SECOND_WIND = bind("killRules.allowVoidSecondWind", Boolean.class);
    public static final ConfigValue<Boolean> ENABLE_DOWNED_VIGNETTE = bind("clientFeedback.enableDownedVignette", Boolean.class);
    public static final ConfigValue<Boolean> ENABLE_DESATURATION = bind("clientFeedback.enableDesaturation", Boolean.class);
    public static final ConfigValue<Boolean> ENABLE_DOWNED_BLOOM = bind("clientFeedback.enableDownedBloom", Boolean.class);
    public static final ConfigValue<Boolean> ENABLE_SOUNDS = bind("clientFeedback.enableSounds", Boolean.class);
    public static final ConfigValue<Boolean> ENABLE_CHAT_MESSAGES = bind("clientFeedback.enableChatMessages", Boolean.class);
    public static final ConfigValue<Boolean> LOCALIZE_CHAT_MESSAGES = bind("clientFeedback.localizeChatMessages", Boolean.class);
    public static final ConfigValue<Boolean> ENABLE_SECOND_WIND_POPUP = bind("clientFeedback.enableSecondWindPopup", Boolean.class);
    public static final ConfigValue<Boolean> USE_SIMPLE_DOWNED_TIMER = bind("clientFeedback.useSimpleDownedTimer", Boolean.class);

    private static final Map<String, String> LEGACY_KEYS = Map.ofEntries(
            Map.entry("downedTimerSeconds", "secondWind.downedTimerSeconds"),
            Map.entry("minimumDownedTimerSeconds", "secondWind.minimumDownedTimerSeconds"),
            Map.entry("timerPenaltyPerDown", "secondWind.timerPenaltyPerDown"),
            Map.entry("downedSlownessLevel", "secondWind.downedSlownessLevel"),
            Map.entry("downedDamageReducesTimer", "secondWind.downedDamageReducesTimer"),
            Map.entry("downedDamageCooldownTicks", "secondWind.downedDamageCooldownTicks"),
            Map.entry("downedDamageRegisters", "secondWind.downedDamageRegisters"),
            Map.entry("blockHealingWhileDowned", "secondWind.blockHealingWhileDowned"),
            Map.entry("blockEatingWhileDowned", "secondWind.blockEatingWhileDowned"),
            Map.entry("reviveHealthHalfHearts", "secondWind.reviveHealthHalfHearts"),
            Map.entry("reviveRegenerationSeconds", "secondWind.reviveRegenerationSeconds"),
            Map.entry("postReviveInvulnerabilitySeconds", "secondWind.postReviveInvulnerabilitySeconds"),
            Map.entry("cooldownMode", "secondWind.cooldownMode"),
            Map.entry("cooldownDurationSeconds", "secondWind.cooldownDurationSeconds"),
            Map.entry("resetCooldownOnDeath", "secondWind.resetCooldownOnDeath"),
            Map.entry("multiplayerRevive", "multiplayerRevive.multiplayerRevive"),
            Map.entry("reviveChannelSeconds", "multiplayerRevive.reviveChannelSeconds"),
            Map.entry("reviveDistance", "multiplayerRevive.reviveDistance"),
            Map.entry("reviveInterruptOnDamage", "multiplayerRevive.reviveInterruptOnDamage"),
            Map.entry("allowPassiveKills", "killRules.allowPassiveKills"),
            Map.entry("allowPlayerKills", "killRules.allowPlayerKills"),
            Map.entry("allowPetKills", "killRules.allowPetKills"),
            Map.entry("allowVoidSecondWind", "killRules.allowVoidSecondWind"),
            Map.entry("enableDownedVignette", "clientFeedback.enableDownedVignette"),
            Map.entry("enableDesaturation", "clientFeedback.enableDesaturation"),
            Map.entry("enableDownedBloom", "clientFeedback.enableDownedBloom"),
            Map.entry("enableSounds", "clientFeedback.enableSounds"),
            Map.entry("enableChatMessages", "clientFeedback.enableChatMessages"),
            Map.entry("localizeChatMessages", "clientFeedback.localizeChatMessages"),
            Map.entry("enableSecondWindPopup", "clientFeedback.enableSecondWindPopup"),
            Map.entry("useSimpleDownedTimer", "clientFeedback.useSimpleDownedTimer"));

    private SecondWindConfig() {
    }

    public static void init() {
    }

    public static void load() {
        CONFIG.load();
    }

    public static void save() {
        CONFIG.save();
    }

    public static void resetDefaults() {
        CONFIG.allOptions().values().forEach(SecondWindConfig::resetOption);
        CONFIG.save();
    }

    private static ConfigWrapper<?> loadConfig() {
        ConfigWrapper<?> config = instantiateConfigWrapper();
        if (!migrateLegacyJsonIfNeeded(config)) {
            config.load();
        }
        return config;
    }

    private static ConfigWrapper<?> instantiateConfigWrapper() {
        try {
            Class<?> wrapperClass = Class.forName("com.jvn.secondwind.config.SecondWindOwoConfig");
            var constructor = wrapperClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (ConfigWrapper<?>) constructor.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                 | NoSuchMethodException | InvocationTargetException exception) {
            throw new IllegalStateException("Failed to initialize generated owo config wrapper", exception);
        }
    }

    private static boolean migrateLegacyJsonIfNeeded(ConfigWrapper<?> config) {
        if (Files.exists(config.fileLocation())) {
            return false;
        }

        Path legacyPath = FabricLoader.getInstance().getConfigDir().resolve("secondwind.json");
        if (!Files.exists(legacyPath)) {
            return false;
        }

        boolean migratedAny = false;
        try (Reader reader = Files.newBufferedReader(legacyPath)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (Map.Entry<String, String> entry : LEGACY_KEYS.entrySet()) {
                if (!root.has(entry.getKey())) {
                    continue;
                }
                migratedAny |= applyJsonValue(config, entry.getValue(), root.get(entry.getKey()));
            }
        } catch (Exception exception) {
            SecondWindMod.LOGGER.warn("Failed to migrate legacy Fabric config from {}.", legacyPath, exception);
        }

        if (migratedAny) {
            config.save();
        }
        return migratedAny;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean applyJsonValue(ConfigWrapper<?> config, String key, JsonElement element) {
        Option option = config.optionForKey(new Option.Key(key));
        if (option == null) {
            return false;
        }

        try {
            option.set(parseJsonValue(option.clazz(), element));
            return true;
        } catch (Exception exception) {
            SecondWindMod.LOGGER.warn("Failed to migrate legacy config value {}", key, exception);
            return false;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object parseJsonValue(Class<?> type, JsonElement element) {
        if (type == boolean.class || type == Boolean.class) {
            return element.getAsBoolean();
        }
        if (type == int.class || type == Integer.class) {
            return element.getAsInt();
        }
        if (type == double.class || type == Double.class) {
            return element.getAsDouble();
        }
        if (type.isEnum()) {
            return Enum.valueOf((Class<? extends Enum>) type.asSubclass(Enum.class), element.getAsString());
        }
        throw new IllegalArgumentException("Unsupported legacy config value type: " + type.getName());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void resetOption(Option<?> option) {
        ((Option) option).set(option.defaultValue());
    }

    private static <T> ConfigValue<T> bind(String key, Class<T> expectedType) {
        Option<?> option = CONFIG.optionForKey(new Option.Key(key));
        if (option == null) {
            throw new IllegalStateException("Missing config option: " + key);
        }
        if (!isCompatibleType(option.clazz(), expectedType)) {
            throw new IllegalStateException("Config option " + key + " expected " + expectedType.getName()
                    + " but found " + option.clazz().getName());
        }
        @SuppressWarnings("unchecked")
        Option<T> typedOption = (Option<T>) option;
        return new ConfigValue<>(typedOption);
    }

    private static boolean isCompatibleType(Class<?> actualType, Class<?> expectedType) {
        return boxed(actualType) == boxed(expectedType);
    }

    private static Class<?> boxed(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        return type;
    }

    public static final class ConfigValue<T> {
        private final Option<T> option;

        private ConfigValue(Option<T> option) {
            this.option = option;
        }

        public T get() {
            return option.value();
        }

        public void set(T value) {
            option.set(value);
        }

        public Option<T> option() {
            return option;
        }
    }
}
