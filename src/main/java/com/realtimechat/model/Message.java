package com.realtimechat.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Core protocol model representing messages transferred across the network.
 * Messages are serialized and deserialized as newline-delimited JSON objects.
 */
public class Message {

    private static final Gson GSON = new GsonBuilder().create();

    private MessageType type;
    private String sender;
    private String recipient;
    private String content;
    private long timestamp;
    private Long conversationId;

    public Message() {
        this.timestamp = System.currentTimeMillis();
    }

    public Message(MessageType type, String sender, String recipient, String content) {
        this.type = type;
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    public Message(MessageType type, String sender, String recipient, String content, long timestamp) {
        this.type = type;
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
        this.timestamp = timestamp;
    }

    public Message(MessageType type, String sender, String recipient, String content, Long conversationId) {
        this.type = type;
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
        this.conversationId = conversationId;
        this.timestamp = System.currentTimeMillis();
    }

    public Message(MessageType type, String sender, String recipient, String content, Long conversationId, long timestamp) {
        this.type = type;
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
        this.conversationId = conversationId;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    /**
     * Serializes this Message object into a single-line JSON string.
     */
    public String toJson() {
        return GSON.toJson(this);
    }

    /**
     * Deserializes a JSON string into a Message object.
     */
    public static Message fromJson(String json) {
        return GSON.fromJson(json, Message.class);
    }

    // Factory helper methods
    public static Message createChat(String sender, String recipient, String content, Long conversationId) {
        return new Message(MessageType.CHAT, sender, recipient, content, conversationId);
    }

    public static Message createError(String errorMessage) {
        return new Message(MessageType.ERROR, "SERVER", null, errorMessage);
    }

    public static Message createHistoryRequest(String sender, Long conversationId) {
        Message msg = new Message(MessageType.HISTORY, sender, null, null);
        msg.setConversationId(conversationId);
        return msg;
    }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", sender='" + sender + '\'' +
                ", recipient='" + recipient + '\'' +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                ", conversationId=" + conversationId +
                '}';
    }
}
