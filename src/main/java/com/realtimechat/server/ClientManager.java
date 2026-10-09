package com.realtimechat.server;

import com.realtimechat.model.Message;
import com.realtimechat.model.MessageType;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages connected clients and dispatches Message objects.
 * Maintains thread-safe collections for both unauthenticated connections and logged-in users.
 */
public class ClientManager {

    // All active client sockets currently connected
    private final List<ClientHandler> allClients = new CopyOnWriteArrayList<>();

    // Logged-in clients keyed by username (case-insensitive for uniqueness)
    private final Map<String, ClientHandler> clientsByUsername = new ConcurrentHashMap<>();

    /**
     * Adds an incoming client connection.
     */
    public void addClient(ClientHandler client) {
        allClients.add(client);
        System.out.println("[ClientManager] New socket connection from " + client.getConnectionId());
    }

    /**
     * Binds a username to a client connection upon successful login.
     *
     * @param username requested username
     * @param client   handler associated with the user
     * @return true if successfully bound, false if username is already taken
     */
    public synchronized boolean bindUsername(String username, ClientHandler client) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        String normalized = username.trim();
        for (String existingUser : clientsByUsername.keySet()) {
            if (existingUser.equalsIgnoreCase(normalized)) {
                return false; // Username already in use
            }
        }

        clientsByUsername.put(normalized, client);
        System.out.println("[ClientManager] User logged in: " + normalized + 
                           ". Total authenticated users: " + clientsByUsername.size());
        return true;
    }

    /**
     * Removes a client upon disconnection and frees their username.
     */
    public synchronized void removeClient(ClientHandler client) {
        allClients.remove(client);
        if (client.getUsername() != null) {
            clientsByUsername.remove(client.getUsername());
            System.out.println("[ClientManager] User logged out: " + client.getUsername());
            broadcastUserList();
        }
    }

    /**
     * Broadcasts a message to all connected clients EXCEPT the sender.
     */
    public void broadcast(Message message, ClientHandler sender) {
        for (ClientHandler client : allClients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    /**
     * Broadcasts a message to ALL connected clients.
     */
    public void broadcastAll(Message message) {
        for (ClientHandler client : allClients) {
            client.sendMessage(message);
        }
    }

    /**
     * Retrieves the active ClientHandler for an online user.
     */
    public ClientHandler getClientByUsername(String username) {
        if (username == null) return null;
        return clientsByUsername.get(username.trim());
    }

    /**
     * Sends a message to a specific recipient user.
     *
     * @param recipientUsername destination username
     * @param message           the message to send
     * @return true if recipient is currently online and message was sent
     */
    public boolean sendDirect(String recipientUsername, Message message) {
        ClientHandler target = getClientByUsername(recipientUsername);
        if (target != null) {
            target.sendMessage(message);
            return true;
        }
        return false;
    }

    /**
     * Broadcasts an updated list of online usernames to all clients.
     */
    public void broadcastUserList() {
        Set<String> users = clientsByUsername.keySet();
        String csvList = String.join(",", users);
        Message userListMessage = new Message(MessageType.USER_LIST, "SERVER", null, csvList);
        broadcastAll(userListMessage);
    }

    /**
     * Returns an unmodifiable set of currently online usernames.
     */
    public Set<String> getOnlineUsernames() {
        return Collections.unmodifiableSet(clientsByUsername.keySet());
    }

    public int getActiveConnectionCount() {
        return allClients.size();
    }
}
