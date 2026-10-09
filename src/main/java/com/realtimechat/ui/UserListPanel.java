package com.realtimechat.ui;

import javax.swing.BorderFactory;
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
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.DefaultListCellRenderer;

/**
 * Sidebar panel displaying online users with presence status and unread badges.
 * Supports selective real-time presence updates and unread counts.
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
        setPreferredSize(new Dimension(230, 0));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(220, 224, 230)));
        setBackground(new Color(245, 247, 250));

        // Header
        headerLabel = new JLabel("Contacts (0 Online)");
        headerLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        add(headerLabel, BorderLayout.NORTH);

        // List Model & JList
        listModel = new DefaultListModel<>();
        listModel.addElement(PUBLIC_CHAT);

        userList = new JList<>(listModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setSelectedIndex(0);
        userList.setFont(new Font("SansSerif", Font.PLAIN, 13));
        userList.setBackground(new Color(245, 247, 250));
        userList.setFixedCellHeight(38);

        // Custom list item renderer with presence dots and unread badges
        userList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, 
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
                String username = (String) value;

                if (PUBLIC_CHAT.equals(username)) {
                    label.setText(username);
                } else {
                    boolean online = presenceStatus.getOrDefault(username, true);
                    String statusDot = online ? "🟢 " : "⚪ ";
                    int unread = unreadCounts.getOrDefault(username, 0);
                    String badge = (unread > 0) ? " (" + unread + " unread)" : "";
                    label.setText(statusDot + username + badge);
                }

                if (isSelected) {
                    label.setBackground(new Color(214, 234, 248));
                    label.setForeground(new Color(21, 67, 96));
                } else {
                    label.setBackground(new Color(245, 247, 250));
                    label.setForeground(Color.BLACK);
                }
                return label;
            }
        });

        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = userList.getSelectedValue();
                if (selected != null) {
                    // Clear unread count when clicking conversation
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
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Updates full user list from server CSV.
     */
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

    /**
     * Updates presence for a specific user (ONLINE / OFFLINE) from PRESENCE message.
     */
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

    /**
     * Sets or increments unread badge count for a sender.
     */
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
