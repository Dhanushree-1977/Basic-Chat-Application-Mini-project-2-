package com.realtimechat.server;

import com.realtimechat.database.MessageRepository;
import com.realtimechat.database.UserRepository;
import com.realtimechat.service.AuthenticationService;
import com.realtimechat.service.MessageRouter;
import com.realtimechat.service.NotificationService;
import com.realtimechat.service.PresenceManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main TCP Chat Server listening on port 5000.
 * Coordinates multithreaded client workers, MySQL persistence, PresenceManager,
 * and NotificationService.
 */
public class ChatServer {

    public static final int PORT = 5000;

    private final ClientManager clientManager;
    private final AuthenticationService authService;
    private final MessageRouter messageRouter;
    private final PresenceManager presenceManager;
    private final NotificationService notificationService;
    private final ExecutorService threadPool;
    private final AtomicInteger connectionCounter;
    private volatile boolean isRunning = true;
    private ServerSocket serverSocket;

    public ChatServer() {
        this.clientManager = new ClientManager();

        UserRepository userRepository = new UserRepository();
        MessageRepository messageRepository = new MessageRepository();

        this.presenceManager = new PresenceManager(messageRepository);
        this.notificationService = new NotificationService(messageRepository);
        this.authService = new AuthenticationService(userRepository);
        this.messageRouter = new MessageRouter(userRepository, messageRepository, presenceManager);

        this.threadPool = Executors.newCachedThreadPool();
        this.connectionCounter = new AtomicInteger(1);
    }

    /**
     * Starts listening for client connections on PORT 5000.
     */
    public void start() {
        System.out.println("==================================================");
        System.out.println("   Real-Time Chat Server Starting on Port " + PORT);
        System.out.println("   Presence: Multi-Session | Notifications: Active");
        System.out.println("==================================================");

        try {
            // Initialize secure SSLServerSocket using TLSv1.3 and self-signed PKCS12 keystore
            serverSocket = com.realtimechat.service.SslContextFactory.createSSLServerSocket(PORT);
            System.out.println("[ChatServer] TLS Encrypted Server bound successfully on port " + PORT + ".");
            System.out.println("[ChatServer] Listening for secure SSLSocket client connections...");

            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    String connectionId = "Conn-" + connectionCounter.getAndIncrement();

                    System.out.println("[ChatServer] Accepted connection from " + 
                                       clientSocket.getRemoteSocketAddress() + " (" + connectionId + ")");

                    ClientHandler handler = new ClientHandler(
                            clientSocket, 
                            clientManager, 
                            authService, 
                            messageRouter,
                            presenceManager,
                            notificationService,
                            connectionId
                    );
                    clientManager.addClient(handler);

                    threadPool.submit(handler);

                } catch (IOException e) {
                    if (!isRunning) {
                        System.out.println("[ChatServer] Server socket closed. Exiting accept loop.");
                        break;
                    }
                    System.err.println("[ChatServer] Error accepting client connection: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("[ChatServer] Could not initialize TLS server on port " + PORT + ": " + e.getMessage());
        } finally {
            stop();
        }
    }

    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("[ChatServer] Error closing server socket: " + e.getMessage());
        }
        threadPool.shutdown();
        System.out.println("[ChatServer] Server stopped.");
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        server.start();
    }
}
