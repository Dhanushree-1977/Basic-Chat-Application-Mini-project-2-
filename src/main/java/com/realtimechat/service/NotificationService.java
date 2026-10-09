package com.realtimechat.service;

import com.google.gson.Gson;
import com.realtimechat.database.MessageRepository;
import com.realtimechat.model.Message;
import com.realtimechat.model.MessageType;
import com.realtimechat.model.User;
import com.realtimechat.server.ClientHandler;

import java.util.Map;

/**
 * Service managing unread counts and in-app notifications.
 * Summarizes pending messages upon login and dispatches real-time alerts.
 */
public class NotificationService {

    private static final Gson GSON = new Gson();
    private final MessageRepository messageRepository;

    public NotificationService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    /**
     * Checks unread message counts for a newly logged-in user and delivers
     * an in-app notification summary if unread messages are waiting.
     *
     * @param user    the authenticated user
     * @param handler the target client session handler
     */
    public void sendUnreadSummary(User user, ClientHandler handler) {
        Map<String, Integer> unreadBySender = messageRepository.getUnreadCountsBySender(user.getId());

        if (unreadBySender.isEmpty()) {
            return;
        }

        int totalUnread = unreadBySender.values().stream().mapToInt(Integer::intValue).sum();
        String jsonPayload = GSON.toJson(unreadBySender);

        // Send structured NOTIFICATION message
        Message notification = new Message(MessageType.NOTIFICATION, "SERVER", user.getUsername(), jsonPayload);
        handler.sendMessage(notification);

        System.out.println("[NotificationService] Sent unread summary to " + user.getUsername() + 
                           ": " + totalUnread + " unread message(s) from " + unreadBySender.keySet());
    }

    /**
     * Sends an in-app notification to a specific client session.
     *
     * @param handler     target handler
     * @param recipient   target username
     * @param alertNotice textual alert
     */
    public void sendInAppAlert(ClientHandler handler, String recipient, String alertNotice) {
        if (handler != null) {
            Message alert = new Message(MessageType.NOTIFICATION, "SERVER", recipient, alertNotice);
            handler.sendMessage(alert);
        }
    }
}
