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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Main Glassmorphic Chat Window in Swing.
 * Features:
 * - Aurora gradient ambient background
 * - Frosted glass top header with status badges
 * - Translucent message bubble timeline with specular highlights
 * - Frosted sidebar with presence indicators
 * - Glowing glass input dock
 */
public class ChatFrame extends JFrame {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Gson GSON = new Gson();

    private final ClientNetworkService networkService;
    private final String currentUsername;

    private UserListPanel userListPanel;
    private JPanel messagesContainer;
    private JScrollPane chatScrollPane;
    private GlassUtils.GlassTextField messageInputField;
    private GlassUtils.GlassButton sendButton;
    private GlassUtils.GlassButton historyButton;
    private JLabel currentChatHeader;
    private JLabel connectionStatusLabel;

    private String activeRecipient = "ALL"; // "ALL" represents public room

    public ChatFrame(ClientNetworkService networkService, String currentUsername) {
        this.networkService = networkService;
        this.currentUsername = currentUsername;

        setTitle("Real-Time Chat - " + currentUsername);
        setSize(960, 660);
        setMinimumSize(new Dimension(720, 500));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initUI();
        setupNetworking();
    }

    private void initUI() {
        // Window container with aurora mesh gradient background
        GlassUtils.BackgroundPanel backgroundPanel = new GlassUtils.BackgroundPanel();
        backgroundPanel.setLayout(new BorderLayout());

        // 1. Top Frosted Header Bar
        JPanel topHeader = new FrostedBar(true);
        topHeader.setLayout(new BorderLayout());
        topHeader.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        currentChatHeader = new JLabel("📢 Public Chat");
        currentChatHeader.setFont(GlassUtils.FONT_TITLE);
        currentChatHeader.setForeground(GlassUtils.TEXT_PRIMARY);
        topHeader.add(currentChatHeader, BorderLayout.WEST);

        JPanel rightHeaderPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightHeaderPanel.setOpaque(false);

        connectionStatusLabel = new JLabel("🟢 Connected");
        connectionStatusLabel.setFont(GlassUtils.FONT_BOLD);
        connectionStatusLabel.setForeground(GlassUtils.ACCENT_GREEN);
        rightHeaderPanel.add(connectionStatusLabel);

        // User Profile Pill
        GlassProfilePill userBadge = new GlassProfilePill("👤 " + currentUsername);
        rightHeaderPanel.add(userBadge);

        topHeader.add(rightHeaderPanel, BorderLayout.EAST);
        backgroundPanel.add(topHeader, BorderLayout.NORTH);

        // 2. Left Frosted Sidebar (User List)
        userListPanel = new UserListPanel();
        userListPanel.setOnUserSelected(recipient -> {
            this.activeRecipient = recipient;
            if ("ALL".equalsIgnoreCase(recipient)) {
                currentChatHeader.setText("📢 Public Chat");
                historyButton.setEnabled(false);
            } else {
                currentChatHeader.setText("💬 " + recipient);
                historyButton.setEnabled(true);
            }
            addSystemNotice("Switched conversation to: " + 
                    ("ALL".equals(recipient) ? "Public Chat" : recipient));
        });
        backgroundPanel.add(userListPanel, BorderLayout.WEST);

        // 3. Central Chat Bubble Container
        messagesContainer = new JPanel();
        messagesContainer.setLayout(new BoxLayout(messagesContainer, BoxLayout.Y_AXIS));
        messagesContainer.setOpaque(false);
        messagesContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        chatScrollPane = new JScrollPane(messagesContainer);
        chatScrollPane.setOpaque(false);
        chatScrollPane.getViewport().setOpaque(false);
        chatScrollPane.setBorder(BorderFactory.createEmptyBorder());
        chatScrollPane.getVerticalScrollBar().setUI(new GlassUtils.GlassScrollBarUI());
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        backgroundPanel.add(chatScrollPane, BorderLayout.CENTER);

        // 4. Bottom Glass Input Dock
        JPanel bottomPanel = new FrostedBar(false);
        bottomPanel.setLayout(new BorderLayout(12, 0));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));

        historyButton = new GlassUtils.GlassButton("Load History", false);
        historyButton.setEnabled(false);
        historyButton.setPreferredSize(new Dimension(115, 40));
        historyButton.addActionListener(e -> requestHistory());
        bottomPanel.add(historyButton, BorderLayout.WEST);

        messageInputField = new GlassUtils.GlassTextField();
        messageInputField.addActionListener(e -> handleSendMessage());
        bottomPanel.add(messageInputField, BorderLayout.CENTER);

        sendButton = new GlassUtils.GlassButton("Send", true);
        sendButton.setPreferredSize(new Dimension(95, 40));
        sendButton.addActionListener(e -> handleSendMessage());
        bottomPanel.add(sendButton, BorderLayout.EAST);

        backgroundPanel.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(backgroundPanel);
    }

    private void setupNetworking() {
        networkService.addConnectionListener(connected -> {
            if (connected) {
                connectionStatusLabel.setText("🟢 Connected");
                connectionStatusLabel.setForeground(GlassUtils.ACCENT_GREEN);
            } else {
                connectionStatusLabel.setText("🔴 Disconnected");
                connectionStatusLabel.setForeground(GlassUtils.ACCENT_ROSE);
                addSystemNotice("Connection to server was lost.");
            }
        });

        networkService.addMessageListener(this::handleIncomingMessage);
        networkService.sendMessage(new Message(MessageType.USER_LIST, currentUsername, null, ""));
    }

    private void handleSendMessage() {
        String text = messageInputField.getText().trim();
        if (text.isEmpty()) return;

        messageInputField.setText("");
        String timeStr = TIME_FMT.format(Instant.now().atZone(ZoneId.systemDefault()));

        if ("ALL".equalsIgnoreCase(activeRecipient)) {
            networkService.sendMessage(new Message(MessageType.CHAT, currentUsername, null, text));
            addMessageBubble(currentUsername, text, timeStr, MessageBubble.BubbleType.OUTGOING);
        } else {
            networkService.sendMessage(new Message(MessageType.CHAT, currentUsername, activeRecipient, text));
            addMessageBubble(currentUsername, text, timeStr, MessageBubble.BubbleType.OUTGOING);
        }
    }

    private void requestHistory() {
        if ("ALL".equalsIgnoreCase(activeRecipient)) return;
        addSystemNotice("Requesting conversation history with " + activeRecipient + "...");
        networkService.sendMessage(new Message(MessageType.HISTORY, currentUsername, null, activeRecipient));
    }

    private void handleIncomingMessage(Message msg) {
        String timeStr = Instant.ofEpochMilli(msg.getTimestamp())
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
                .format(TIME_FMT);

        switch (msg.getType()) {
            case CHAT:
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

    /**
     * Frosted Glass Bar for Top Header and Bottom Input Dock.
     */
    private static class FrostedBar extends JPanel {
        private final boolean isTop;

        public FrostedBar(boolean isTop) {
            this.isTop = isTop;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Glass background wash
            g2.setColor(new Color(255, 255, 255, 16));
            g2.fillRect(0, 0, w, h);

            // Hairline divider
            g2.setColor(new Color(255, 255, 255, 35));
            if (isTop) {
                g2.drawLine(0, h - 1, w, h - 1);
            } else {
                g2.drawLine(0, 0, w, 0);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Glass Profile Badge Pill.
     */
    private static class GlassProfilePill extends JPanel {
        public GlassProfilePill(String text) {
            setLayout(new FlowLayout(FlowLayout.CENTER, 10, 4));
            setOpaque(false);
            JLabel label = new JLabel(text);
            label.setFont(GlassUtils.FONT_BOLD);
            label.setForeground(GlassUtils.TEXT_PRIMARY);
            add(label);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(255, 255, 255, 22));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.setColor(new Color(255, 255, 255, 60));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
