package com.realtimechat.client;

import com.google.gson.JsonSyntaxException;
import com.realtimechat.model.Message;

import javax.swing.SwingUtilities;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Service managing client socket networking in the background for Swing GUI.
 * Reads messages on a dedicated background thread and dispatches updates
 * to UI callbacks using SwingUtilities.invokeLater to guarantee thread safety.
 */
public class ClientNetworkService {

    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private volatile boolean isConnected = false;
    private String currentUsername;

    private final List<Consumer<Message>> messageListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<Boolean>> connectionListeners = new CopyOnWriteArrayList<>();

    public ClientNetworkService(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public synchronized boolean connect() {
        if (isConnected) return true;

        try {
            // Establish secure TLS connection using SSLSocket and client.truststore
            socket = com.realtimechat.service.SslContextFactory.createSSLSocket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            isConnected = true;

            // Notify UI that connection succeeded
            notifyConnectionStatus(true);

            // Start background worker thread for incoming network reads
            Thread readThread = new Thread(this::listenForServerMessages, "NetworkListenerThread");
            readThread.setDaemon(true);
            readThread.start();
            return true;

        } catch (Exception e) {
            System.err.println("[ClientNetworkService] TLS Connection failed: " + e.getMessage());
            notifyConnectionStatus(false);
            return false;
        }
    }

    private void listenForServerMessages() {
        try {
            String rawLine;
            while (isConnected && (rawLine = in.readLine()) != null) {
                try {
                    Message message = Message.fromJson(rawLine);
                    if (message != null) {
                        // CRITICAL: Always dispatch UI updates to Swing Event Dispatch Thread (EDT)
                        SwingUtilities.invokeLater(() -> {
                            for (Consumer<Message> listener : messageListeners) {
                                listener.accept(message);
                            }
                        });
                    }
                } catch (JsonSyntaxException e) {
                    System.err.println("[ClientNetworkService] Malformed packet received: " + rawLine);
                }
            }
        } catch (IOException e) {
            System.out.println("[ClientNetworkService] Socket connection lost.");
        } finally {
            disconnect();
        }
    }

    public synchronized void sendMessage(Message message) {
        if (out != null && message != null) {
            out.println(message.toJson());
        }
    }

    public void addMessageListener(Consumer<Message> listener) {
        messageListeners.add(listener);
    }

    public void addConnectionListener(Consumer<Boolean> listener) {
        connectionListeners.add(listener);
    }

    private void notifyConnectionStatus(boolean connected) {
        SwingUtilities.invokeLater(() -> {
            for (Consumer<Boolean> listener : connectionListeners) {
                listener.accept(connected);
            }
        });
    }

    public synchronized void disconnect() {
        isConnected = false;
        notifyConnectionStatus(false);

        try {
            if (in != null) in.close();
        } catch (IOException ignored) {}

        if (out != null) out.close();

        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    public boolean isConnected() {
        return isConnected;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public void setCurrentUsername(String username) {
        this.currentUsername = username;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
