package com.realtimechat.server;

import com.google.gson.JsonSyntaxException;
import com.realtimechat.model.Message;
import com.realtimechat.model.MessageType;
import com.realtimechat.model.User;
import com.realtimechat.service.AuthenticationService;
import com.realtimechat.service.MessageRouter;
import com.realtimechat.service.NotificationService;
import com.realtimechat.service.PresenceManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Handles network I/O for a connected client session.
 * Coordinates with AuthenticationService, MessageRouter, PresenceManager (multi-session),
 * and NotificationService (unread counts).
 */
public class ClientHandler implements Runnable {

    public static final int MAX_MESSAGE_SIZE = 4096;

    private final Socket socket;
    private final ClientManager clientManager;
    private final AuthenticationService authService;
    private final MessageRouter messageRouter;
    private final PresenceManager presenceManager;
    private final NotificationService notificationService;
    private final String connectionId;

    private BufferedReader reader;
    private PrintWriter writer;
    private User authenticatedUser = null;
    private volatile boolean isRunning = true;

    public ClientHandler(Socket socket, 
                         ClientManager clientManager, 
                         AuthenticationService authService,
                         MessageRouter messageRouter,
                         PresenceManager presenceManager,
                         NotificationService notificationService,
                         String connectionId) {
        this.socket = socket;
        this.clientManager = clientManager;
        this.authService = authService;
        this.messageRouter = messageRouter;
        this.presenceManager = presenceManager;
        this.notificationService = notificationService;
        this.connectionId = connectionId;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public String getUsername() {
        return authenticatedUser != null ? authenticatedUser.getUsername() : null;
    }

    public User getAuthenticatedUser() {
        return authenticatedUser;
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            // Send initial connection welcome
            sendMessage(new Message(MessageType.LOGIN, "SERVER", null, 
                    "Welcome! Please log in (LOGIN) or register an account (REGISTER)."));

            String rawLine;
            while (isRunning && (rawLine = reader.readLine()) != null) {
                if (rawLine.length() > MAX_MESSAGE_SIZE) {
                    sendMessage(Message.createError("Message exceeds maximum size limit of " + MAX_MESSAGE_SIZE + " bytes."));
                    continue;
                }

                String trimmed = rawLine.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                Message incoming;
                try {
                    incoming = Message.fromJson(trimmed);
                } catch (JsonSyntaxException e) {
                    sendMessage(Message.createError("Malformed JSON: " + e.getMessage()));
                    continue;
                }

                if (incoming == null || incoming.getType() == null) {
                    sendMessage(Message.createError("Missing message 'type' field."));
                    continue;
                }

                processMessage(incoming);
            }

        } catch (IOException e) {
            System.out.println("[ClientHandler] Connection closed for " + 
                               (getUsername() != null ? getUsername() : connectionId) + ": " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    private void processMessage(Message msg) {
        switch (msg.getType()) {
            case LOGIN:
                handleLogin(msg);
                break;

            case REGISTER:
                handleRegister(msg);
                break;

            case CHAT:
                handleChat(msg);
                break;

            case HISTORY:
                handleHistory(msg);
                break;

            case USER_LIST:
                handleUserList();
                break;

            case ERROR:
                System.err.println("[ClientHandler] Client sent error notice: " + msg.getContent());
                break;

            default:
                sendMessage(Message.createError("Unsupported message type: " + msg.getType()));
        }
    }

    private void handleLogin(Message msg) {
        String username = msg.getSender();
        String password = msg.getContent();

        if (username == null || password == null) {
            sendMessage(Message.createError("Login requires both username and password."));
            return;
        }

        User user = authService.login(username, password);
        if (user == null) {
            sendMessage(Message.createError("Authentication failed: Invalid username or password."));
            return;
        }

        this.authenticatedUser = user;
        clientManager.bindUsername(user.getUsername(), this);

        // Register session in PresenceManager (supports multiple devices/sessions!)
        presenceManager.registerSession(user.getUsername(), this, user.getId());

        sendMessage(new Message(MessageType.LOGIN, "SERVER", user.getUsername(), "Login successful!"));

        // Check for unread notification count
        notificationService.sendUnreadSummary(user, this);

        // Deliver queued offline messages
        messageRouter.deliverOfflineMessages(user, this);

        // Refresh user list
        clientManager.broadcastUserList();
    }

    private void handleRegister(Message msg) {
        String username = msg.getSender();
        String password = msg.getContent();

        try {
            User registered = authService.register(username, password);
            sendMessage(new Message(MessageType.REGISTER, "SERVER", registered.getUsername(), 
                    "Registration successful for '" + registered.getUsername() + "'. You may now log in."));
        } catch (Exception e) {
            sendMessage(Message.createError("Registration failed: " + e.getMessage()));
        }
    }

    private void handleChat(Message msg) {
        if (this.authenticatedUser == null) {
            sendMessage(Message.createError("Unauthorized: You must log in before sending messages."));
            return;
        }

        if (msg.getContent() == null || msg.getContent().trim().isEmpty()) {
            sendMessage(Message.createError("Message content cannot be blank."));
            return;
        }

        // CRITICAL SECURITY ENFORCEMENT:
        // Always override sender with server-authenticated identity
        msg.setSender(this.authenticatedUser.getUsername());
        msg.setTimestamp(System.currentTimeMillis());

        String recipient = msg.getRecipient();
        if (recipient != null && !recipient.trim().isEmpty() && !recipient.equalsIgnoreCase("ALL")) {
            // Private 1-to-1 message routed via MessageRouter
            Message status = messageRouter.routeDirectMessage(this.authenticatedUser, recipient.trim(), msg.getContent());
            sendMessage(status);
        } else {
            // Public room message
            clientManager.broadcast(msg, this);
        }
    }

    private void handleHistory(Message msg) {
        if (this.authenticatedUser == null) {
            sendMessage(Message.createError("Unauthorized: You must log in to view chat history."));
            return;
        }

        Message historyResponse;
        if (msg.getConversationId() != null) {
            historyResponse = messageRouter.fetchHistory(this.authenticatedUser, msg.getConversationId(), 50);
        } else if (msg.getContent() != null && !msg.getContent().trim().isEmpty()) {
            historyResponse = messageRouter.fetchDirectHistoryWithUser(this.authenticatedUser, msg.getContent().trim(), 50);
        } else {
            historyResponse = Message.createError("History request requires conversationId or target username.");
        }

        sendMessage(historyResponse);
    }

    private void handleUserList() {
        String csv = String.join(",", presenceManager.getOnlineUsernames());
        sendMessage(new Message(MessageType.USER_LIST, "SERVER", getUsername(), csv));
    }

    public synchronized void sendMessage(Message message) {
        if (writer != null && message != null) {
            writer.println(message.toJson());
        }
    }

    private void disconnect() {
        isRunning = false;
        closeResources();
        if (authenticatedUser != null) {
            // Unregister session in PresenceManager (multi-session aware)
            presenceManager.unregisterSession(authenticatedUser.getUsername(), this, authenticatedUser.getId());
            clientManager.removeClient(this);
        }
    }

    public void closeResources() {
        try {
            if (reader != null) reader.close();
        } catch (IOException ignored) {}

        if (writer != null) {
            writer.close();
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}
    }
}
