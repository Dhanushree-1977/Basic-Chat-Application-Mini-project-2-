package com.realtimechat.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.realtimechat.database.MessageRepository;
import com.realtimechat.database.UserRepository;
import com.realtimechat.model.Message;
import com.realtimechat.model.MessageType;
import com.realtimechat.model.StoredMessage;
import com.realtimechat.model.User;
import com.realtimechat.server.ClientHandler;

import java.util.List;
import java.util.Set;

/**
 * Routes private one-to-one messages.
 * Stores every message in MySQL, delivers live across all active user sessions
 * via PresenceManager, queues messages for offline users, and enforces participant
 * access controls on chat history.
 */
public class MessageRouter {

    private static final Gson GSON = new GsonBuilder().create();

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final PresenceManager presenceManager;

    public MessageRouter(UserRepository userRepository, 
                         MessageRepository messageRepository, 
                         PresenceManager presenceManager) {
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.presenceManager = presenceManager;
    }

    /**
     * Routes a direct 1-to-1 message between two users.
     * Persists the message in MySQL and delivers live to all active sessions of the recipient.
     *
     * @param senderUser        authenticated sender
     * @param recipientUsername recipient's username
     * @param content           chat message text
     * @return status Message to notify sender of delivery state
     */
    public Message routeDirectMessage(User senderUser, String recipientUsername, String content) {
        // 1. Verify recipient exists in database
        User recipientUser = userRepository.findByUsername(recipientUsername);
        if (recipientUser == null) {
            return Message.createError("User '" + recipientUsername + "' does not exist.");
        }

        if (recipientUser.getId().equals(senderUser.getId())) {
            return Message.createError("You cannot send a private message to yourself.");
        }

        // 2. Locate or create 1-to-1 conversation
        long conversationId = messageRepository.getOrCreateDirectConversation(senderUser.getId(), recipientUser.getId());
        if (conversationId <= 0) {
            return Message.createError("Database error creating conversation with " + recipientUsername);
        }

        // 3. Store message in MySQL (default: is_delivered = false)
        long messageId = messageRepository.saveMessage(conversationId, senderUser.getId(), content);
        if (messageId <= 0) {
            return Message.createError("Database error persisting message.");
        }

        // 4. Multi-session delivery via PresenceManager
        Set<ClientHandler> activeSessions = presenceManager.getSessions(recipientUsername);
        if (!activeSessions.isEmpty()) {
            Message liveMessage = new Message(MessageType.CHAT, senderUser.getUsername(), recipientUsername, content, conversationId);
            for (ClientHandler session : activeSessions) {
                session.sendMessage(liveMessage);
            }

            // Update delivered flag in database
            messageRepository.markDelivered(messageId);
            System.out.println("[MessageRouter] Live message delivered from " + senderUser.getUsername() + 
                               " to " + activeSessions.size() + " active session(s) of " + recipientUsername);
            return new Message(MessageType.CHAT, "SERVER", senderUser.getUsername(), 
                    "Message delivered live to " + recipientUsername, conversationId);
        } else {
            // Recipient is offline; message remains in DB with is_delivered = false
            System.out.println("[MessageRouter] Stored offline message for " + recipientUsername + " (msgId=" + messageId + ")");
            return new Message(MessageType.CHAT, "SERVER", senderUser.getUsername(), 
                    "Message queued: " + recipientUsername + " is currently offline.", conversationId);
        }
    }

    /**
     * Checks for and delivers all unread messages queued while a user was offline.
     *
     * @param user    the newly logged-in user
     * @param handler the user's active connection handler
     */
    public void deliverOfflineMessages(User user, ClientHandler handler) {
        List<StoredMessage> undelivered = messageRepository.getUndeliveredMessagesForUser(user.getId());
        if (undelivered.isEmpty()) {
            return;
        }

        System.out.println("[MessageRouter] Delivering " + undelivered.size() + 
                           " offline messages to " + user.getUsername());

        handler.sendMessage(new Message(MessageType.CHAT, "SERVER", user.getUsername(), 
                "You have " + undelivered.size() + " unread offline message(s):"));

        for (StoredMessage stored : undelivered) {
            Message networkMsg = stored.toNetworkMessage();
            networkMsg.setRecipient(user.getUsername());
            networkMsg.setConversationId(stored.getConversationId());
            handler.sendMessage(networkMsg);

            // Mark delivered in MySQL
            messageRepository.markDelivered(stored.getId());
        }
    }

    /**
     * Fetches conversation history by conversation ID.
     * Strictly validates that the requesting user belongs to the conversation.
     *
     * @param requesterUser  authenticated requester
     * @param conversationId conversation ID
     * @param limit          max messages to retrieve
     * @return Message containing history or ERROR if unauthorized
     */
    public Message fetchHistory(User requesterUser, long conversationId, int limit) {
        // SECURITY CHECK: Verify requester is a participant of this conversation
        boolean isParticipant = messageRepository.isUserInConversation(requesterUser.getId(), conversationId);
        if (!isParticipant) {
            System.out.println("[MessageRouter] UNAUTHORIZED: " + requesterUser.getUsername() + 
                               " attempted to access conversation #" + conversationId);
            return Message.createError("Unauthorized: You do not have permission to view conversation #" + conversationId);
        }

        List<StoredMessage> history = messageRepository.getConversationHistory(conversationId, limit);
        String historyJson = GSON.toJson(history);

        Message response = new Message(MessageType.HISTORY, "SERVER", requesterUser.getUsername(), historyJson);
        response.setConversationId(conversationId);
        return response;
    }

    /**
     * Convenience method to fetch history between two users by recipient username.
     */
    public Message fetchDirectHistoryWithUser(User requesterUser, String otherUsername, int limit) {
        User otherUser = userRepository.findByUsername(otherUsername);
        if (otherUser == null) {
            return Message.createError("User '" + otherUsername + "' does not exist.");
        }

        long convoId = messageRepository.getOrCreateDirectConversation(requesterUser.getId(), otherUser.getId());
        return fetchHistory(requesterUser, convoId, limit);
    }
}
