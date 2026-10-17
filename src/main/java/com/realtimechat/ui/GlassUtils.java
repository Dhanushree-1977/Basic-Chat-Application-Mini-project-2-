package com.realtimechat.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollBar;
import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Design system utilities and custom components for Glassmorphism UI.
 * Features translucent frosted surfaces, specular lighting highlights,
 * vibrant aurora background glows, and crisp hairline glass borders.
 */
public class GlassUtils {

    // --- Color Palette ---
    public static final Color BG_DARK = new Color(11, 15, 25);
    public static final Color BG_CARD_GLASS = new Color(255, 255, 255, 22);
    public static final Color BG_CARD_GLASS_HOVER = new Color(255, 255, 255, 35);
    public static final Color BORDER_GLASS = new Color(255, 255, 255, 60);
    public static final Color BORDER_GLASS_FOCUSED = new Color(165, 180, 252, 220);

    // Neon Accents
    public static final Color ACCENT_PURPLE = new Color(139, 92, 246);
    public static final Color ACCENT_INDIGO = new Color(99, 102, 241);
    public static final Color ACCENT_CYAN = new Color(6, 182, 212);
    public static final Color ACCENT_GREEN = new Color(52, 211, 153);
    public static final Color ACCENT_ROSE = new Color(244, 63, 94);

    // Text Colors
    public static final Color TEXT_PRIMARY = new Color(248, 250, 252);
    public static final Color TEXT_SECONDARY = new Color(148, 163, 184);
    public static final Color TEXT_MUTED = new Color(100, 116, 139);
    public static final Color TEXT_GLOW = new Color(224, 231, 255);

    // Typography
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);

    /**
     * Top-level container panel that renders a rich obsidian aurora mesh background.
     */
    public static class BackgroundPanel extends JPanel {
        public BackgroundPanel() {
            setOpaque(true);
            setBackground(BG_DARK);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // 1. Dark background base gradient
            GradientPaint baseGradient = new GradientPaint(
                    0, 0, new Color(15, 23, 42),
                    w, h, new Color(10, 15, 30)
            );
            g2.setPaint(baseGradient);
            g2.fillRect(0, 0, w, h);

            // 2. Violet / Indigo Aurora orb (top left)
            if (w > 0 && h > 0) {
                float radius1 = Math.max(w, h) * 0.65f;
                Point2D center1 = new Point2D.Float(w * 0.15f, h * 0.1f);
                float[] dist1 = {0.0f, 0.45f, 1.0f};
                Color[] colors1 = {
                        new Color(99, 102, 241, 100), // Vibrant Indigo
                        new Color(139, 92, 246, 35),  // Purple fade
                        new Color(15, 23, 42, 0)      // Transparent
                };
                RadialGradientPaint p1 = new RadialGradientPaint(center1, radius1, dist1, colors1);
                g2.setPaint(p1);
                g2.fillRect(0, 0, w, h);

                // 3. Cyan / Teal Aurora orb (bottom right)
                float radius2 = Math.max(w, h) * 0.55f;
                Point2D center2 = new Point2D.Float(w * 0.85f, h * 0.85f);
                float[] dist2 = {0.0f, 0.45f, 1.0f};
                Color[] colors2 = {
                        new Color(6, 182, 212, 85),  // Vibrant Cyan
                        new Color(59, 130, 246, 25), // Blue fade
                        new Color(15, 23, 42, 0)
                };
                RadialGradientPaint p2 = new RadialGradientPaint(center2, radius2, dist2, colors2);
                g2.setPaint(p2);
                g2.fillRect(0, 0, w, h);

                // 4. Subtle Magenta specular burst (center right)
                float radius3 = Math.max(w, h) * 0.35f;
                Point2D center3 = new Point2D.Float(w * 0.7f, h * 0.25f);
                float[] dist3 = {0.0f, 1.0f};
                Color[] colors3 = {
                        new Color(236, 72, 153, 45), // Hot pink glow
                        new Color(15, 23, 42, 0)
                };
                RadialGradientPaint p3 = new RadialGradientPaint(center3, radius3, dist3, colors3);
                g2.setPaint(p3);
                g2.fillRect(0, 0, w, h);
            }

            g2.dispose();
        }
    }

    /**
     * Translucent Frosted Glass Card with subtle specular highlight and hairline border.
     */
    public static class GlassCard extends JPanel {
        private int cornerRadius = 20;
        private Color glassColor = BG_CARD_GLASS;
        private Color borderColor = BORDER_GLASS;

        public GlassCard() {
            this(20, BG_CARD_GLASS);
        }

        public GlassCard(int radius) {
            this(radius, BG_CARD_GLASS);
        }

        public GlassCard(int radius, Color glassColor) {
            this.cornerRadius = radius;
            this.glassColor = glassColor;
            setOpaque(false);
        }

        public void setCornerRadius(int r) {
            this.cornerRadius = r;
            repaint();
        }

        public void setGlassColor(Color c) {
            this.glassColor = c;
            repaint();
        }

        public void setBorderColor(Color c) {
            this.borderColor = c;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Glass background fill
            g2.setColor(glassColor);
            g2.fillRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);

            // Specular light gradient (reflection from top)
            GradientPaint specular = new GradientPaint(
                    0, 0, new Color(255, 255, 255, 38),
                    0, h / 2f, new Color(255, 255, 255, 5)
            );
            g2.setPaint(specular);
            g2.fillRoundRect(0, 0, w - 1, h / 2, cornerRadius, cornerRadius);

            // Frosted hairline glass border (brighter at top, softer at bottom)
            GradientPaint borderGrad = new GradientPaint(
                    0, 0, new Color(255, 255, 255, 95),
                    0, h, new Color(255, 255, 255, 25)
            );
            g2.setPaint(borderGrad);
            g2.drawRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Modern Glass Button with hover animations and optional vibrant gradient styling.
     */
    public static class GlassButton extends JButton {
        private boolean isHovered = false;
        private boolean isPressed = false;
        private final boolean isPrimary;
        private int cornerRadius = 14;

        public GlassButton(String text, boolean isPrimary) {
            super(text);
            this.isPrimary = isPrimary;
            setFont(FONT_BOLD);
            setForeground(TEXT_PRIMARY);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    isPressed = true;
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    isPressed = false;
                    repaint();
                }
            });
        }

        public void setCornerRadius(int r) {
            this.cornerRadius = r;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            if (isPrimary) {
                // Vibrant Indigo to Purple gradient
                Color c1 = isPressed ? ACCENT_INDIGO.darker() : (isHovered ? new Color(129, 140, 248) : ACCENT_INDIGO);
                Color c2 = isPressed ? ACCENT_PURPLE.darker() : (isHovered ? new Color(167, 139, 250) : ACCENT_PURPLE);
                GradientPaint gp = new GradientPaint(0, 0, c1, w, h, c2);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);

                // Specular top highlight
                g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 80), 0, h / 2f, new Color(255, 255, 255, 0)));
                g2.fillRoundRect(0, 0, w - 1, h / 2, cornerRadius, cornerRadius);

                // Border
                g2.setColor(isHovered ? new Color(255, 255, 255, 180) : new Color(255, 255, 255, 90));
                g2.drawRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);
            } else {
                // Frosted neutral glass button
                Color bg = isPressed ? new Color(255, 255, 255, 50) : (isHovered ? new Color(255, 255, 255, 40) : new Color(255, 255, 255, 20));
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);

                // Glass border
                g2.setColor(isHovered ? BORDER_GLASS_FOCUSED : BORDER_GLASS);
                g2.drawRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Modern Frosted Glass Text Field with smooth focus glow.
     */
    public static class GlassTextField extends JTextField {
        private boolean isFocused = false;
        private int cornerRadius = 12;

        public GlassTextField() {
            this("");
        }

        public GlassTextField(String text) {
            super(text);
            init();
        }

        public GlassTextField(int columns) {
            super(columns);
            init();
        }

        private void init() {
            setOpaque(false);
            setFont(FONT_BODY);
            setForeground(TEXT_PRIMARY);
            setCaretColor(Color.WHITE);
            setSelectedTextColor(Color.WHITE);
            setSelectionColor(new Color(99, 102, 241, 140));
            setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));

            addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    isFocused = true;
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    isFocused = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Translucent glass fill
            g2.setColor(isFocused ? new Color(255, 255, 255, 32) : new Color(255, 255, 255, 18));
            g2.fillRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);

            // Glowing border
            if (isFocused) {
                g2.setColor(BORDER_GLASS_FOCUSED);
                g2.drawRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);
                // Outer glow line
                g2.setColor(new Color(99, 102, 241, 70));
                g2.drawRoundRect(1, 1, w - 3, h - 3, cornerRadius - 1, cornerRadius - 1);
            } else {
                g2.setColor(BORDER_GLASS);
                g2.drawRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Modern Frosted Glass Password Field with smooth focus glow.
     */
    public static class GlassPasswordField extends JPasswordField {
        private boolean isFocused = false;
        private int cornerRadius = 12;

        public GlassPasswordField() {
            init();
        }

        public GlassPasswordField(int columns) {
            super(columns);
            init();
        }

        private void init() {
            setOpaque(false);
            setFont(FONT_BODY);
            setForeground(TEXT_PRIMARY);
            setCaretColor(Color.WHITE);
            setSelectedTextColor(Color.WHITE);
            setSelectionColor(new Color(99, 102, 241, 140));
            setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));

            addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    isFocused = true;
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    isFocused = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2.setColor(isFocused ? new Color(255, 255, 255, 32) : new Color(255, 255, 255, 18));
            g2.fillRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);

            if (isFocused) {
                g2.setColor(BORDER_GLASS_FOCUSED);
                g2.drawRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);
                g2.setColor(new Color(99, 102, 241, 70));
                g2.drawRoundRect(1, 1, w - 3, h - 3, cornerRadius - 1, cornerRadius - 1);
            } else {
                g2.setColor(BORDER_GLASS);
                g2.drawRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Minimal translucent scrollbar that seamlessly integrates into the glass aesthetic.
     */
    public static class GlassScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = new Color(255, 255, 255, 60);
            this.trackColor = new Color(0, 0, 0, 0);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, java.awt.Rectangle trackBounds) {
            // Invisible track to allow glass background to shine through
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, java.awt.Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color color = isThumbRollover() ? new Color(255, 255, 255, 110) : new Color(255, 255, 255, 55);
            g2.setColor(color);
            int arc = Math.min(thumbBounds.width, 10);
            g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2, thumbBounds.width - 4, thumbBounds.height - 4, arc, arc);

            g2.dispose();
        }
    }
}
