package com.realtimechat.ui;

import com.realtimechat.client.ClientNetworkService;
import com.realtimechat.model.Message;
import com.realtimechat.model.MessageType;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Swing Authentication & Launch Entry Window.
 * Connects to the chat server, authenticates credentials,
 * and launches the main ChatFrame upon successful validation.
 */
public class LoginFrame extends JFrame {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5000;

    private final ClientNetworkService networkService;

    private JTextField hostField;
    private JTextField portField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel statusLabel;
    private JButton loginButton;

    public LoginFrame() {
        this.networkService = new ClientNetworkService(DEFAULT_HOST, DEFAULT_PORT);

        setTitle("Real-Time Chat - Sign In");
        setSize(420, 520);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initUI();

        // Listen for server authentication responses (dispatched to EDT by networkService)
        networkService.addMessageListener(this::handleServerResponse);
    }

    private void initUI() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 35, 25, 35));
        mainPanel.setBackground(Color.WHITE);

        // Header Title
        JLabel titleLabel = new JLabel("Welcome Back!");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setForeground(new Color(44, 62, 80));
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);

        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        JLabel subtitleLabel = new JLabel("Sign in to your account");
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitleLabel.setForeground(Color.GRAY);
        subtitleLabel.setAlignmentX(CENTER_ALIGNMENT);
        mainPanel.add(subtitleLabel);

        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Server Connection Config Fields
        hostField = new JTextField(DEFAULT_HOST);
        portField = new JTextField(String.valueOf(DEFAULT_PORT));
        usernameField = new JTextField();
        passwordField = new JPasswordField();

        addField(mainPanel, "Server Host:", hostField);
        addField(mainPanel, "Server Port:", portField);
        addField(mainPanel, "Username:", usernameField);
        addField(mainPanel, "Password:", passwordField);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(192, 57, 43));
        statusLabel.setAlignmentX(CENTER_ALIGNMENT);
        mainPanel.add(statusLabel);

        mainPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        // Login Button
        loginButton = new JButton("Sign In");
        loginButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        loginButton.setBackground(new Color(41, 128, 185));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        loginButton.setAlignmentX(CENTER_ALIGNMENT);
        loginButton.addActionListener(e -> performLogin());
        mainPanel.add(loginButton);

        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Link to Registration Frame
        JButton registerLinkButton = new JButton("Don't have an account? Register");
        registerLinkButton.setFont(new Font("SansSerif", Font.PLAIN, 13));
        registerLinkButton.setBorderPainted(false);
        registerLinkButton.setContentAreaFilled(false);
        registerLinkButton.setForeground(new Color(41, 128, 185));
        registerLinkButton.setAlignmentX(CENTER_ALIGNMENT);
        registerLinkButton.addActionListener(e -> openRegisterFrame());
        mainPanel.add(registerLinkButton);

        add(mainPanel);
    }

    private void addField(JPanel panel, String labelText, JTextField field) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("SansSerif", Font.PLAIN, 13));
        label.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(label);

        panel.add(Box.createRigidArea(new Dimension(0, 4)));

        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
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
            statusLabel.setForeground(new Color(192, 57, 43));
            statusLabel.setText("Please enter both username and password.");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid port number.");
            return;
        }

        statusLabel.setText("Connecting to server...");
        statusLabel.setForeground(new Color(41, 128, 185));

        // Connect if not already connected
        if (!networkService.isConnected()) {
            boolean connected = networkService.connect();
            if (!connected) {
                statusLabel.setForeground(new Color(192, 57, 43));
                statusLabel.setText("Could not reach chat server at " + host + ":" + port);
                return;
            }
        }

        networkService.setCurrentUsername(username);
        statusLabel.setText("Authenticating credentials...");
        networkService.sendMessage(new Message(MessageType.LOGIN, username, null, password));
    }

    private void openRegisterFrame() {
        if (!networkService.isConnected()) {
            boolean connected = networkService.connect();
            if (!connected) {
                statusLabel.setForeground(new Color(192, 57, 43));
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
                // Authentication succeeded: open ChatFrame
                dispose();
                ChatFrame chatFrame = new ChatFrame(networkService, usernameField.getText().trim());
                chatFrame.setVisible(true);
            } else {
                statusLabel.setForeground(new Color(41, 128, 185));
                statusLabel.setText(msg.getContent());
            }
        } else if (msg.getType() == MessageType.ERROR) {
            statusLabel.setForeground(new Color(192, 57, 43));
            statusLabel.setText(msg.getContent());
        }
    }

    public static void main(String[] args) {
        // Set modern Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            LoginFrame frame = new LoginFrame();
            frame.setVisible(true);
        });
    }
}
