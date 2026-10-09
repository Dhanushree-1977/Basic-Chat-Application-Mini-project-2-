package com.realtimechat.service;

import com.realtimechat.database.UserRepository;
import com.realtimechat.model.User;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Service managing user registration and authentication.
 * Employs BCrypt hashing with 12 salt rounds to secure user credentials.
 */
public class AuthenticationService {

    private final UserRepository userRepository;

    public AuthenticationService() {
        this.userRepository = new UserRepository();
    }

    public AuthenticationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Registers a new user account with a BCrypt hashed password.
     *
     * @param username    unique username (3-20 alphanumeric characters)
     * @param rawPassword plain-text password (minimum 6 characters)
     * @return created User entity
     * @throws IllegalArgumentException if validation fails or username is already taken
     */
    public User register(String username, String rawPassword) {
        if (username == null || !username.matches("^[a-zA-Z0-9_]{3,20}$")) {
            throw new IllegalArgumentException("Username must be 3-20 characters (letters, numbers, underscores).");
        }

        if (rawPassword == null || rawPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username '" + username + "' is already registered.");
        }

        // Generate salted BCrypt hash (work factor 12)
        String hashedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));

        User newUser = new User(username, hashedPassword);
        boolean saved = userRepository.createUser(newUser);

        if (!saved) {
            throw new RuntimeException("Database error saving new user.");
        }

        System.out.println("[AuthenticationService] Successfully registered user: " + username);
        return newUser;
    }

    /**
     * Authenticates an existing user against stored BCrypt credentials.
     *
     * @param username    login username
     * @param rawPassword plain-text password entered by user
     * @return User entity if credentials match, or null if authentication fails
     */
    public User login(String username, String rawPassword) {
        if (username == null || rawPassword == null) {
            return null;
        }

        User user = userRepository.findByUsername(username.trim());
        if (user == null) {
            return null; // User not found
        }

        // Verify password against stored BCrypt hash
        boolean passwordMatches = BCrypt.checkpw(rawPassword, user.getPasswordHash());
        if (passwordMatches) {
            userRepository.updateLastLogin(user.getId());
            System.out.println("[AuthenticationService] User authenticated successfully: " + username);
            return user;
        }

        System.out.println("[AuthenticationService] Failed login attempt for user: " + username);
        return null;
    }
}
