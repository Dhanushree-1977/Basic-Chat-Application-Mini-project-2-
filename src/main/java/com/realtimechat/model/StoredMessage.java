package com.realtimechat.model;

import java.sql.Timestamp;

/**
 * Entity representing a persistent message record loaded from the MySQL 'messages' table.
 */
public class StoredMessage {

    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderUsername;
    private String content;
    private boolean isDelivered;
    private Timestamp createdAt;

    public StoredMessage() {}

    public StoredMessage(Long id, Long conversationId, Long senderId, String senderUsername, 
                         String content, boolean isDelivered, Timestamp createdAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.content = content;
        this.isDelivered = isDelivered;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isDelivered() {
        return isDelivered;
    }

    public void setDelivered(boolean delivered) {
        isDelivered = delivered;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Converts this database entity into a network protocol Message object.
     */
    public Message toNetworkMessage() {
        return new Message(MessageType.CHAT, senderUsername, null, content, createdAt.getTime());
    }
}
