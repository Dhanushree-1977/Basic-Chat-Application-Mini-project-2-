package com.realtimechat.model;

import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying JSON serialization, deserialization,
 * and protocol contract integrity for the Message model.
 */
public class MessageTest {

    @Test
    @DisplayName("Test serialization and deserialization of a standard chat message")
    public void testSerializationAndDeserialization() {
        Message original = new Message(MessageType.CHAT, "alice", "bob", "Hello Bob, secure TLS check!", 1001L, 1700000000000L);

        String json = original.toJson();
        assertNotNull(json);
        assertTrue(json.contains("\"type\":\"CHAT\""));
        assertTrue(json.contains("\"sender\":\"alice\""));
        assertTrue(json.contains("\"recipient\":\"bob\""));
        assertTrue(json.contains("\"content\":\"Hello Bob, secure TLS check!\""));
        assertTrue(json.contains("\"conversationId\":1001"));

        Message deserialized = Message.fromJson(json);
        assertNotNull(deserialized);
        assertEquals(MessageType.CHAT, deserialized.getType());
        assertEquals("alice", deserialized.getSender());
        assertEquals("bob", deserialized.getRecipient());
        assertEquals("Hello Bob, secure TLS check!", deserialized.getContent());
        assertEquals(1001L, deserialized.getConversationId());
        assertEquals(1700000000000L, deserialized.getTimestamp());
    }

    @Test
    @DisplayName("Test protocol coverage for all MessageType enum variants")
    public void testAllMessageTypesSerialization() {
        for (MessageType type : MessageType.values()) {
            Message msg = new Message(type, "testUser", "targetUser", "Payload for " + type);
            String json = msg.toJson();
            Message parsed = Message.fromJson(json);

            assertNotNull(parsed, "Parsed message should not be null for type " + type);
            assertEquals(type, parsed.getType(), "MessageType mismatch for " + type);
            assertEquals("testUser", parsed.getSender());
            assertEquals("Payload for " + type, parsed.getContent());
        }
    }

    @Test
    @DisplayName("Test handling of malformed or invalid JSON input")
    public void testMalformedJsonThrowsException() {
        String invalidJson = "{ bad json without quotes: true ";
        assertThrows(JsonSyntaxException.class, () -> Message.fromJson(invalidJson));
    }

    @Test
    @DisplayName("Test factory helper methods")
    public void testFactoryHelperMethods() {
        Message errorMsg = Message.createError("Database unreachable");
        assertEquals(MessageType.ERROR, errorMsg.getType());
        assertEquals("SERVER", errorMsg.getSender());
        assertEquals("Database unreachable", errorMsg.getContent());

        Message historyMsg = Message.createHistoryRequest("alice", 42L);
        assertEquals(MessageType.HISTORY, historyMsg.getType());
        assertEquals("alice", historyMsg.getSender());
        assertEquals(42L, historyMsg.getConversationId());
    }
}
