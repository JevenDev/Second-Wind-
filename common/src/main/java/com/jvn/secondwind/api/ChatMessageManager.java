package com.jvn.secondwind.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jvn.secondwind.common.SecondWindCommon;
import dev.architectury.registry.ReloadListenerRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;

public final class ChatMessageManager extends SimpleJsonResourceReloadListener {
    public static final ResourceLocation SCHEMA = SecondWindCommon.id("chat_message_pool/v1");
    private static final Gson GSON = new Gson();
    private static final ChatMessageManager INSTANCE = new ChatMessageManager();
    private static volatile Map<Event, List<AnnouncementMessage>> pools = Map.of();

    private ChatMessageManager() {
        super(GSON, "secondwind/chat_messages");
    }

    public static void register() {
        ReloadListenerRegistry.register(PackType.SERVER_DATA, INSTANCE, SecondWindCommon.id("chat_messages"));
    }

    public static Optional<AnnouncementMessage> select(Event event, RandomSource random) {
        List<AnnouncementMessage> messages = pools.getOrDefault(event, List.of());
        return messages.isEmpty() ? Optional.empty() : Optional.of(messages.get(random.nextInt(messages.size())));
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> prepared, ResourceManager manager, ProfilerFiller profiler) {
        List<PoolDefinition> definitions = new ArrayList<>();
        prepared.forEach((id, element) -> {
            try {
                definitions.add(parse(id, GsonHelper.convertToJsonObject(element, "chat message pool")));
            } catch (RuntimeException exception) {
                SecondWindCommon.LOGGER.warn("Skipping invalid Second Wind chat message pool {}: {}", id, exception.getMessage());
            }
        });
        pools = compilePools(definitions);
        SecondWindCommon.LOGGER.info("Loaded {} Second Wind chat message pool(s).", definitions.size());
    }

    static PoolDefinition parse(ResourceLocation id, JsonObject root) {
        ResourceLocation schema = ResourceLocation.parse(GsonHelper.getAsString(root, "schema"));
        if (!SCHEMA.equals(schema)) {
            throw new IllegalArgumentException("unsupported schema " + schema);
        }

        Event event = Event.parse(GsonHelper.getAsString(root, "event"));
        JsonArray messageArray = GsonHelper.getAsJsonArray(root, "messages");
        if (messageArray.isEmpty()) {
            throw new IllegalArgumentException("messages cannot be empty");
        }
        List<AnnouncementMessage> messages = new ArrayList<>();
        for (JsonElement message : messageArray) {
            messages.add(AnnouncementMessage.parse(message, "message"));
        }
        return new PoolDefinition(id, event, GsonHelper.getAsInt(root, "priority", 0), List.copyOf(messages));
    }

    static Map<Event, List<AnnouncementMessage>> compilePools(List<PoolDefinition> definitions) {
        Map<Event, List<AnnouncementMessage>> compiled = new EnumMap<>(Event.class);
        for (Event event : Event.values()) {
            int highestPriority = definitions.stream()
                    .filter(definition -> definition.event() == event)
                    .mapToInt(PoolDefinition::priority)
                    .max()
                    .orElse(Integer.MIN_VALUE);
            List<AnnouncementMessage> messages = definitions.stream()
                    .filter(definition -> definition.event() == event && definition.priority() == highestPriority)
                    .sorted(Comparator.comparing(definition -> definition.id().toString()))
                    .flatMap(definition -> definition.messages().stream())
                    .toList();
            if (!messages.isEmpty()) compiled.put(event, messages);
        }
        return Map.copyOf(compiled);
    }

    public enum Event {
        DOWNED,
        REVIVED_PLAYER,
        REVIVED_KILL,
        REVIVED_ADMIN;

        static Event parse(String value) {
            return switch (value) {
                case "downed" -> DOWNED;
                case "revived/player_revive" -> REVIVED_PLAYER;
                case "revived/kill" -> REVIVED_KILL;
                case "revived/admin" -> REVIVED_ADMIN;
                default -> throw new IllegalArgumentException("unknown chat message event " + value);
            };
        }
    }

    record PoolDefinition(ResourceLocation id, Event event, int priority, List<AnnouncementMessage> messages) {
    }
}
