package com.jvn.secondwind.api;

import com.google.gson.JsonParser;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class EntityBehaviorManagerTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void parsesManagedEntityPolicyAndPosePool() {
        EntityBehaviorDefinition definition = EntityBehaviorManager.parse(
                ResourceLocation.fromNamespaceAndPath("test", "wolf"),
                JsonParser.parseString("""
                        {
                          "schema":"secondwind:entity_behavior/v1",
                          "target":"minecraft:wolf",
                          "conditions":{"tamed":true},
                          "priority":4,
                          "downed":{"timer_ticks":200,"minimum_timer_ticks":40,"damage_mode":"reduce_timer"},
                          "revive":{"enabled":true,"channel_ticks":30,"distance":3.0,"health":8.0},
                          "presentation":{
                            "downed_message":{"translate":"message.test.wolf_downed","fallback":"%1$s needs help!"},
                            "poses":["secondwind:sideways","test:fallen"]
                          }
                        }
                        """).getAsJsonObject());

        assertEquals(EntityBehaviorDefinition.Lifecycle.Type.MANAGED, definition.lifecycle().type());
        assertTrue(definition.target().exact());
        assertTrue(definition.conditions().tamed());
        assertEquals(200, definition.downed().timerTicks());
        assertEquals(EntityBehaviorDefinition.Downed.DamageMode.REDUCE_TIMER, definition.downed().damageMode());
        assertEquals(2, definition.presentation().poses().size());
        assertEquals("message.test.wolf_downed", definition.presentation().downedMessage().translationKey());
        assertEquals("%1$s needs help!", definition.presentation().downedMessage().fallback());
    }

    @Test
    void parsesExternalTagPolicyWithoutManagedTimer() {
        EntityBehaviorDefinition definition = EntityBehaviorManager.parse(
                ResourceLocation.fromNamespaceAndPath("test", "external"),
                JsonParser.parseString("""
                        {
                          "schema":"secondwind:entity_behavior/v1",
                          "target":"#test:companions",
                          "lifecycle":{"type":"external","adapter":"test:companion"},
                          "presentation":{"show_timer":false,"poses":["test:resting"]}
                        }
                        """).getAsJsonObject());

        assertEquals(EntityBehaviorDefinition.Lifecycle.Type.EXTERNAL, definition.lifecycle().type());
        assertFalse(definition.target().exact());
        assertEquals(ResourceLocation.fromNamespaceAndPath("test", "companion"), definition.lifecycle().adapter());
        assertFalse(definition.presentation().showTimer());
        assertTrue(definition.presentation().announce());
        assertNull(definition.conditions().tamed());
    }

    @Test
    void rejectsPlayersAndInconsistentTimers() {
        assertThrows(IllegalArgumentException.class, () -> EntityBehaviorManager.parse(
                ResourceLocation.fromNamespaceAndPath("test", "player"),
                JsonParser.parseString("{\"schema\":\"secondwind:entity_behavior/v1\",\"target\":\"minecraft:player\"}").getAsJsonObject()));
        assertThrows(IllegalArgumentException.class, () -> EntityBehaviorManager.parse(
                ResourceLocation.fromNamespaceAndPath("test", "timers"),
                JsonParser.parseString("{\"schema\":\"secondwind:entity_behavior/v1\",\"target\":\"minecraft:zombie\",\"downed\":{\"timer_ticks\":20,\"minimum_timer_ticks\":40}}").getAsJsonObject()));
    }

    @Test
    void defaultsToSidewaysPoseAndEntityAnnouncement() {
        EntityBehaviorDefinition definition = EntityBehaviorManager.parse(
                ResourceLocation.fromNamespaceAndPath("test", "zombie"),
                JsonParser.parseString("""
                        {"schema":"secondwind:entity_behavior/v1","target":"minecraft:zombie"}
                        """).getAsJsonObject());

        assertEquals(List.of(ResourceLocation.fromNamespaceAndPath("secondwind", "sideways")), definition.presentation().poses());
        assertEquals("message.secondwind.entity_downed", definition.presentation().downedMessage().translationKey());
        assertNull(definition.presentation().downedMessage().fallback());
    }
}
