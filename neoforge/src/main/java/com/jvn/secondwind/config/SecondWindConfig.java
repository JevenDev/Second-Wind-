package com.jvn.secondwind.config;

import com.jvn.secondwind.SecondWindMod;
import io.wispforest.owo.config.ConfigWrapper;
import io.wispforest.owo.config.Option;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;

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
        if (!migrateLegacyTomlIfNeeded(config)) {
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

    private static boolean migrateLegacyTomlIfNeeded(ConfigWrapper<?> config) {
        if (Files.exists(config.fileLocation())) {
            return false;
        }

        Path legacyPath = config.fileLocation().getParent().resolve("secondwind-common.toml");
        if (!Files.exists(legacyPath)) {
            return false;
        }

        boolean migrated = migrateLegacyToml(config, legacyPath);
        if (migrated) {
            config.save();
        }
        return migrated;
    }

    private static boolean migrateLegacyToml(ConfigWrapper<?> config, Path path) {
        boolean migratedAny = false;
        String currentSection = null;

        try {
            for (String rawLine : Files.readAllLines(path)) {
                String line = stripTomlComment(rawLine).trim();
                if (line.isEmpty()) {
                    continue;
                }

                if (line.startsWith("[") && line.endsWith("]")) {
                    currentSection = line.substring(1, line.length() - 1).trim().replace("\"", "");
                    continue;
                }

                if (currentSection == null) {
                    continue;
                }

                int separator = line.indexOf('=');
                if (separator < 0) {
                    continue;
                }

                String key = mapLegacyTomlKey(currentSection + "." + line.substring(0, separator).trim());
                String value = line.substring(separator + 1).trim();
                migratedAny |= applyLegacyValue(config, key, value);
            }
        } catch (IOException exception) {
            SecondWindMod.LOGGER.warn("Failed to read legacy config file {}", path, exception);
        }

        return migratedAny;
    }

    private static String stripTomlComment(String line) {
        int commentIndex = line.indexOf('#');
        return commentIndex >= 0 ? line.substring(0, commentIndex) : line;
    }

    private static String mapLegacyTomlKey(String key) {
        return key.startsWith("secondWind.multiplayerRevive.")
                || key.startsWith("secondWind.killRules.")
                || key.startsWith("secondWind.clientFeedback.")
                ? key.substring("secondWind.".length())
                : key;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean applyLegacyValue(ConfigWrapper<?> config, String key, String rawValue) {
        Option option = config.optionForKey(new Option.Key(key));
        if (option == null) {
            return false;
        }

        try {
            option.set(parseLegacyValue(option.clazz(), rawValue));
            return true;
        } catch (Exception exception) {
            SecondWindMod.LOGGER.warn("Failed to migrate legacy config value {}={}", key, rawValue, exception);
            return false;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object parseLegacyValue(Class<?> valueType, String rawValue) {
        String normalized = rawValue.trim();
        if ((normalized.startsWith("\"") && normalized.endsWith("\""))
                || (normalized.startsWith("'") && normalized.endsWith("'"))) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }

        if (valueType == boolean.class || valueType == Boolean.class) {
            return Boolean.parseBoolean(normalized);
        }
        if (valueType == int.class || valueType == Integer.class) {
            return Integer.parseInt(normalized);
        }
        if (valueType == double.class || valueType == Double.class) {
            return Double.parseDouble(normalized);
        }
        if (valueType.isEnum()) {
            return Enum.valueOf((Class<? extends Enum>) valueType.asSubclass(Enum.class), normalized);
        }
        throw new IllegalArgumentException("Unsupported legacy config value type: " + valueType.getName());
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
