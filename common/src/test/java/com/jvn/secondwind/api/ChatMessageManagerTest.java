package com.jvn.secondwind.api;

import com.google.gson.JsonParser;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ChatMessageManagerTest {
    @Test
    void parsesTranslationKeysAndFallbackMessages() {
        ChatMessageManager.PoolDefinition definition = ChatMessageManager.parse(
                ResourceLocation.fromNamespaceAndPath("test", "downed"),
                JsonParser.parseString("""
                        {
                          "schema":"secondwind:chat_message_pool/v1",
                          "event":"downed",
                          "priority":5,
                          "messages":[
                            "message.test.downed",
                            {"translate":"message.test.help","fallback":"%1$s needs help!"},
                            {"text":"Get %1$s back up!"}
                          ]
                        }
                        """).getAsJsonObject());

        assertEquals(ChatMessageManager.Event.DOWNED, definition.event());
        assertEquals(5, definition.priority());
        assertEquals(new AnnouncementMessage("message.test.downed", null), definition.messages().get(0));
        assertEquals(new AnnouncementMessage("message.test.help", "%1$s needs help!"), definition.messages().get(1));
        assertEquals(AnnouncementMessage.text("Get %1$s back up!"), definition.messages().get(2));
        assertEquals("Get Alex back up!", definition.messages().get(2).render(net.minecraft.network.chat.Component.literal("Alex"), true).getString());
    }

    @Test
    void highestPriorityReplacesDefaultsAndTiesMerge() {
        ChatMessageManager.PoolDefinition defaults = pool("test", "defaults", ChatMessageManager.Event.DOWNED, 0, "message.default");
        ChatMessageManager.PoolDefinition replacementB = pool("test", "replacement_b", ChatMessageManager.Event.DOWNED, 10, "message.b");
        ChatMessageManager.PoolDefinition replacementA = pool("test", "replacement_a", ChatMessageManager.Event.DOWNED, 10, "message.a");
        ChatMessageManager.PoolDefinition revived = pool("test", "revived", ChatMessageManager.Event.REVIVED_KILL, 0, "message.revived");

        Map<ChatMessageManager.Event, List<AnnouncementMessage>> pools =
                ChatMessageManager.compilePools(List.of(defaults, replacementB, replacementA, revived));

        assertEquals(List.of(new AnnouncementMessage("message.a", null), new AnnouncementMessage("message.b", null)),
                pools.get(ChatMessageManager.Event.DOWNED));
        assertEquals(List.of(new AnnouncementMessage("message.revived", null)), pools.get(ChatMessageManager.Event.REVIVED_KILL));
    }

    @Test
    void rejectsEmptyMessagePools() {
        assertThrows(IllegalArgumentException.class, () -> ChatMessageManager.parse(
                ResourceLocation.fromNamespaceAndPath("test", "empty"),
                JsonParser.parseString("""
                        {"schema":"secondwind:chat_message_pool/v1","event":"downed","messages":[]}
                        """).getAsJsonObject()));
    }

    private static ChatMessageManager.PoolDefinition pool(
            String namespace,
            String path,
            ChatMessageManager.Event event,
            int priority,
            String message) {
        return new ChatMessageManager.PoolDefinition(
                ResourceLocation.fromNamespaceAndPath(namespace, path),
                event,
                priority,
                List.of(new AnnouncementMessage(message, null)));
    }
}
