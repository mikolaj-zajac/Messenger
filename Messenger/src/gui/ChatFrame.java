package gui;

import model.User;
import service.LoggerService;
import service.SimpleChatClient;
import service.UserService;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class ChatFrame extends JFrame {
    private User currentUser;
    private JPanel usersPanel = new JPanel();
    private static final String USERS_FILE = "Messenger/data/users.txt";
    private JLabel connectionStatusLabel;
    private javax.swing.Timer refreshTimer;
    private javax.swing.Timer onlineTimer;
    private UserService userService;
    private JLabel onlineCountLabel;
    private SimpleChatClient networkClient;

    public ChatFrame(User user) {
        this.currentUser = user;
        this.userService = new UserService();

        setTitle("Messenger - " + user.getUsername());
        setSize(450, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Próba automatycznego połączenia z serwerem
        networkClient = new SimpleChatClient();
        boolean connected = networkClient.connect(user.getUsername());

        initUI();

        if (connected) {
            connectionStatusLabel.setText("🟢 Połączono z serwerem");
            connectionStatusLabel.setForeground(new Color(0, 150, 0));
            System.out.println("Połączono z serwerem!");
        } else {
            connectionStatusLabel.setText("🔴 Tryb lokalny");
            connectionStatusLabel.setForeground(Color.RED);
            networkClient = null; // Tryb lokalny
            System.out.println("Uruchomiono w trybie lokalnym");
        }

        loadUsersFromFile();
        startAutoRefresh();
        startOnlineUpdater();

        // Zaktualizuj swój status online (nawet w trybie lokalnym)
        userService.updateUserOnlineStatus(currentUser.getUsername());

        setVisible(true);
    }

    private void startOnlineUpdater() {
        // Co 10 sekund aktualizuj swój status online
        onlineTimer = new javax.swing.Timer(10000, e -> {
            userService.updateUserOnlineStatus(currentUser.getUsername());
            refreshUserList();
        });
        onlineTimer.start();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.gridx = 0;
        c.weightx = 1.0;

        // Panel statusu połączenia
        connectionStatusLabel = new JLabel("Łączenie...", SwingConstants.CENTER);
        connectionStatusLabel.setFont(new Font("Arial", Font.BOLD, 12));

        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0;
        mainPanel.add(connectionStatusLabel, c);

        // Tytuł
        JLabel title = new JLabel("Wybierz użytkownika do czatu");
        title.setFont(new Font("Arial", Font.BOLD, 16));
        title.setHorizontalAlignment(SwingConstants.CENTER);

        c.gridy = 1;
        mainPanel.add(title, c);

        // Panel z użytkownikami
        usersPanel.setLayout(new BoxLayout(usersPanel, BoxLayout.Y_AXIS));
        usersPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        usersPanel.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(usersPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        c.gridy = 2;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1.0;
        mainPanel.add(scrollPane, c);

        // Panel przycisków akcji
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JButton createGroupBtn = new JButton("👥 Utwórz grupę");
        createGroupBtn.setFont(new Font("Arial", Font.PLAIN, 14));
        createGroupBtn.setFocusPainted(false);
        createGroupBtn.setBackground(new Color(70, 130, 180));
        createGroupBtn.setForeground(Color.WHITE);
        createGroupBtn.setOpaque(true);
        createGroupBtn.setBorderPainted(false);
        createGroupBtn.addActionListener(e -> new CreateGroupFrame(currentUser, this));

        JButton refreshBtn = new JButton("🔄 Odśwież");
        refreshBtn.setFont(new Font("Arial", Font.PLAIN, 14));
        refreshBtn.setFocusPainted(false);
        refreshBtn.setBackground(new Color(60, 179, 113));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setOpaque(true);
        refreshBtn.setBorderPainted(false);
        refreshBtn.addActionListener(e -> refreshUserList());

        buttonPanel.add(createGroupBtn);
        buttonPanel.add(refreshBtn);

        c.gridy = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0;
        mainPanel.add(buttonPanel, c);

        // Przycisk wylogowania
        JButton logoutBtn = new JButton("🚪 Wyloguj");
        logoutBtn.setFont(new Font("Arial", Font.BOLD, 14));
        logoutBtn.setFocusPainted(false);
        logoutBtn.setBackground(new Color(220, 20, 60));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setOpaque(true);
        logoutBtn.setBorderPainted(false);
        logoutBtn.addActionListener(e -> logout());

        c.gridy = 4;
        mainPanel.add(logoutBtn, c);

        add(mainPanel);
    }

    private void addUserButton(String username, boolean online) {
        JButton userBtn = new JButton();
        userBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        userBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        userBtn.setFocusPainted(false);
        userBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        if (online) {
            userBtn.setText("🟢 " + username);
            userBtn.setBackground(new Color(220, 255, 220));
            userBtn.setForeground(Color.BLACK);
            userBtn.setFont(new Font("Arial", Font.BOLD, 13));
        } else {
            userBtn.setText("⚫ " + username);
            userBtn.setBackground(new Color(245, 245, 245));
            userBtn.setForeground(Color.GRAY);
            userBtn.setFont(new Font("Arial", Font.PLAIN, 13));
        }

        userBtn.addActionListener(e -> {
            new PrivateChatFrame(currentUser, username, networkClient);
        });

        userBtn.setOpaque(true);
        userBtn.setBorderPainted(false);

        usersPanel.add(userBtn);
        usersPanel.add(Box.createVerticalStrut(5));
    }

    private void loadGroupsFromFile() {
        JLabel groupsHeader = new JLabel("👥 Twoje grupy");
        groupsHeader.setFont(new Font("Arial", Font.BOLD, 14));
        groupsHeader.setForeground(new Color(138, 43, 226));
        groupsHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        usersPanel.add(groupsHeader);
        usersPanel.add(Box.createVerticalStrut(10));

        try (BufferedReader br = new BufferedReader(new FileReader(USERS_FILE))) {
            boolean hasGroups = false;

            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("group:")) {
                    String[] parts = line.substring(6).split(";");
                    String groupName = parts[0];
                    String[] members = parts[1].split(",");

                    boolean isMember = false;
                    for (String m : members) {
                        if (m.equals(currentUser.getUsername())) {
                            isMember = true;
                            break;
                        }
                    }

                    if (isMember) {
                        JButton groupBtn = new JButton("👥 " + groupName);
                        groupBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
                        groupBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
                        groupBtn.setFocusPainted(false);
                        groupBtn.setBackground(new Color(230, 230, 250));
                        groupBtn.setForeground(Color.BLACK);
                        groupBtn.setOpaque(true);
                        groupBtn.setBorderPainted(false);
                        groupBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

                        String membersText = "Członkowie: " + String.join(", ", members);
                        groupBtn.setToolTipText(membersText);

                        groupBtn.addActionListener(e -> {
                            new GroupChatFrame(currentUser, groupName);
                        });

                        usersPanel.add(groupBtn);
                        usersPanel.add(Box.createVerticalStrut(5));
                        hasGroups = true;
                    }
                }
            }

            if (!hasGroups) {
                JLabel noGroupsLabel = new JLabel("Nie należysz do żadnej grupy");
                noGroupsLabel.setFont(new Font("Arial", Font.ITALIC, 12));
                noGroupsLabel.setForeground(Color.GRAY);
                noGroupsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                usersPanel.add(noGroupsLabel);
            }

        } catch (IOException e) {
            JLabel errorLabel = new JLabel("Błąd ładowania grup");
            errorLabel.setFont(new Font("Arial", Font.ITALIC, 12));
            errorLabel.setForeground(Color.RED);
            errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            usersPanel.add(errorLabel);
        }
    }

    private void loadUsersFromFile() {
        usersPanel.removeAll();

        // Nagłówek użytkowników online
        List<String> onlineUsers = userService.getOnlineUsers();
        int onlineCount = 0;

        if (!onlineUsers.isEmpty()) {
            onlineCount = onlineUsers.size() - (onlineUsers.contains(currentUser.getUsername()) ? 1 : 0);

            if (onlineCount > 0) {
                JLabel onlineHeader = new JLabel("🟢 Online (" + onlineCount + ")");
                onlineHeader.setFont(new Font("Arial", Font.BOLD, 14));
                onlineHeader.setForeground(new Color(0, 150, 0));
                onlineHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
                usersPanel.add(onlineHeader);
                usersPanel.add(Box.createVerticalStrut(5));

                for (String username : onlineUsers) {
                    if (!username.equals(currentUser.getUsername())) {
                        addUserButton(username, true);
                    }
                }
            }
        }

        // Wszyscy użytkownicy (offline)
        try (BufferedReader br = new BufferedReader(new FileReader(USERS_FILE))) {
            List<String> allUsers = new ArrayList<>();
            List<String> offlineUsers = new ArrayList<>();

            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("group:")) {
                    String username = line.split(";")[0];
                    if (!username.equals(currentUser.getUsername())) {
                        allUsers.add(username);
                    }
                }
            }

            // Oddziel online od offline
            for (String username : allUsers) {
                boolean isOnline = onlineUsers.contains(username);
                if (!isOnline) {
                    offlineUsers.add(username);
                }
            }

            if (!offlineUsers.isEmpty()) {
                if (onlineCount > 0) {
                    usersPanel.add(Box.createVerticalStrut(10));
                }

                JLabel allUsersHeader = new JLabel("⚫ Wszyscy użytkownicy (" + offlineUsers.size() + ")");
                allUsersHeader.setFont(new Font("Arial", Font.BOLD, 14));
                allUsersHeader.setForeground(Color.BLACK);
                allUsersHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
                usersPanel.add(allUsersHeader);
                usersPanel.add(Box.createVerticalStrut(5));

                for (String username : offlineUsers) {
                    addUserButton(username, false);
                }
            }

            if (allUsers.isEmpty() && onlineCount == 0) {
                JLabel noUsersLabel = new JLabel("Brak innych użytkowników");
                noUsersLabel.setFont(new Font("Arial", Font.ITALIC, 12));
                noUsersLabel.setForeground(Color.GRAY);
                noUsersLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                usersPanel.add(noUsersLabel);
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Nie można wczytać listy użytkowników",
                    "Błąd",
                    JOptionPane.ERROR_MESSAGE);
        }

        usersPanel.add(Box.createVerticalStrut(20));
        loadGroupsFromFile();
        usersPanel.revalidate();
        usersPanel.repaint();
    }

    private void refreshUserList() {
        loadUsersFromFile();
    }

    public void refresh() {
        refreshUserList();
    }

    private void startAutoRefresh() {
        // Odświeżaj co 15 sekund
        refreshTimer = new javax.swing.Timer(15000, e -> refreshUserList());
        refreshTimer.start();
    }

    private void logout() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        if (onlineTimer != null) {
            onlineTimer.stop();
        }

        // Rozłącz z serwerem
        if (networkClient != null) {
            networkClient.disconnect();
        }

        // Usuń swój status online
        userService.removeUserOnlineStatus(currentUser.getUsername());

        LoggerService.write("Użytkownik " + currentUser.getUsername() + " wylogował się");
        dispose();
        new LoginFrame();
    }

    @Override
    public void dispose() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        if (onlineTimer != null) {
            onlineTimer.stop();
        }

        if (networkClient != null) {
            networkClient.disconnect();
        }

        userService.removeUserOnlineStatus(currentUser.getUsername());

        super.dispose();
    }
}