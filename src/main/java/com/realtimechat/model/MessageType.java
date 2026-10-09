package com.realtimechat.model;

/**
 * Enumeration representing all protocol message types supported by the chat system.
 */
public enum MessageType {
    LOGIN,        // Authentication request / response
    REGISTER,     // User registration request / response
    CHAT,         // Public or private chat payload
    USER_LIST,    // Online user roster
    HISTORY,      // Chat conversation history request / response
    PRESENCE,     // Online/offline status broadcast to relevant contacts
    NOTIFICATION, // In-app alerts, unread counts, and notifications
    ERROR         // Server error or validation feedback
}
