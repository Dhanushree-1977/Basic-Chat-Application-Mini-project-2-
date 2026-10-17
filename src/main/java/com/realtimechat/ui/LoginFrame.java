package com.realtimechat.ui;

import com.realtimechat.client.ClientNetworkService;
import com.realtimechat.model.Message;
import com.realtimechat.model.MessageType;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;

/**
 * Glassmorphic Swing Authentication Window.
 * Features an aurora gradient backdrop, frosted translucent glass card,
 * glowing input fields, and modern glass buttons.
 */
public class LoginFrame extends JFrame {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5000;

    private final ClientNetworkService networkService;

    private GlassUtils.GlassTextField hostField;
    private GlassUtils.GlassTextField portField;
    private GlassUtils.GlassTextField usernameField;
    private GlassUtils.GlassPasswordField passwordField;
    private JLabel statusLabel;
    private GlassUtils.GlassButton loginButton;

    public LoginFrame() {
        this.networkService = new ClientNetworkService(DEFAULT_HOST, DEFAULT_PORT);

        setTitle("Real-Time Chat - Sign In");
        setSize(460, 580);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initUI();

        // Listen for server authentication responses
        networkService.addMessageListener(this::handleServerResponse);
    }

    private void initUI() {
        // 1. Aurora Mesh Background
        GlassUtils.BackgroundPanel backgroundPanel = new GlassUtils.BackgroundPanel();
        backgroundPanel.setLayout(new GridBagLayout()); // Centers the glass card

        // 2. Center Frosted Glass Card
        GlassUtils.GlassCard card = new GlassUtils.GlassCard(24);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(30, 36, 30, 36));
        card.setPreferredSize(new Dimension(380, 500));

        // Header Title
        JLabel logoLabel = new JLabel("✨ REAL-TIME CHAT");
        logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logoLabel.setForeground(GlassUtils.ACCENT_CYAN);
        logoLabel.setAlignmentX(CENTER_ALIGNMENT);
        card.add(logoLabel);

        card.add(Box.createRigidArea(new Dimension(0, 6)));

        JLabel titleLabel = new JLabel("Welcome Back");
        titleLabel.setFont(GlassUtils.FONT_TITLE);
        titleLabel.setForeground(GlassUtils.TEXT_PRIMARY);
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);
        card.add(titleLabel);

        card.add(Box.createRigidArea(new Dimension(0, 4)));

        JLabel subtitleLabel = new JLabel("Sign in to continue your conversations");
        subtitleLabel.setFont(GlassUtils.FONT_SUBTITLE);
        subtitleLabel.setForeground(GlassUtils.TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(CENTER_ALIGNMENT);
        card.add(subtitleLabel);

        card.add(Box.createRigidArea(new Dimension(0, 20)));

        // Form Fields
        hostField = new GlassUtils.GlassTextField(DEFAULT_HOST);
        portField = new GlassUtils.GlassTextField(String.valueOf(DEFAULT_PORT));
        usernameField = new GlassUtils.GlassTextField();
        passwordField = new GlassUtils.GlassPasswordField();

        // Host and Port in side-by-side row
        JPanel connectionRow = new JPanel(new BorderLayout(10, 0));
        connectionRow.setOpaque(false);

        JPanel hostCol = new JPanel();
        hostCol.setLayout(new BoxLayout(hostCol, BoxLayout.Y_AXIS));
        hostCol.setOpaque(false);
        JLabel hostLabel = new JLabel("Server Host");
        hostLabel.setFont(GlassUtils.FONT_SMALL);
        hostLabel.setForeground(GlassUtils.TEXT_SECONDARY);
        hostCol.add(hostLabel);
        hostCol.add(Box.createRigidArea(new Dimension(0, 4)));
        hostCol.add(hostField);
        connectionRow.add(hostCol, BorderLayout.CENTER);

        JPanel portCol = new JPanel();
        portCol.setLayout(new BoxLayout(portCol, BoxLayout.Y_AXIS));
        portCol.setOpaque(false);
        JLabel portLabel = new JLabel("Port");
        portLabel.setFont(GlassUtils.FONT_SMALL);
        portLabel.setForeground(GlassUtils.TEXT_SECONDARY);
        portCol.add(portLabel);
        portCol.add(Box.createRigidArea(new Dimension(0, 4)));
        portField.setPreferredSize(new Dimension(80, 36));
        portCol.add(portField);
        connectionRow.add(portCol, BorderLayout.EAST);

        card.add(connectionRow);
        card.add(Box.createRigidArea(new Dimension(0, 12)));

        addField(card, "Username", usernameField);
        addField(card, "Password", passwordField);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(GlassUtils.FONT_SMALL);
        statusLabel.setForeground(GlassUtils.ACCENT_ROSE);
        statusLabel.setAlignmentX(CENTER_ALIGNMENT);
        card.add(statusLabel);

        card.add(Box.createRigidArea(new Dimension(0, 10)));

        // Sign In Button
        loginButton = new GlassUtils.GlassButton("Sign In", true);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        loginButton.setAlignmentX(CENTER_ALIGNMENT);
        loginButton.addActionListener(e -> performLogin());
        card.add(loginButton);

        card.add(Box.createRigidArea(new Dimension(0, 10)));

        // Register Link Button
        GlassUtils.GlassButton registerLinkButton = new GlassUtils.GlassButton("Don't have an account? Register", false);
        registerLinkButton.setFont(GlassUtils.FONT_SUBTITLE);
        registerLinkButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        registerLinkButton.setAlignmentX(CENTER_ALIGNMENT);
        registerLinkButton.addActionListener(e -> openRegisterFrame());
        card.add(registerLinkButton);

        backgroundPanel.add(card);
        setContentPane(backgroundPanel);
    }

    private void addField(JPanel panel, String labelText, javax.swing.JComponent field) {
        JLabel label = new JLabel(labelText);
        label.setFont(GlassUtils.FONT_SMALL);
        label.setForeground(GlassUtils.TEXT_SECONDARY);
        label.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(label);

        panel.add(Box.createRigidArea(new Dimension(0, 4)));

        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        field.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(field);

        panel.add(Box.createRigidArea(new Dimension(0, 10)));
    }

    private void performLogin() {
        String host = hostField.getText().trim();
        String portStr = portField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setForeground(GlassUtils.ACCENT_ROSE);
            statusLabel.setText("Please enter both username and password.");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            statusLabel.setForeground(GlassUtils.ACCENT_ROSE);
            statusLabel.setText("Invalid port number.");
            return;
        }

        statusLabel.setText("Connecting to server...");
        statusLabel.setForeground(GlassUtils.ACCENT_CYAN);

        if (!networkService.isConnected()) {
            boolean connected = networkService.connect();
            if (!connected) {
                statusLabel.setForeground(GlassUtils.ACCENT_ROSE);
                statusLabel.setText("Could not reach chat server at " + host + ":" + port);
                return;
            }
        }

        networkService.setCurrentUsername(username);
        statusLabel.setText("Authenticating credentials...");
        statusLabel.setForeground(GlassUtils.ACCENT_CYAN);
        networkService.sendMessage(new Message(MessageType.LOGIN, username, null, password));
    }

    private void openRegisterFrame() {
        if (!networkService.isConnected()) {
            boolean connected = networkService.connect();
            if (!connected) {
                statusLabel.setForeground(GlassUtils.ACCENT_ROSE);
                statusLabel.setText("Could not reach chat server to register.");
                return;
            }
        }

        setVisible(false);
        RegisterFrame registerFrame = new RegisterFrame(networkService, this);
        registerFrame.setVisible(true);
    }

    private void handleServerResponse(Message msg) {
        if (!isVisible()) return;

        if (msg.getType() == MessageType.LOGIN) {
            if (msg.getContent() != null && msg.getContent().toLowerCase().contains("successful")) {
                dispose();
                ChatFrame chatFrame = new ChatFrame(networkService, usernameField.getText().trim());
                chatFrame.setVisible(true);
            } else {
                statusLabel.setForeground(GlassUtils.ACCENT_CYAN);
                statusLabel.setText(msg.getContent());
            }
        } else if (msg.getType() == MessageType.ERROR) {
            statusLabel.setForeground(GlassUtils.ACCENT_ROSE);
            statusLabel.setText(msg.getContent());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginFrame frame = new LoginFrame();
            frame.setVisible(true);
        });
    }
}
