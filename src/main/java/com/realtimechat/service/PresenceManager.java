package com.realtimechat.service;

import com.realtimechat.database.MessageRepository;
import com.realtimechat.model.Message;
import com.realtimechat.model.MessageType;
import com.realtimechat.server.ClientHandler;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manages user presence (online/offline states), supports multiple concurrent sessions
 * per user, and selectively broadcasts presence transitions only to relevant contacts.
 */
public class PresenceManager {

    // Maps each username to their active connection sessions (e.g., Desktop + Mobile)
    private final ConcurrentMap<String, Set<ClientHandler>> userSessions = new ConcurrentHashMap<>();
    private final MessageRepository messageRepository;

    public PresenceManager(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    /**
     * Registers a new connection session for a user.
     * If this is the user's first active session, triggers an ONLINE presence transition
     * broadcast exclusively to relevant contacts who share conversations with them.
     *
     * @param username username logging in
     * @param handler  the active client session handler
     * @param userId   user ID in database
     */
    public synchronized void registerSession(String username, ClientHandler handler, long userId) {
        Set<ClientHandler> sessions = userSessions.computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet());
        boolean wasOffline = sessions.isEmpty();

        sessions.add(handler);
        System.out.println("[PresenceManager] Registered session for '" + username + 
                           "'. Active sessions: " + sessions.size());

        if (wasOffline) {
            System.out.println("[PresenceManager] Presence transition: " + username + " is now ONLINE.");
            broadcastPresenceToRelevantContacts(username, userId, "ONLINE");
        }
    }

    /**
     * Unregisters a disconnected session for a user.
     * Only triggers an OFFLINE transition when the user's LAST session closes.
     *
     * @param username username disconnecting
     * @param handler  the terminating client session handler
     * @param userId   user ID in database
     */
    public synchronized void unregisterSession(String username, ClientHandler handler, long userId) {
        Set<ClientHandler> sessions = userSessions.get(username);
        if (sessions == null) return;

        sessions.remove(handler);
        System.out.println("[PresenceManager] Removed session for '" + username + 
                           "'. Remaining sessions: " + sessions.size());

        if (sessions.isEmpty()) {
            userSessions.remove(username);
            System.out.println("[PresenceManager] Presence transition: " + username + " is now OFFLINE.");
            broadcastPresenceToRelevantContacts(username, userId, "OFFLINE");
        }
    }

    /**
     * Selectively broadcasts presence changes only to relevant users
     * (users who share an active conversation in MySQL with the target user).
     */
    private void broadcastPresenceToRelevantContacts(String username, long userId, String status) {
        List<String> contacts = messageRepository.findRelevantContactUsernames(userId);
        System.out.println("[PresenceManager] Broadcasting " + status + " for " + username + 
                           " to " + contacts.size() + " relevant contacts.");

        for (String contactUsername : contacts) {
            Set<ClientHandler> recipientSessions = userSessions.get(contactUsername);
            if (recipientSessions != null) {
                Message presenceMsg = new Message(MessageType.PRESENCE, username, contactUsername, status);
                for (ClientHandler session : recipientSessions) {
                    session.sendMessage(presenceMsg);
                }
            }
        }
    }

    /**
     * Checks if a user has at least one active connection session.
     */
    public boolean isUserOnline(String username) {
        Set<ClientHandler> sessions = userSessions.get(username);
        return sessions != null && !sessions.isEmpty();
    }

    /**
     * Retrieves all active sessions for a user (e.g. to deliver messages to all open devices).
     */
    public Set<ClientHandler> getSessions(String username) {
        Set<ClientHandler> sessions = userSessions.get(username);
        return sessions != null ? Collections.unmodifiableSet(sessions) : Collections.emptySet();
    }

    /**
     * Returns all currently online usernames.
     */
    public Set<String> getOnlineUsernames() {
        return Collections.unmodifiableSet(userSessions.keySet());
    }

    public int getSessionCount(String username) {
        Set<ClientHandler> sessions = userSessions.get(username);
        return sessions != null ? sessions.size() : 0;
    }
}
