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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Swing Registration Frame.
 * Handles new account creation with client-side form validation before transmission.
 */
public class RegisterFrame extends JFrame {

    private final ClientNetworkService networkService;
    private final JFrame parentLoginFrame;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JLabel statusLabel;
    private JButton registerButton;

    public RegisterFrame(ClientNetworkService networkService, JFrame parentLoginFrame) {
        this.networkService = networkService;
        this.parentLoginFrame = parentLoginFrame;

        setTitle("Real-Time Chat - Register Account");
        setSize(420, 480);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initUI();

        // Listen for server registration response
        networkService.addMessageListener(this::handleServerResponse);
    }

    private void initUI() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 35, 25, 35));
        mainPanel.setBackground(Color.WHITE);

        // Header Title
        JLabel titleLabel = new JLabel("Create an Account");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        titleLabel.setForeground(new Color(44, 62, 80));
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);

        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        JLabel subtitleLabel = new JLabel("Sign up to chat with friends in real-time");
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitleLabel.setForeground(Color.GRAY);
        subtitleLabel.setAlignmentX(CENTER_ALIGNMENT);
        mainPanel.add(subtitleLabel);

        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Form Fields
        usernameField = new JTextField(15);
        passwordField = new JPasswordField(15);
        confirmPasswordField = new JPasswordField(15);

        addField(mainPanel, "Username (3-20 letters/numbers):", usernameField);
        addField(mainPanel, "Password (minimum 6 characters):", passwordField);
        addField(mainPanel, "Confirm Password:", confirmPasswordField);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(192, 57, 43));
        statusLabel.setAlignmentX(CENTER_ALIGNMENT);
        mainPanel.add(statusLabel);

        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Action Buttons
        registerButton = new JButton("Register Account");
        registerButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        registerButton.setBackground(new Color(41, 128, 185));
        registerButton.setForeground(Color.WHITE);
        registerButton.setFocusPainted(false);
        registerButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        registerButton.setAlignmentX(CENTER_ALIGNMENT);
        registerButton.addActionListener(e -> performRegister());
        mainPanel.add(registerButton);

        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        JButton backButton = new JButton("Back to Login");
        backButton.setFont(new Font("SansSerif", Font.PLAIN, 13));
        backButton.setBorderPainted(false);
        backButton.setContentAreaFilled(false);
        backButton.setForeground(new Color(41, 128, 185));
        backButton.setAlignmentX(CENTER_ALIGNMENT);
        backButton.addActionListener(e -> {
            dispose();
            if (parentLoginFrame != null) {
                parentLoginFrame.setVisible(true);
            }
        });
        mainPanel.add(backButton);

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

        panel.add(Box.createRigidArea(new Dimension(0, 12)));
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
        statusLabel.setForeground(new Color(41, 128, 185));

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
            statusLabel.setForeground(new Color(192, 57, 43));
            statusLabel.setText(msg.getContent());
        }
    }
}
