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
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Glassmorphic chat bubble with frosted translucent cards, specular lighting,
 * and high-contrast modern typography.
 */
public class MessageBubble extends JPanel {

    public enum BubbleType {
        OUTGOING, // Sent by current user (Right-aligned, glowing violet/indigo glass)
        INCOMING, // Received from another user (Left-aligned, frosted pearl glass)
        SYSTEM    // System notice (Centered, minimal frosted pill)
    }

    public MessageBubble(String sender, String content, String timestamp, BubbleType type) {
        setLayout(new FlowLayout(
                type == BubbleType.OUTGOING ? FlowLayout.RIGHT :
                type == BubbleType.INCOMING ? FlowLayout.LEFT : FlowLayout.CENTER,
                14, 4
        ));
        setOpaque(false);

        // Glass Card Container
        GlassBubbleCard card = new GlassBubbleCard(type);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        if (type != BubbleType.SYSTEM) {
            // Sender header
            JLabel senderLabel = new JLabel(type == BubbleType.OUTGOING ? "You" : sender);
            senderLabel.setFont(GlassUtils.FONT_BOLD);
            senderLabel.setForeground(type == BubbleType.OUTGOING ? GlassUtils.TEXT_GLOW : GlassUtils.ACCENT_CYAN);
            card.add(senderLabel);
            card.add(Box.createRigidArea(new Dimension(0, 4)));
        }

        // Message text
        String textColor = (type == BubbleType.SYSTEM) ? "#94A3B8" : "#F8FAFC";
        JLabel contentLabel = new JLabel("<html><body style='width: 290px; color: " + textColor + "; font-family: Segoe UI, sans-serif; font-size: 13px;'>" 
                + escapeHtml(content) + "</body></html>");
        contentLabel.setFont(GlassUtils.FONT_BODY);
        card.add(contentLabel);

        // Timestamp footer
        if (timestamp != null && !timestamp.isEmpty()) {
            card.add(Box.createRigidArea(new Dimension(0, 4)));
            JLabel timeLabel = new JLabel(timestamp);
            timeLabel.setFont(GlassUtils.FONT_SMALL);
            timeLabel.setForeground(new Color(203, 213, 225, 180));
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
     * Custom glassmorphic bubble card with specular gradient shine and hairline edge reflection.
     */
    private static class GlassBubbleCard extends JPanel {
        private final BubbleType type;

        public GlassBubbleCard(BubbleType type) {
            this.type = type;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int radius = (type == BubbleType.SYSTEM) ? 14 : 18;

            if (type == BubbleType.OUTGOING) {
                // Vibrant Violet/Indigo Glass
                GradientPaint bgGrad = new GradientPaint(
                        0, 0, new Color(99, 102, 241, 185),
                        w, h, new Color(139, 92, 246, 195)
                );
                g2.setPaint(bgGrad);
                g2.fillRoundRect(0, 0, w - 1, h - 1, radius, radius);

                // Specular top highlight
                g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 75), 0, h / 2f, new Color(255, 255, 255, 0)));
                g2.fillRoundRect(0, 0, w - 1, h / 2, radius, radius);

                // Hairline glowing border
                g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 130), 0, h, new Color(255, 255, 255, 45)));
                g2.drawRoundRect(0, 0, w - 1, h - 1, radius, radius);

            } else if (type == BubbleType.INCOMING) {
                // Frosted Pearl Glass
                g2.setColor(new Color(255, 255, 255, 28));
                g2.fillRoundRect(0, 0, w - 1, h - 1, radius, radius);

                // Specular shine
                g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 45), 0, h / 2f, new Color(255, 255, 255, 5)));
                g2.fillRoundRect(0, 0, w - 1, h / 2, radius, radius);

                // Subtle white edge
                g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 80), 0, h, new Color(255, 255, 255, 20)));
                g2.drawRoundRect(0, 0, w - 1, h - 1, radius, radius);

            } else {
                // Minimal System Notice Pill
                g2.setColor(new Color(255, 255, 255, 16));
                g2.fillRoundRect(0, 0, w - 1, h - 1, radius, radius);
                g2.setColor(new Color(255, 255, 255, 40));
                g2.drawRoundRect(0, 0, w - 1, h - 1, radius, radius);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
