package com.realtimechat.service;

import com.realtimechat.database.UserRepository;
import com.realtimechat.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying BCrypt authentication, password hashing,
 * and user registration validation rules.
 */
public class AuthenticationServiceTest {

    private AuthenticationService authService;
    private InMemoryUserRepository fakeRepository;

    @BeforeEach
    public void setUp() {
        fakeRepository = new InMemoryUserRepository();
        authService = new AuthenticationService(fakeRepository);
    }

    @Test
    @DisplayName("Verify user registration hashes plain-text password using BCrypt")
    public void testRegistrationHashesPasswordWithBCrypt() {
        User user = authService.register("john_doe", "mySecret123");

        assertNotNull(user);
        assertEquals("john_doe", user.getUsername());
        assertNotNull(user.getPasswordHash());

        // Password must NEVER be stored in plain text
        assertNotEquals("mySecret123", user.getPasswordHash());

        // BCrypt hashes begin with $2a$ or $2b$ and are 60 characters in length
        assertTrue(user.getPasswordHash().startsWith("$2a$12$") || user.getPasswordHash().startsWith("$2b$12$"),
                "Hash should use BCrypt work factor 12 format");
        assertEquals(60, user.getPasswordHash().length());
    }

    @Test
    @DisplayName("Verify duplicate username registration is rejected")
    public void testDuplicateUsernameRejected() {
        authService.register("alice", "password123");

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.register("alice", "anotherPassword456");
        });

        assertTrue(exception.getMessage().contains("already registered"));
    }

    @Test
    @DisplayName("Verify username validation constraints (length and characters)")
    public void testInvalidUsernameConstraints() {
        // Less than 3 characters
        assertThrows(IllegalArgumentException.class, () -> authService.register("ab", "validPass123"));

        // Greater than 20 characters
        assertThrows(IllegalArgumentException.class, () -> authService.register("this_is_a_very_long_username_exceeding_limit", "validPass123"));

        // Invalid characters (spaces or symbols)
        assertThrows(IllegalArgumentException.class, () -> authService.register("john doe", "validPass123"));
        assertThrows(IllegalArgumentException.class, () -> authService.register("john@home!", "validPass123"));
    }

    @Test
    @DisplayName("Verify password minimum length requirement (at least 6 characters)")
    public void testShortPasswordRejected() {
        assertThrows(IllegalArgumentException.class, () -> authService.register("validUser", "12345"));
    }

    @Test
    @DisplayName("Verify successful login with valid credentials")
    public void testSuccessfulLogin() {
        authService.register("sam_smith", "strongPassword99");

        User loggedIn = authService.login("sam_smith", "strongPassword99");
        assertNotNull(loggedIn, "Login should succeed with matching password");
        assertEquals("sam_smith", loggedIn.getUsername());
    }

    @Test
    @DisplayName("Verify failed login with incorrect password")
    public void testFailedLoginWrongPassword() {
        authService.register("sam_smith", "strongPassword99");

        User result = authService.login("sam_smith", "wrongPasswordHere");
        assertNull(result, "Login must fail with incorrect password");
    }

    @Test
    @DisplayName("Verify failed login for non-existent user")
    public void testFailedLoginNonExistentUser() {
        User result = authService.login("nobody", "somePassword");
        assertNull(result, "Login must return null for unregistered user");
    }

    /**
     * Test Double (InMemory Stub) of UserRepository.
     * Allows fast, deterministic unit testing without requiring an active MySQL instance.
     */
    private static class InMemoryUserRepository extends UserRepository {
        private final Map<String, User> storage = new HashMap<>();
        private long idCounter = 1;

        @Override
        public boolean createUser(User user) {
            if (storage.containsKey(user.getUsername().toLowerCase())) {
                return false;
            }
            user.setId(idCounter++);
            storage.put(user.getUsername().toLowerCase(), user);
            return true;
        }

        @Override
        public User findByUsername(String username) {
            return storage.get(username.toLowerCase());
        }

        @Override
        public boolean existsByUsername(String username) {
            return storage.containsKey(username.toLowerCase());
        }

        @Override
        public boolean updateLastLogin(long userId) {
            return true;
        }
    }
}
