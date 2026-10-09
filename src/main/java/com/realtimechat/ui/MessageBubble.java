package com.realtimechat.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Custom Swing component that renders modern, rounded chat bubbles with
 * sender names, message content, and timestamps.
 */
public class MessageBubble extends JPanel {

    private static final Font SENDER_FONT = new Font("SansSerif", Font.BOLD, 12);
    private static final Font CONTENT_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font TIME_FONT = new Font("SansSerif", Font.ITALIC, 10);

    // Color Palette
    private static final Color OUTGOING_BG = new Color(220, 248, 198); // Soft WhatsApp green
    private static final Color INCOMING_BG = new Color(255, 255, 255); // Crisp White
    private static final Color SYSTEM_BG = new Color(235, 237, 239);   // Neutral Light Gray
    private static final Color TIME_COLOR = new Color(128, 128, 128);

    public enum BubbleType {
        OUTGOING, // Sent by the current user (Aligned Right)
        INCOMING, // Received from another user (Aligned Left)
        SYSTEM    // System notice (Centered)
    }

    public MessageBubble(String sender, String content, String timestamp, BubbleType type) {
        setLayout(new FlowLayout(
                type == BubbleType.OUTGOING ? FlowLayout.RIGHT :
                type == BubbleType.INCOMING ? FlowLayout.LEFT : FlowLayout.CENTER,
                10, 4
        ));
        setOpaque(false);

        Color bubbleBg = (type == BubbleType.OUTGOING) ? OUTGOING_BG :
                         (type == BubbleType.INCOMING) ? INCOMING_BG : SYSTEM_BG;

        // Bubble inner card
        RoundedPanel card = new RoundedPanel(16, bubbleBg);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        if (type != BubbleType.SYSTEM) {
            // Sender header
            JLabel senderLabel = new JLabel(type == BubbleType.OUTGOING ? "You" : sender);
            senderLabel.setFont(SENDER_FONT);
            senderLabel.setForeground(type == BubbleType.OUTGOING ? new Color(18, 107, 34) : new Color(33, 97, 140));
            card.add(senderLabel);
            card.add(Box.createRigidArea(new Dimension(0, 3)));
        }

        // Message content (HTML formatting enables automatic line wrapping)
        JLabel contentLabel = new JLabel("<html><body style='width: 280px;'>" + escapeHtml(content) + "</body></html>");
        contentLabel.setFont(CONTENT_FONT);
        contentLabel.setForeground(Color.BLACK);
        card.add(contentLabel);

        // Timestamp footer
        if (timestamp != null && !timestamp.isEmpty()) {
            card.add(Box.createRigidArea(new Dimension(0, 4)));
            JLabel timeLabel = new JLabel(timestamp);
            timeLabel.setFont(TIME_FONT);
            timeLabel.setForeground(TIME_COLOR);
            JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            timePanel.setOpaque(false);
            timePanel.add(timeLabel);
            card.add(timePanel);
        }

        add(card);
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\n", "<br/>");
    }

    /**
     * Inner custom panel providing smooth rounded corners with anti-aliasing.
     */
    private static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color backgroundColor;

        public RoundedPanel(int radius, Color bgColor) {
            this.radius = radius;
            this.backgroundColor = bgColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(backgroundColor);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.setColor(new Color(210, 214, 222));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.dispose();
        }
    }
}
