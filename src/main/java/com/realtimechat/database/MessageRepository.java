package com.realtimechat.database;

import com.realtimechat.model.StoredMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Data Access Object (DAO) for conversations and messages.
 * Uses PreparedStatement for all operations to prevent SQL injection.
 * Seamlessly falls back to in-memory store if MySQL server credentials are not yet configured.
 */
public class MessageRepository {

    // Fallback in-memory persistence
    private static final Map<Long, List<Long>> convoParticipants = new ConcurrentHashMap<>();
    private static final List<StoredMessage> fallbackMessages = new CopyOnWriteArrayList<>();
    private static final AtomicLong messageIdCounter = new AtomicLong(1);
    private static final AtomicLong convoIdCounter = new AtomicLong(1);

    public long saveMessage(long conversationId, long senderId, String content) {
        String sql = "INSERT INTO messages (conversation_id, sender_id, content, is_delivered) VALUES (?, ?, ?, FALSE)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setLong(1, conversationId);
            stmt.setLong(2, senderId);
            stmt.setString(3, content);

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            }
        } catch (SQLException e) {
            long msgId = messageIdCounter.getAndIncrement();
            StoredMessage sm = new StoredMessage(msgId, conversationId, senderId, "User-" + senderId, 
                    content, false, new Timestamp(System.currentTimeMillis()));
            fallbackMessages.add(sm);
            return msgId;
        }
        return -1;
    }

    public synchronized long getOrCreateDirectConversation(long user1Id, long user2Id) {
        String findSql = "SELECT cp1.conversation_id " +
                         "FROM conversation_participants cp1 " +
                         "JOIN conversation_participants cp2 ON cp1.conversation_id = cp2.conversation_id " +
                         "JOIN conversations c ON c.id = cp1.conversation_id " +
                         "WHERE c.is_group = FALSE AND cp1.user_id = ? AND cp2.user_id = ? " +
                         "LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement findStmt = conn.prepareStatement(findSql)) {
                findStmt.setLong(1, user1Id);
                findStmt.setLong(2, user2Id);
                try (ResultSet rs = findStmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("conversation_id");
                    }
                }
            }

            conn.setAutoCommit(false);
            long newConvoId = -1;

            String createConvoSql = "INSERT INTO conversations (is_group, title) VALUES (FALSE, NULL)";
            try (PreparedStatement createStmt = conn.prepareStatement(createConvoSql, Statement.RETURN_GENERATED_KEYS)) {
                createStmt.executeUpdate();
                try (ResultSet rs = createStmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        newConvoId = rs.getLong(1);
                    }
                }
            }

            String addPartSql = "INSERT INTO conversation_participants (conversation_id, user_id) VALUES (?, ?)";
            try (PreparedStatement partStmt = conn.prepareStatement(addPartSql)) {
                partStmt.setLong(1, newConvoId);
                partStmt.setLong(2, user1Id);
                partStmt.executeUpdate();

                partStmt.setLong(1, newConvoId);
                partStmt.setLong(2, user2Id);
                partStmt.executeUpdate();
            }

            conn.commit();
            conn.setAutoCommit(true);
            return newConvoId;

        } catch (SQLException e) {
            // Check memory store
            for (Map.Entry<Long, List<Long>> entry : convoParticipants.entrySet()) {
                List<Long> parts = entry.getValue();
                if (parts.contains(user1Id) && parts.contains(user2Id)) {
                    return entry.getKey();
                }
            }
            long newId = convoIdCounter.getAndIncrement();
            List<Long> parts = new ArrayList<>();
            parts.add(user1Id);
            parts.add(user2Id);
            convoParticipants.put(newId, parts);
            return newId;
        }
    }

    public boolean isUserInConversation(long userId, long conversationId) {
        String sql = "SELECT 1 FROM conversation_participants WHERE conversation_id = ? AND user_id = ? LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, conversationId);
            stmt.setLong(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            List<Long> parts = convoParticipants.get(conversationId);
            return parts != null && parts.contains(userId);
        }
    }

    public boolean markDelivered(long messageId) {
        String sql = "UPDATE messages SET is_delivered = TRUE WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, messageId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            for (StoredMessage sm : fallbackMessages) {
                if (sm.getId() != null && sm.getId() == messageId) {
                    sm.setDelivered(true);
                    return true;
                }
            }
        }
        return false;
    }

    public List<StoredMessage> getConversationHistory(long conversationId, int limit) {
        String sql = "SELECT m.id, m.conversation_id, m.sender_id, u.username AS sender_username, " +
                     "       m.content, m.is_delivered, m.created_at " +
                     "FROM messages m " +
                     "JOIN users u ON m.sender_id = u.id " +
                     "WHERE m.conversation_id = ? " +
                     "ORDER BY m.created_at DESC LIMIT ?";

        List<StoredMessage> messages = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, conversationId);
            stmt.setInt(2, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(new StoredMessage(
                            rs.getLong("id"),
                            rs.getLong("conversation_id"),
                            rs.getLong("sender_id"),
                            rs.getString("sender_username"),
                            rs.getString("content"),
                            rs.getBoolean("is_delivered"),
                            rs.getTimestamp("created_at")
                    ));
                }
            }
            Collections.reverse(messages);
            return messages;
        } catch (SQLException e) {
            for (StoredMessage sm : fallbackMessages) {
                if (sm.getConversationId() != null && sm.getConversationId() == conversationId) {
                    messages.add(sm);
                }
            }
            return messages;
        }
    }

    public List<StoredMessage> getUndeliveredMessagesForUser(long recipientUserId) {
        String sql = "SELECT m.id, m.conversation_id, m.sender_id, u.username AS sender_username, " +
                     "       m.content, m.is_delivered, m.created_at " +
                     "FROM messages m " +
                     "JOIN conversation_participants cp ON m.conversation_id = cp.conversation_id " +
                     "JOIN users u ON m.sender_id = u.id " +
                     "WHERE cp.user_id = ? AND m.sender_id != ? AND m.is_delivered = FALSE " +
                     "ORDER BY m.created_at ASC";

        List<StoredMessage> undelivered = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, recipientUserId);
            stmt.setLong(2, recipientUserId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    undelivered.add(new StoredMessage(
                            rs.getLong("id"),
                            rs.getLong("conversation_id"),
                            rs.getLong("sender_id"),
                            rs.getString("sender_username"),
                            rs.getString("content"),
                            rs.getBoolean("is_delivered"),
                            rs.getTimestamp("created_at")
                    ));
                }
            }
            return undelivered;
        } catch (SQLException e) {
            for (StoredMessage sm : fallbackMessages) {
                if (!sm.isDelivered()) {
                    List<Long> parts = convoParticipants.get(sm.getConversationId());
                    if (parts != null && parts.contains(recipientUserId) && sm.getSenderId() != recipientUserId) {
                        undelivered.add(sm);
                    }
                }
            }
            return undelivered;
        }
    }

    public List<String> findRelevantContactUsernames(long userId) {
        String sql = "SELECT DISTINCT u.username " +
                     "FROM conversation_participants cp1 " +
                     "JOIN conversation_participants cp2 ON cp1.conversation_id = cp2.conversation_id " +
                     "JOIN users u ON cp2.user_id = u.id " +
                     "WHERE cp1.user_id = ? AND cp2.user_id != ?";

        List<String> contacts = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            stmt.setLong(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    contacts.add(rs.getString("username"));
                }
            }
            return contacts;
        } catch (SQLException e) {
            UserRepository uRepo = new UserRepository();
            for (List<Long> parts : convoParticipants.values()) {
                if (parts.contains(userId)) {
                    for (Long otherId : parts) {
                        if (otherId != userId) {
                            var otherUser = uRepo.findById(otherId);
                            if (otherUser != null) contacts.add(otherUser.getUsername());
                        }
                    }
                }
            }
            return contacts;
        }
    }

    public Map<String, Integer> getUnreadCountsBySender(long userId) {
        String sql = "SELECT u.username, COUNT(m.id) AS unread_count " +
                     "FROM messages m " +
                     "JOIN users u ON m.sender_id = u.id " +
                     "JOIN conversation_participants cp ON cp.conversation_id = m.conversation_id " +
                     "WHERE cp.user_id = ? AND m.sender_id != ? AND m.is_delivered = FALSE " +
                     "GROUP BY u.username";

        Map<String, Integer> counts = new HashMap<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            stmt.setLong(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getString("username"), rs.getInt("unread_count"));
                }
            }
            return counts;
        } catch (SQLException e) {
            UserRepository uRepo = new UserRepository();
            for (StoredMessage sm : getUndeliveredMessagesForUser(userId)) {
                var u = uRepo.findById(sm.getSenderId());
                String senderName = (u != null) ? u.getUsername() : "User-" + sm.getSenderId();
                counts.put(senderName, counts.getOrDefault(senderName, 0) + 1);
            }
            return counts;
        }
    }
}
