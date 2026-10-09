package com.realtimechat.client;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.realtimechat.model.Message;
import com.realtimechat.model.MessageType;
import com.realtimechat.model.StoredMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

/**
 * Console-based client supporting user registration, authentication,
 * private one-to-one messaging, and chat history retrieval.
 */
public class ChatClient {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5000;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Gson GSON = new Gson();

    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String currentUsername;
    private volatile boolean isLoggedIn = false;
    private volatile boolean isRunning = true;

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        System.out.println("==================================================");
        System.out.println("   Real-Time Chat Client (MySQL + BCrypt Auth)");
        System.out.println("   Connecting to " + host + ":" + port + "...");
        System.out.println("==================================================");

        try {
            // Establish secure TLS connection using SSLSocket and client.truststore
            socket = com.realtimechat.service.SslContextFactory.createSSLSocket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            // 1. Start incoming message listener
            Thread listenerThread = new Thread(this::listenForServerMessages, "IncomingListener");
            listenerThread.setDaemon(true);
            listenerThread.start();

            // 2. Guide user through Login or Registration
            Scanner scanner = new Scanner(System.in);
            handleAuthenticationMenu(scanner);

            if (!isRunning) {
                return;
            }

            printHelp();

            // 3. Main interactive command loop
            while (isRunning) {
                if (!scanner.hasNextLine()) break;
                String line = scanner.nextLine().trim();

                if (line.isEmpty()) continue;

                if (line.equalsIgnoreCase("/quit") || line.equalsIgnoreCase("exit")) {
                    System.out.println("[Client] Quitting...");
                    break;
                }

                handleCommand(line);
            }

        } catch (Exception e) {
            System.err.println("[Client] TLS Connection failure: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    private void handleAuthenticationMenu(Scanner scanner) {
        while (isRunning && !isLoggedIn) {
            System.out.println("\nSelect an option:");
            System.out.println("  [1] Login");
            System.out.println("  [2] Register new account");
            System.out.println("  [3] Exit");
            System.out.print("Choice: ");

            if (!scanner.hasNextLine()) break;
            String choice = scanner.nextLine().trim();

            if ("1".equals(choice)) {
                System.out.print("Username: ");
                String user = scanner.nextLine().trim();
                System.out.print("Password: ");
                String pass = scanner.nextLine().trim();

                this.currentUsername = user;
                sendJsonMessage(new Message(MessageType.LOGIN, user, null, pass));
                waitForAuthResponse();

            } else if ("2".equals(choice)) {
                System.out.print("Choose Username (3-20 characters): ");
                String user = scanner.nextLine().trim();
                System.out.print("Choose Password (min 6 characters): ");
                String pass = scanner.nextLine().trim();

                sendJsonMessage(new Message(MessageType.REGISTER, user, null, pass));
                waitForAuthResponse();

            } else if ("3".equals(choice)) {
                isRunning = false;
                break;
            } else {
                System.out.println("Invalid option. Please enter 1, 2, or 3.");
            }
        }
    }

    private void waitForAuthResponse() {
        try {
            Thread.sleep(600); // Wait briefly for server feedback
        } catch (InterruptedException ignored) {}
    }

    private void handleCommand(String line) {
        if (line.startsWith("/msg ")) {
            // Private messaging: /msg <username> <content>
            String[] parts = line.split(" ", 3);
            if (parts.length < 3 || parts[2].trim().isEmpty()) {
                System.out.println("[Usage] /msg <targetUser> <message>");
            } else {
                String target = parts[1].trim();
                String content = parts[2].trim();
                sendJsonMessage(new Message(MessageType.CHAT, currentUsername, target, content));
            }
        } else if (line.startsWith("/history ")) {
            // History request: /history <targetUser or conversationId>
            String target = line.substring(9).trim();
            if (target.isEmpty()) {
                System.out.println("[Usage] /history <targetUsername> OR /history <conversationId>");
            } else {
                try {
                    long convoId = Long.parseLong(target);
                    sendJsonMessage(Message.createHistoryRequest(currentUsername, convoId));
                } catch (NumberFormatException e) {
                    // Send with username in content
                    Message historyReq = new Message(MessageType.HISTORY, currentUsername, null, target);
                    sendJsonMessage(historyReq);
                }
            }
        } else if (line.equalsIgnoreCase("/users")) {
            sendJsonMessage(new Message(MessageType.USER_LIST, currentUsername, null, ""));
        } else if (line.equalsIgnoreCase("/help")) {
            printHelp();
        } else {
            // Public room broadcast
            sendJsonMessage(new Message(MessageType.CHAT, currentUsername, null, line));
        }
    }

    private void sendJsonMessage(Message message) {
        if (out != null && message != null) {
            out.println(message.toJson());
        }
    }

    private void listenForServerMessages() {
        try {
            String line;
            while (isRunning && (line = in.readLine()) != null) {
                try {
                    Message msg = Message.fromJson(line);
                    if (msg != null && msg.getType() != null) {
                        handleIncomingMessage(msg);
                    }
                } catch (JsonSyntaxException e) {
                    System.err.println("[Client] Malformed packet: " + line);
                }
            }
        } catch (IOException e) {
            if (isRunning) {
                System.out.println("\n[Client] Disconnected from server.");
            }
        } finally {
            isRunning = false;
        }
    }

    private void handleIncomingMessage(Message msg) {
        String timeStr = Instant.ofEpochMilli(msg.getTimestamp())
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
                .format(TIME_FORMATTER);

        switch (msg.getType()) {
            case LOGIN:
                System.out.println("[Auth]: " + msg.getContent());
                if (msg.getContent() != null && msg.getContent().toLowerCase().contains("successful")) {
                    this.isLoggedIn = true;
                }
                break;

            case REGISTER:
                System.out.println("[Registration]: " + msg.getContent());
                break;

            case CHAT:
                if (msg.getRecipient() != null && !msg.getRecipient().equalsIgnoreCase("ALL")) {
                    System.out.println("[" + timeStr + "] [Private from " + msg.getSender() + "]: " + msg.getContent());
                } else if ("SERVER".equals(msg.getSender())) {
                    System.out.println("[" + timeStr + "] [Server]: " + msg.getContent());
                } else {
                    System.out.println("[" + timeStr + "] [" + msg.getSender() + "]: " + msg.getContent());
                }
                break;

            case HISTORY:
                displayHistory(msg);
                break;

            case USER_LIST:
                System.out.println("--------------------------------------------------");
                System.out.println("  Online Users: [" + (msg.getContent() != null ? msg.getContent() : "") + "]");
                System.out.println("--------------------------------------------------");
                break;

            case ERROR:
                System.err.println("[SERVER ERROR]: " + msg.getContent());
                break;

            default:
                System.out.println("[Notice]: " + msg.getContent());
        }
    }

    private void displayHistory(Message msg) {
        System.out.println("\n==================================================");
        System.out.println("   Conversation History (Conversation #" + msg.getConversationId() + ")");
        System.out.println("==================================================");

        try {
            Type listType = new TypeToken<List<StoredMessage>>() {}.getType();
            List<StoredMessage> history = GSON.fromJson(msg.getContent(), listType);

            if (history == null || history.isEmpty()) {
                System.out.println("  (No messages found in this conversation yet)");
            } else {
                for (StoredMessage sm : history) {
                    String time = sm.getCreatedAt() != null ? sm.getCreatedAt().toString() : "";
                    System.out.println("  [" + time + "] " + sm.getSenderUsername() + ": " + sm.getContent());
                }
            }
        } catch (Exception e) {
            System.out.println("  History data: " + msg.getContent());
        }
        System.out.println("==================================================\n");
    }

    private void printHelp() {
        System.out.println("--------------------------------------------------");
        System.out.println("Commands:");
        System.out.println("  <message>                 -> Public broadcast");
        System.out.println("  /msg <username> <text>    -> Private 1-to-1 message");
        System.out.println("  /history <username>       -> View chat history with user");
        System.out.println("  /history <convoId>        -> View history by conversation ID");
        System.out.println("  /users                    -> List online users");
        System.out.println("  /help                     -> Show menu");
        System.out.println("  /quit                     -> Exit application");
        System.out.println("--------------------------------------------------");
    }

    private void disconnect() {
        isRunning = false;
        try {
            if (in != null) in.close();
        } catch (IOException ignored) {}

        if (out != null) {
            out.close();
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}

        System.out.println("[Client] Application exited.");
    }

    public static void main(String[] args) {
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;

        if (args.length >= 1) host = args[0];
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {}
        }

        ChatClient client = new ChatClient(host, port);
        client.start();
    }
}
