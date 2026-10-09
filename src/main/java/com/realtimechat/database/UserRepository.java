package com.realtimechat.database;

import com.realtimechat.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Data Access Object (DAO) for the 'users' table.
 * All database operations strictly use PreparedStatement to eliminate SQL injection vulnerabilities.
 * Seamlessly includes in-memory fallback for local demonstration if MySQL credentials are not yet configured.
 */
public class UserRepository {

    // Fallback store in case database is offline or not yet configured
    private static final Map<String, User> fallbackUsers = new ConcurrentHashMap<>();
    private static final AtomicLong fallbackIdCounter = new AtomicLong(1);

    /**
     * Inserts a new user record into the database.
     *
     * @param user user entity containing username and BCrypt-hashed password
     * @return true if inserted successfully, false otherwise
     */
    public boolean createUser(User user) {
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPasswordHash());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setId(generatedKeys.getLong(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[UserRepository] MySQL unavailable (" + e.getMessage() + "), saving to memory store.");
            if (fallbackUsers.containsKey(user.getUsername().toLowerCase())) {
                return false;
            }
            user.setId(fallbackIdCounter.getAndIncrement());
            user.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            fallbackUsers.put(user.getUsername().toLowerCase(), user);
            return true;
        }
        return false;
    }

    /**
     * Finds a user record by username.
     *
     * @param username target username
     * @return User object if found, or null
     */
    public User findByUsername(String username) {
        if (username == null) return null;
        String sql = "SELECT id, username, password_hash, created_at, last_login FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        } catch (SQLException e) {
            return fallbackUsers.get(username.toLowerCase());
        }
        return fallbackUsers.get(username.toLowerCase());
    }

    /**
     * Finds a user record by primary key ID.
     *
     * @param id target user ID
     * @return User object if found, or null
     */
    public User findById(long id) {
        String sql = "SELECT id, username, password_hash, created_at, last_login FROM users WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        } catch (SQLException e) {
            for (User u : fallbackUsers.values()) {
                if (u.getId() != null && u.getId() == id) return u;
            }
        }
        return null;
    }

    /**
     * Checks if a username already exists in the database.
     */
    public boolean existsByUsername(String username) {
        if (username == null) return false;
        String sql = "SELECT 1 FROM users WHERE username = ? LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return fallbackUsers.containsKey(username.toLowerCase());
        }
    }

    /**
     * Updates the last_login timestamp for a user.
     */
    public boolean updateLastLogin(long userId) {
        String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            for (User u : fallbackUsers.values()) {
                if (u.getId() != null && u.getId() == userId) {
                    u.setLastLogin(new Timestamp(System.currentTimeMillis()));
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Retrieves all registered usernames in alphabetical order.
     */
    public List<String> findAllUsernames() {
        List<String> usernames = new ArrayList<>();
        String sql = "SELECT username FROM users ORDER BY username ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                usernames.add(rs.getString("username"));
            }
            return usernames;
        } catch (SQLException e) {
            usernames.addAll(fallbackUsers.keySet());
            return usernames;
        }
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getTimestamp("created_at"),
                rs.getTimestamp("last_login")
        );
    }
}
