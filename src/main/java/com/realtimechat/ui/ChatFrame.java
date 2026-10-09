package com.realtimechat.ui;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.realtimechat.client.ClientNetworkService;
import com.realtimechat.model.Message;
import com.realtimechat.model.MessageType;
import com.realtimechat.model.StoredMessage;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Main Chat Application Window in Swing.
 * Features:
 * - Real-time message bubbles with timestamps and sender headers
 * - Online user list sidebar (UserListPanel)
 * - Live connection status indicator (Connected / Disconnected)
 * - 1-to-1 private messaging and history loading
 */
public class ChatFrame extends JFrame {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Gson GSON = new Gson();

    private final ClientNetworkService networkService;
    private final String currentUsername;

    private UserListPanel userListPanel;
    private JPanel messagesContainer;
    private JScrollPane chatScrollPane;
    private JTextField messageInputField;
    private JButton sendButton;
    private JButton historyButton;
    private JLabel currentChatHeader;
    private JLabel connectionStatusLabel;

    private String activeRecipient = "ALL"; // "ALL" represents public room

    public ChatFrame(ClientNetworkService networkService, String currentUsername) {
        this.networkService = networkService;
        this.currentUsername = currentUsername;

        setTitle("Real-Time Chat - Logged in as: " + currentUsername);
        setSize(900, 620);
        setMinimumSize(new Dimension(650, 450));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initUI();
        setupNetworking();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // 1. Top Header Panel
        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setBackground(new Color(44, 62, 80));
        topHeader.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        currentChatHeader = new JLabel("📢 Public Chat");
        currentChatHeader.setFont(new Font("SansSerif", Font.BOLD, 16));
        currentChatHeader.setForeground(Color.WHITE);
        topHeader.add(currentChatHeader, BorderLayout.WEST);

        JPanel rightHeaderPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightHeaderPanel.setOpaque(false);

        connectionStatusLabel = new JLabel("🟢 Connected");
        connectionStatusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        connectionStatusLabel.setForeground(new Color(46, 204, 113)); // Bright green
        rightHeaderPanel.add(connectionStatusLabel);

        JLabel userBadge = new JLabel("👤 " + currentUsername);
        userBadge.setFont(new Font("SansSerif", Font.PLAIN, 13));
        userBadge.setForeground(Color.WHITE);
        rightHeaderPanel.add(userBadge);

        topHeader.add(rightHeaderPanel, BorderLayout.EAST);
        add(topHeader, BorderLayout.NORTH);

        // 2. Left Sidebar: Online User List
        userListPanel = new UserListPanel();
        userListPanel.setOnUserSelected(recipient -> {
            this.activeRecipient = recipient;
            if ("ALL".equalsIgnoreCase(recipient)) {
                currentChatHeader.setText("📢 Public Chat");
                historyButton.setEnabled(false);
            } else {
                currentChatHeader.setText("💬 Private Chat with: " + recipient);
                historyButton.setEnabled(true);
            }
            addSystemNotice("Switched conversation to: " + 
                    ("ALL".equals(recipient) ? "Public Chat" : recipient));
        });
        add(userListPanel, BorderLayout.WEST);

        // 3. Central Chat Area (Message Bubbles)
        messagesContainer = new JPanel();
        messagesContainer.setLayout(new BoxLayout(messagesContainer, BoxLayout.Y_AXIS));
        messagesContainer.setBackground(new Color(248, 249, 250));

        chatScrollPane = new JScrollPane(messagesContainer);
        chatScrollPane.setBorder(BorderFactory.createEmptyBorder());
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(chatScrollPane, BorderLayout.CENTER);

        // 4. Bottom Input Bar
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        bottomPanel.setBackground(Color.WHITE);

        historyButton = new JButton("Load History");
        historyButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        historyButton.setEnabled(false); // Initially in Public room
        historyButton.addActionListener(e -> requestHistory());
        bottomPanel.add(historyButton, BorderLayout.WEST);

        messageInputField = new JTextField();
        messageInputField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        messageInputField.addActionListener(e -> handleSendMessage());
        bottomPanel.add(messageInputField, BorderLayout.CENTER);

        sendButton = new JButton("Send");
        sendButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        sendButton.setBackground(new Color(41, 128, 185));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setPreferredSize(new Dimension(85, 36));
        sendButton.addActionListener(e -> handleSendMessage());
        bottomPanel.add(sendButton, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void setupNetworking() {
        // Register connection state listener
        networkService.addConnectionListener(connected -> {
            if (connected) {
                connectionStatusLabel.setText("🟢 Connected");
                connectionStatusLabel.setForeground(new Color(46, 204, 113));
            } else {
                connectionStatusLabel.setText("🔴 Disconnected");
                connectionStatusLabel.setForeground(new Color(231, 76, 60));
                addSystemNotice("Connection to server was lost.");
            }
        });

        // Register incoming message listener (invoked on Swing EDT by ClientNetworkService)
        networkService.addMessageListener(this::handleIncomingMessage);

        // Fetch initial user list
        networkService.sendMessage(new Message(MessageType.USER_LIST, currentUsername, null, ""));
    }

    private void handleSendMessage() {
        String text = messageInputField.getText().trim();
        if (text.isEmpty()) return;

        messageInputField.setText("");

        String timeStr = TIME_FMT.format(Instant.now().atZone(ZoneId.systemDefault()));

        if ("ALL".equalsIgnoreCase(activeRecipient)) {
            // Public Broadcast
            networkService.sendMessage(new Message(MessageType.CHAT, currentUsername, null, text));
            addMessageBubble(currentUsername, text, timeStr, MessageBubble.BubbleType.OUTGOING);
        } else {
            // Private 1-to-1 message
            networkService.sendMessage(new Message(MessageType.CHAT, currentUsername, activeRecipient, text));
            addMessageBubble(currentUsername, text, timeStr, MessageBubble.BubbleType.OUTGOING);
        }
    }

    private void requestHistory() {
        if ("ALL".equalsIgnoreCase(activeRecipient)) return;
        addSystemNotice("Requesting chat history with " + activeRecipient + "...");
        networkService.sendMessage(new Message(MessageType.HISTORY, currentUsername, null, activeRecipient));
    }

    private void handleIncomingMessage(Message msg) {
        String timeStr = Instant.ofEpochMilli(msg.getTimestamp())
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
                .format(TIME_FMT);

        switch (msg.getType()) {
            case CHAT:
                // Check if message is from server or another user
                if ("SERVER".equalsIgnoreCase(msg.getSender())) {
                    addSystemNotice(msg.getContent());
                } else {
                    boolean isFromMe = currentUsername.equalsIgnoreCase(msg.getSender());
                    if (!isFromMe) {
                        addMessageBubble(msg.getSender(), msg.getContent(), timeStr, MessageBubble.BubbleType.INCOMING);
                    }
                }
                break;

            case USER_LIST:
                userListPanel.updateUserList(msg.getContent(), currentUsername);
                break;

            case PRESENCE:
                userListPanel.updateUserPresence(msg.getSender(), msg.getContent());
                addSystemNotice("👤 " + msg.getSender() + " is now " + msg.getContent());
                break;

            case NOTIFICATION:
                handleNotification(msg);
                break;

            case HISTORY:
                renderHistory(msg);
                break;

            case ERROR:
                JOptionPane.showMessageDialog(this, msg.getContent(), "Server Error", JOptionPane.ERROR_MESSAGE);
                break;

            default:
                break;
        }
    }

    private void handleNotification(Message msg) {
        try {
            java.lang.reflect.Type mapType = new TypeToken<java.util.Map<String, Integer>>() {}.getType();
            java.util.Map<String, Integer> unread = GSON.fromJson(msg.getContent(), mapType);
            if (unread != null && !unread.isEmpty()) {
                unread.forEach(userListPanel::setUnreadCount);
                int total = unread.values().stream().mapToInt(Integer::intValue).sum();
                addSystemNotice("🔔 Notification: You have " + total + " unread message(s) waiting!");
            } else {
                addSystemNotice("🔔 " + msg.getContent());
            }
        } catch (Exception e) {
            addSystemNotice("🔔 " + msg.getContent());
        }
    }

    private void renderHistory(Message msg) {
        try {
            java.lang.reflect.Type listType = new TypeToken<List<StoredMessage>>() {}.getType();
            List<StoredMessage> history = GSON.fromJson(msg.getContent(), listType);

            addSystemNotice("--- Conversation History #" + msg.getConversationId() + " Loaded ---");
            if (history != null && !history.isEmpty()) {
                for (StoredMessage sm : history) {
                    boolean isMe = currentUsername.equalsIgnoreCase(sm.getSenderUsername());
                    String time = (sm.getCreatedAt() != null) ? sm.getCreatedAt().toString().substring(11, 19) : "";
                    addMessageBubble(
                            sm.getSenderUsername(),
                            sm.getContent(),
                            time,
                            isMe ? MessageBubble.BubbleType.OUTGOING : MessageBubble.BubbleType.INCOMING
                    );
                }
            } else {
                addSystemNotice("(No previous message history found)");
            }
        } catch (Exception e) {
            addSystemNotice("History payload: " + msg.getContent());
        }
    }

    public void addMessageBubble(String sender, String content, String timestamp, MessageBubble.BubbleType type) {
        MessageBubble bubble = new MessageBubble(sender, content, timestamp, type);
        messagesContainer.add(bubble);
        messagesContainer.revalidate();
        messagesContainer.repaint();
        scrollToBottom();
    }

    public void addSystemNotice(String notice) {
        MessageBubble bubble = new MessageBubble("Server", notice, "", MessageBubble.BubbleType.SYSTEM);
        messagesContainer.add(bubble);
        messagesContainer.revalidate();
        messagesContainer.repaint();
        scrollToBottom();
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar verticalBar = chatScrollPane.getVerticalScrollBar();
            verticalBar.setValue(verticalBar.getMaximum());
        });
    }
}
