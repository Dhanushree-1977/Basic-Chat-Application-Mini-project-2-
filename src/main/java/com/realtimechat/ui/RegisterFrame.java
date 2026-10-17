package com.realtimechat.ui;

import com.realtimechat.client.ClientNetworkService;
import com.realtimechat.model.Message;
import com.realtimechat.model.MessageType;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;

/**
 * Glassmorphic Swing Registration Window.
 * Form validation with frosted glass cards, glowing inputs, and neon buttons.
 */
public class RegisterFrame extends JFrame {

    private final ClientNetworkService networkService;
    private final JFrame parentLoginFrame;

    private GlassUtils.GlassTextField usernameField;
    private GlassUtils.GlassPasswordField passwordField;
    private GlassUtils.GlassPasswordField confirmPasswordField;
    private JLabel statusLabel;
    private GlassUtils.GlassButton registerButton;

    public RegisterFrame(ClientNetworkService networkService, JFrame parentLoginFrame) {
        this.networkService = networkService;
        this.parentLoginFrame = parentLoginFrame;

        setTitle("Real-Time Chat - Create Account");
        setSize(460, 560);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initUI();

        // Listen for server registration response
        networkService.addMessageListener(this::handleServerResponse);
    }

    private void initUI() {
        // 1. Aurora Mesh Background
        GlassUtils.BackgroundPanel backgroundPanel = new GlassUtils.BackgroundPanel();
        backgroundPanel.setLayout(new GridBagLayout());

        // 2. Frosted Glass Card
        GlassUtils.GlassCard card = new GlassUtils.GlassCard(24);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));
        card.setPreferredSize(new Dimension(380, 480));

        // Header Title
        JLabel logoLabel = new JLabel("🚀 JOIN REAL-TIME CHAT");
        logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logoLabel.setForeground(GlassUtils.ACCENT_CYAN);
        logoLabel.setAlignmentX(CENTER_ALIGNMENT);
        card.add(logoLabel);

        card.add(Box.createRigidArea(new Dimension(0, 6)));

        JLabel titleLabel = new JLabel("Create Account");
        titleLabel.setFont(GlassUtils.FONT_TITLE);
        titleLabel.setForeground(GlassUtils.TEXT_PRIMARY);
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);
        card.add(titleLabel);

        card.add(Box.createRigidArea(new Dimension(0, 4)));

        JLabel subtitleLabel = new JLabel("Start messaging securely via TLS sockets");
        subtitleLabel.setFont(GlassUtils.FONT_SUBTITLE);
        subtitleLabel.setForeground(GlassUtils.TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(CENTER_ALIGNMENT);
        card.add(subtitleLabel);

        card.add(Box.createRigidArea(new Dimension(0, 20)));

        // Form Fields
        usernameField = new GlassUtils.GlassTextField();
        passwordField = new GlassUtils.GlassPasswordField();
        confirmPasswordField = new GlassUtils.GlassPasswordField();

        addField(card, "Username (3-20 letters/numbers)", usernameField);
        addField(card, "Password (minimum 6 characters)", passwordField);
        addField(card, "Confirm Password", confirmPasswordField);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(GlassUtils.FONT_SMALL);
        statusLabel.setForeground(GlassUtils.ACCENT_ROSE);
        statusLabel.setAlignmentX(CENTER_ALIGNMENT);
        card.add(statusLabel);

        card.add(Box.createRigidArea(new Dimension(0, 8)));

        // Register Button
        registerButton = new GlassUtils.GlassButton("Register Account", true);
        registerButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        registerButton.setAlignmentX(CENTER_ALIGNMENT);
        registerButton.addActionListener(e -> performRegister());
        card.add(registerButton);

        card.add(Box.createRigidArea(new Dimension(0, 10)));

        // Back to Login Button
        GlassUtils.GlassButton backButton = new GlassUtils.GlassButton("Back to Login", false);
        backButton.setFont(GlassUtils.FONT_SUBTITLE);
        backButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        backButton.setAlignmentX(CENTER_ALIGNMENT);
        backButton.addActionListener(e -> {
            dispose();
            if (parentLoginFrame != null) {
                parentLoginFrame.setVisible(true);
            }
        });
        card.add(backButton);

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

        panel.add(Box.createRigidArea(new Dimension(0, 8)));
    }

    private void performRegister() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirm = new String(confirmPasswordField.getPassword());

        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            statusLabel.setText("Please fill out all fields.");
            return;
        }

        if (!username.matches("^[a-zA-Z0-9_]{3,20}$")) {
            statusLabel.setText("Username must be 3-20 alphanumeric characters.");
            return;
        }

        if (password.length() < 6) {
            statusLabel.setText("Password must be at least 6 characters.");
            return;
        }

        if (!password.equals(confirm)) {
            statusLabel.setText("Passwords do not match.");
            return;
        }

        statusLabel.setText("Sending registration request...");
        statusLabel.setForeground(GlassUtils.ACCENT_CYAN);

        networkService.sendMessage(new Message(MessageType.REGISTER, username, null, password));
    }

    private void handleServerResponse(Message msg) {
        if (!isVisible()) return;

        if (msg.getType() == MessageType.REGISTER) {
            JOptionPane.showMessageDialog(this, 
                    msg.getContent(), 
                    "Registration Successful", 
                    JOptionPane.INFORMATION_MESSAGE);
            dispose();
            if (parentLoginFrame != null) {
                parentLoginFrame.setVisible(true);
            }
        } else if (msg.getType() == MessageType.ERROR) {
            statusLabel.setForeground(GlassUtils.ACCENT_ROSE);
            statusLabel.setText(msg.getContent());
        }
    }
}
