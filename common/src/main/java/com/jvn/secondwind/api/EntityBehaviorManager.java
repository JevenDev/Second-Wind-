package com.jvn.secondwind.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jvn.secondwind.common.SecondWindCommon;
import dev.architectury.registry.ReloadListenerRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.decoration.ArmorStand;

public final class EntityBehaviorManager extends SimpleJsonResourceReloadListener {
    public static final ResourceLocation SCHEMA = SecondWindCommon.id("entity_behavior/v1");
    private static final Gson GSON = new Gson();
    private static final EntityBehaviorManager INSTANCE = new EntityBehaviorManager();
    private static final Comparator<EntityBehaviorDefinition> ORDER = Comparator
            .comparingInt(EntityBehaviorDefinition::priority).reversed()
            .thenComparing(definition -> !definition.target().exact())
            .thenComparing(definition -> definition.id().toString());
    private static volatile List<EntityBehaviorDefinition> definitions = List.of();

    private EntityBehaviorManager() {
        super(GSON, "secondwind/entity_behaviors");
    }

    public static void register() {
        ReloadListenerRegistry.register(PackType.SERVER_DATA, INSTANCE, SecondWindCommon.id("entity_behaviors"));
    }

    public static Optional<EntityBehaviorDefinition> resolve(LivingEntity entity) {
        if (entity == null || entity instanceof Player || entity instanceof ArmorStand) {
            return Optional.empty();
        }
        return definitions.stream().filter(definition -> definition.matches(entity)).findFirst();
    }

    public static List<EntityBehaviorDefinition> definitions() {
        return definitions;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> prepared, ResourceManager manager, ProfilerFiller profiler) {
        List<EntityBehaviorDefinition> loaded = new ArrayList<>();
        prepared.forEach((id, element) -> {
            try {
                EntityBehaviorDefinition definition = parse(id, GsonHelper.convertToJsonObject(element, "entity behavior"));
                if (definition.lifecycle().type() == EntityBehaviorDefinition.Lifecycle.Type.EXTERNAL
                        && !SecondWindApi.hasExternalAdapter(definition.lifecycle().adapter())) {
                    throw new IllegalArgumentException("external adapter is not registered: " + definition.lifecycle().adapter());
                }
                loaded.add(definition);
            } catch (RuntimeException exception) {
                SecondWindCommon.LOGGER.warn("Skipping invalid Second Wind entity behavior {}: {}", id, exception.getMessage());
            }
        });
        loaded.sort(ORDER);
        for (int first = 0; first < loaded.size(); first++) {
            for (int second = first + 1; second < loaded.size(); second++) {
                EntityBehaviorDefinition left = loaded.get(first);
                EntityBehaviorDefinition right = loaded.get(second);
                if (left.priority() == right.priority() && left.target().equals(right.target())) {
                    SecondWindCommon.LOGGER.warn("Entity behaviors {} and {} have the same target and priority; {} wins by resource order.",
                            left.id(), right.id(), left.id());
                }
            }
        }
        definitions = List.copyOf(loaded);
        SecondWindCommon.LOGGER.info("Loaded {} Second Wind entity behavior definition(s).", definitions.size());
    }

    public static EntityBehaviorDefinition parse(ResourceLocation id, JsonObject root) {
        ResourceLocation schema = ResourceLocation.parse(GsonHelper.getAsString(root, "schema"));
        if (!SCHEMA.equals(schema)) {
            throw new IllegalArgumentException("unsupported schema " + schema);
        }

        String targetValue = GsonHelper.getAsString(root, "target");
        EntityBehaviorDefinition.Target target;
        if (targetValue.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.parse(targetValue.substring(1));
            target = new EntityBehaviorDefinition.Target(null, TagKey.create(Registries.ENTITY_TYPE, tagId));
        } else {
            ResourceLocation typeId = ResourceLocation.parse(targetValue);
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(typeId)
                    || EntityType.PLAYER.equals(BuiltInRegistries.ENTITY_TYPE.get(typeId))
                    || EntityType.ARMOR_STAND.equals(BuiltInRegistries.ENTITY_TYPE.get(typeId))) {
                throw new IllegalArgumentException("unknown or unsupported entity type " + typeId);
            }
            target = new EntityBehaviorDefinition.Target(typeId, null);
        }

        JsonObject lifecycleJson = GsonHelper.getAsJsonObject(root, "lifecycle", new JsonObject());
        String lifecycleValue = GsonHelper.getAsString(lifecycleJson, "type", "managed");
        EntityBehaviorDefinition.Lifecycle.Type lifecycleType = switch (lifecycleValue) {
            case "managed" -> EntityBehaviorDefinition.Lifecycle.Type.MANAGED;
            case "external" -> EntityBehaviorDefinition.Lifecycle.Type.EXTERNAL;
            default -> throw new IllegalArgumentException("unknown lifecycle type " + lifecycleValue);
        };
        ResourceLocation adapter = lifecycleJson.has("adapter")
                ? ResourceLocation.parse(GsonHelper.getAsString(lifecycleJson, "adapter"))
                : null;
        if (lifecycleType == EntityBehaviorDefinition.Lifecycle.Type.EXTERNAL && adapter == null) {
            throw new IllegalArgumentException("external lifecycle requires an adapter");
        }

        JsonObject conditions = GsonHelper.getAsJsonObject(root, "conditions", new JsonObject());
        EntityBehaviorDefinition.Conditions conditionDefinition = new EntityBehaviorDefinition.Conditions(
                optionalBoolean(conditions, "tamed"));

        JsonObject downed = GsonHelper.getAsJsonObject(root, "downed", new JsonObject());
        EntityBehaviorDefinition.Downed downedDefinition = new EntityBehaviorDefinition.Downed(
                optionalInt(downed, "timer_ticks", 1),
                optionalInt(downed, "minimum_timer_ticks", 1),
                optionalInt(downed, "penalty_per_down_ticks", 0),
                optionalDamageMode(downed),
                optionalInt(downed, "damage_cooldown_ticks", 0),
                optionalBoolean(downed, "disable_ai"),
                optionalBoolean(downed, "block_healing"));
        if (downedDefinition.timerTicks() != null && downedDefinition.minimumTimerTicks() != null
                && downedDefinition.minimumTimerTicks() > downedDefinition.timerTicks()) {
            throw new IllegalArgumentException("minimum_timer_ticks cannot exceed timer_ticks");
        }

        JsonObject revive = GsonHelper.getAsJsonObject(root, "revive", new JsonObject());
        EntityBehaviorDefinition.Revive reviveDefinition = new EntityBehaviorDefinition.Revive(
                optionalBoolean(revive, "enabled"),
                optionalInt(revive, "channel_ticks", 0),
                optionalDouble(revive, "distance", 0.0D),
                optionalFloat(revive, "health", 0.0F),
                optionalInt(revive, "regeneration_ticks", 0),
                optionalInt(revive, "invulnerability_ticks", 0),
                optionalInt(revive, "cooldown_ticks", 0));

        JsonObject presentation = GsonHelper.getAsJsonObject(root, "presentation", new JsonObject());
        AnnouncementMessage downedMessage = parseDownedMessage(presentation);
        List<ResourceLocation> poses = new ArrayList<>();
        if (presentation.has("poses")) {
            JsonArray poseArray = GsonHelper.getAsJsonArray(presentation, "poses");
            for (JsonElement pose : poseArray) {
                poses.add(ResourceLocation.parse(GsonHelper.convertToString(pose, "pose id")));
            }
        }
        if (poses.isEmpty()) {
            poses.add(SecondWindCommon.id("sideways"));
        }

        return new EntityBehaviorDefinition(
                id,
                target,
                conditionDefinition,
                GsonHelper.getAsInt(root, "priority", 0),
                new EntityBehaviorDefinition.Lifecycle(lifecycleType, adapter),
                downedDefinition,
                reviveDefinition,
                new EntityBehaviorDefinition.Presentation(
                        GsonHelper.getAsBoolean(presentation, "show_timer", lifecycleType == EntityBehaviorDefinition.Lifecycle.Type.MANAGED),
                        GsonHelper.getAsBoolean(presentation, "announce", true),
                        downedMessage,
                        poses));
    }

    private static AnnouncementMessage parseDownedMessage(JsonObject presentation) {
        if (!presentation.has("downed_message")) {
            return AnnouncementMessage.text("%1$s was downed!");
        }
        return AnnouncementMessage.parse(presentation.get("downed_message"), "downed_message");
    }

    private static Integer optionalInt(JsonObject json, String key, int minimum) {
        if (!json.has(key)) return null;
        int value = GsonHelper.getAsInt(json, key);
        if (value < minimum) throw new IllegalArgumentException(key + " must be at least " + minimum);
        return value;
    }

    private static Double optionalDouble(JsonObject json, String key, double minimum) {
        if (!json.has(key)) return null;
        double value = GsonHelper.getAsDouble(json, key);
        if (!Double.isFinite(value) || value < minimum) throw new IllegalArgumentException(key + " must be at least " + minimum);
        return value;
    }

    private static Float optionalFloat(JsonObject json, String key, float minimum) {
        if (!json.has(key)) return null;
        float value = GsonHelper.getAsFloat(json, key);
        if (!Float.isFinite(value) || value < minimum) throw new IllegalArgumentException(key + " must be at least " + minimum);
        return value;
    }

    private static Boolean optionalBoolean(JsonObject json, String key) {
        return json.has(key) ? GsonHelper.getAsBoolean(json, key) : null;
    }

    private static EntityBehaviorDefinition.Downed.DamageMode optionalDamageMode(JsonObject json) {
        if (!json.has("damage_mode")) return null;
        return switch (GsonHelper.getAsString(json, "damage_mode")) {
            case "ignore" -> EntityBehaviorDefinition.Downed.DamageMode.IGNORE;
            case "reduce_timer" -> EntityBehaviorDefinition.Downed.DamageMode.REDUCE_TIMER;
            case "normal" -> EntityBehaviorDefinition.Downed.DamageMode.NORMAL;
            case "normal_and_reduce_timer" -> EntityBehaviorDefinition.Downed.DamageMode.NORMAL_AND_REDUCE_TIMER;
            default -> throw new IllegalArgumentException("unknown damage_mode");
        };
    }
}
