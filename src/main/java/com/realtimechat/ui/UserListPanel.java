package com.realtimechat.ui;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Glassmorphic sidebar panel displaying online contacts with presence status
 * and glowing unread notification badges.
 */
public class UserListPanel extends JPanel {

    public static final String PUBLIC_CHAT = "📢 Public Chat";

    private final DefaultListModel<String> listModel;
    private final JList<String> userList;
    private final JLabel headerLabel;
    private final Map<String, Integer> unreadCounts = new HashMap<>();
    private final Map<String, Boolean> presenceStatus = new HashMap<>(); // username -> isOnline
    private Consumer<String> onUserSelected;

    public UserListPanel() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(240, 0));
        setOpaque(false);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(255, 255, 255, 30)));

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));

        headerLabel = new JLabel("Contacts (0 Online)");
        headerLabel.setFont(GlassUtils.FONT_BOLD);
        headerLabel.setForeground(GlassUtils.TEXT_PRIMARY);
        headerPanel.add(headerLabel, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);

        // List Model & JList
        listModel = new DefaultListModel<>();
        listModel.addElement(PUBLIC_CHAT);

        userList = new JList<>(listModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setSelectedIndex(0);
        userList.setFont(GlassUtils.FONT_BODY);
        userList.setOpaque(false);
        userList.setFixedCellHeight(42);

        // Glassmorphic List Item Renderer
        userList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, 
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setOpaque(false);
                label.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
                String username = (String) value;

                if (PUBLIC_CHAT.equals(username)) {
                    label.setText(username);
                    label.setFont(GlassUtils.FONT_BOLD);
                } else {
                    boolean online = presenceStatus.getOrDefault(username, true);
                    String statusDot = online ? "🟢 " : "⚫ ";
                    int unread = unreadCounts.getOrDefault(username, 0);
                    String badge = (unread > 0) ? "  [" + unread + "]" : "";
                    label.setText(statusDot + username + badge);
                    label.setFont(GlassUtils.FONT_BODY);
                }

                if (isSelected) {
                    label.setForeground(Color.WHITE);
                } else {
                    label.setForeground(new Color(226, 232, 240));
                }
                return new GlassListCellWrapper(label, isSelected);
            }
        });

        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = userList.getSelectedValue();
                if (selected != null) {
                    if (!PUBLIC_CHAT.equals(selected)) {
                        unreadCounts.remove(selected);
                        userList.repaint();
                    }
                    if (onUserSelected != null) {
                        onUserSelected.accept(PUBLIC_CHAT.equals(selected) ? "ALL" : selected);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(userList);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUI(new GlassUtils.GlassScrollBarUI());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Frosted glass sidebar wash
        g2.setColor(new Color(255, 255, 255, 12));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }

    /**
     * Wrapper panel for list items to render smooth glass highlight pills when selected.
     */
    private static class GlassListCellWrapper extends JPanel {
        private final boolean isSelected;

        public GlassListCellWrapper(Component comp, boolean isSelected) {
            super(new BorderLayout());
            this.isSelected = isSelected;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            add(comp, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (isSelected) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Glass active item pill
                g2.setColor(new Color(99, 102, 241, 85));
                g2.fillRoundRect(6, 2, getWidth() - 12, getHeight() - 4, 12, 12);
                g2.setColor(new Color(165, 180, 252, 140));
                g2.drawRoundRect(6, 2, getWidth() - 12, getHeight() - 4, 12, 12);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    public void updateUserList(String csvUserList, String currentUsername) {
        String currentlySelected = userList.getSelectedValue();
        listModel.clear();
        listModel.addElement(PUBLIC_CHAT);

        int count = 0;
        if (csvUserList != null && !csvUserList.trim().isEmpty()) {
            String[] users = csvUserList.split(",");
            for (String u : users) {
                String trimmed = u.trim();
                if (!trimmed.isEmpty() && !trimmed.equalsIgnoreCase(currentUsername)) {
                    listModel.addElement(trimmed);
                    presenceStatus.put(trimmed, true);
                    count++;
                }
            }
        }

        headerLabel.setText("Contacts (" + count + " Online)");

        if (currentlySelected != null && listModel.contains(currentlySelected)) {
            userList.setSelectedValue(currentlySelected, true);
        } else {
            userList.setSelectedIndex(0);
        }
    }

    public void updateUserPresence(String username, String status) {
        if (username == null) return;
        boolean isOnline = "ONLINE".equalsIgnoreCase(status);
        presenceStatus.put(username, isOnline);

        if (!listModel.contains(username) && isOnline) {
            listModel.addElement(username);
        }

        long onlineCount = presenceStatus.values().stream().filter(Boolean::booleanValue).count();
        headerLabel.setText("Contacts (" + onlineCount + " Online)");
        userList.repaint();
    }

    public void setUnreadCount(String sender, int count) {
        if (count > 0) {
            unreadCounts.put(sender, count);
        } else {
            unreadCounts.remove(sender);
        }
        if (!listModel.contains(sender)) {
            listModel.addElement(sender);
        }
        userList.repaint();
    }

    public void setOnUserSelected(Consumer<String> onUserSelected) {
        this.onUserSelected = onUserSelected;
    }

    public String getSelectedRecipient() {
        String val = userList.getSelectedValue();
        return (val == null || PUBLIC_CHAT.equals(val)) ? "ALL" : val;
    }
}
