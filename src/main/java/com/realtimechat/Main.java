package com.realtimechat;

import com.realtimechat.server.ChatServer;
import com.realtimechat.ui.LoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Main application launcher.
 * Usage:
 *   java com.realtimechat.Main server -> Starts the TLS ChatServer
 *   java com.realtimechat.Main client -> Starts the GUI LoginFrame (Default)
 */
public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && "server".equalsIgnoreCase(args[0])) {
            System.out.println("[Main] Launching Real-Time Chat Server...");
            ChatServer server = new ChatServer();
            server.start();
        } else if (args.length > 0 && "demo".equalsIgnoreCase(args[0])) {
            LiveDemonstration.main(args);
        } else {
            System.out.println("[Main] Launching Real-Time Chat Desktop Client...");
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            SwingUtilities.invokeLater(() -> {
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
            });
        }
    }
}
