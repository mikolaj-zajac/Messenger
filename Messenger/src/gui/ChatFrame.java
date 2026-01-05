package gui;

import model.User;
import service.LoggerService;
import network.ChatClient;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ChatFrame extends JFrame {
    private User currentUser;
    private JPanel usersPanel = new JPanel();
    private static final String USERS_FILE = "Messenger/data/users.txt";
    private ChatClient chatClient;
    private JLabel statusLabel;
    private javax.swing.Timer refreshTimer;

    public ChatFrame(User user) {
        this.currentUser = user;

        setTitle("Messenger - " + user.getUsername());
        setSize(400, 550);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
        setVisible(true);

        // Inicjalizacja połączenia sieciowego
        initNetworkConnection();

        // Załaduj początkową listę użytkowników
        if (chatClient != null && chatClient.isConnected()) {
            loadOnlineUsers();
        } else {
            loadUsersFromFile();
        }

        // Timer do odświeżania listy online co 30 sekund
        startAutoRefresh();
    }

    private void initNetworkConnection() {
        // Tymczasowo wyłączone - dopóki nie masz ChatClient.java

        chatClient = new ChatClient(new ChatClient.MessageListener() {
            @Override
            public void onMessageReceived(String from, String content) {
                SwingUtilities.invokeLater(() -> {
                    showNewMessageNotification(from, content);
                });
            }

            @Override
            public void onOnlineUsersUpdated(List<String> users) {
                SwingUtilities.invokeLater(() -> {
                    updateOnlineUsersList(users);
                });
            }

            @Override
            public void onConnectionStatusChanged(boolean connected) {
                SwingUtilities.invokeLater(() -> {
                    updateConnectionStatus(connected);
                });
            }

            @Override
            public void onGroupMessageReceived(String groupName, String from, String content) {
                SwingUtilities.invokeLater(() -> {
                    showNewGroupMessageNotification(groupName, from, content);
                });
            }
        });

        // Próba połączenia z serwerem
        String serverAddress = System.getProperty("server.url", "localhost");
        int serverPort = Integer.parseInt(System.getProperty("server.port", "8080"));

        boolean connected = chatClient.connect(
            serverAddress,
            serverPort,
            currentUser.getUsername(),
            currentUser.getPassword()
        );

        if (!connected) {
            JOptionPane.showMessageDialog(this,
                "Nie można połączyć z serwerem czatu.\n" +
                "Działanie w trybie offline - tylko czat prywatny lokalnie.",
                "Tryb offline",
                JOptionPane.WARNING_MESSAGE);
        }


        // Tymczasowy status
//        statusLabel.setText("🔴 Tryb offline (brak ChatClient.java)");
//        statusLabel.setForeground(Color.RED);
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.gridx = 0;
        c.weightx = 1.0;

        // Panel statusu
        statusLabel = new JLabel("🔴 Rozłączony");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 12));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setForeground(Color.RED);

        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0;
        mainPanel.add(statusLabel, c);

        JLabel title = new JLabel("Wybierz użytkownika do czatu");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setHorizontalAlignment(SwingConstants.CENTER);

        c.gridy = 1;
        mainPanel.add(title, c);

        // Panel z przyciskami użytkowników
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

    private void updateConnectionStatus(boolean connected) {
        if (connected) {
            statusLabel.setText("🟢 Połączony z serwerem");
            statusLabel.setForeground(new Color(34, 139, 34));
        } else {
            statusLabel.setText("🔴 Rozłączony - tryb offline");
            statusLabel.setForeground(Color.RED);
        }
    }

    private void updateOnlineUsersList(List<String> onlineUsers) {
        usersPanel.removeAll();

        // Dodaj nagłówek
        JLabel onlineHeader = new JLabel("🟢 Użytkownicy online (" + onlineUsers.size() + ")");
        onlineHeader.setFont(new Font("Arial", Font.BOLD, 14));
        onlineHeader.setForeground(new Color(34, 139, 34));
        onlineHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        usersPanel.add(onlineHeader);
        usersPanel.add(Box.createVerticalStrut(10));

        // Dodaj użytkowników online
        boolean hasOnlineUsers = false;
        for (String username : onlineUsers) {
            if (!username.equals(currentUser.getUsername())) {
                addUserButton(username, true);
                hasOnlineUsers = true;
            }
        }

        if (!hasOnlineUsers) {
            JLabel noUsersLabel = new JLabel("Brak innych użytkowników online");
            noUsersLabel.setFont(new Font("Arial", Font.ITALIC, 12));
            noUsersLabel.setForeground(Color.GRAY);
            noUsersLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            usersPanel.add(noUsersLabel);
        }

        usersPanel.add(Box.createVerticalStrut(20));

        // Dodaj grupy z pliku
        loadGroupsFromFile();

        usersPanel.revalidate();
        usersPanel.repaint();
    }

    private void addUserButton(String username, boolean online) {
        JButton userBtn = new JButton();
        userBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        userBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        userBtn.setFocusPainted(false);
        userBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        if (online) {
            userBtn.setText("🟢 " + username);
            userBtn.setBackground(new Color(240, 248, 255));
            userBtn.setForeground(Color.BLACK);
            userBtn.addActionListener(e -> {
                if (chatClient != null && chatClient.isConnected()) {
                    new PrivateChatFrame(currentUser, username, chatClient);
                } else {
                    new PrivateChatFrame(currentUser, username);
                }
            });
        } else {
            userBtn.setText("⚫ " + username);
            userBtn.setBackground(new Color(245, 245, 245));
            userBtn.setForeground(Color.GRAY);
            userBtn.setEnabled(false);
        }

        userBtn.setOpaque(true);
        userBtn.setBorderPainted(false);

        usersPanel.add(userBtn);
        usersPanel.add(Box.createVerticalStrut(5));
    }

    private void loadGroupsFromFile() {
        // Nagłówek grup
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

                        // Podpis z członkami
                        String membersText = "Członkowie: " + String.join(", ", members);
                        groupBtn.setToolTipText(membersText);

                        groupBtn.addActionListener(e -> {
                            if (chatClient != null && chatClient.isConnected()) {
                                new GroupChatFrame(currentUser, groupName, chatClient);
                            } else {
                                new GroupChatFrame(currentUser, groupName, chatClient);
                            }
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

        // Nagłówek wszystkich użytkowników
        JLabel allUsersHeader = new JLabel("👤 Wszyscy użytkowników");
        allUsersHeader.setFont(new Font("Arial", Font.BOLD, 14));
        allUsersHeader.setForeground(Color.BLACK);
        allUsersHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        usersPanel.add(allUsersHeader);
        usersPanel.add(Box.createVerticalStrut(10));

        try (BufferedReader br = new BufferedReader(new FileReader(USERS_FILE))) {
            List<String> allUsers = new ArrayList<>();

            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("group:")) {
                    String username = line.split(";")[0];
                    if (!username.equals(currentUser.getUsername())) {
                        allUsers.add(username);
                    }
                }
            }

            if (allUsers.isEmpty()) {
                JLabel noUsersLabel = new JLabel("Brak innych użytkowników");
                noUsersLabel.setFont(new Font("Arial", Font.ITALIC, 12));
                noUsersLabel.setForeground(Color.GRAY);
                noUsersLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                usersPanel.add(noUsersLabel);
            } else {
                for (String username : allUsers) {
                    addUserButton(username, false);
                }
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

    private void loadOnlineUsers() {
        if (chatClient != null && chatClient.isConnected()) {
            chatClient.requestOnlineUsers();
        } else {
            loadUsersFromFile();
        }
    }

    private void refreshUserList() {
        if (chatClient != null && chatClient.isConnected()) {
            loadOnlineUsers();
        } else {
            loadUsersFromFile();
        }
    }

    public void refresh() {
        refreshUserList();
    }

    private void startAutoRefresh() {
        refreshTimer = new javax.swing.Timer(30000, e -> refreshUserList());
        refreshTimer.start();
    }

    private void showNewMessageNotification(String from, String content) {
        // Pokazuje powiadomienie o nowej wiadomości
        if (!isActive()) {
            Toolkit.getDefaultToolkit().beep();

            JDialog notification = new JDialog(this, "Nowa wiadomość", false);
            notification.setLayout(new BorderLayout());
            notification.setSize(300, 150);
            notification.setLocationRelativeTo(this);

            JLabel messageLabel = new JLabel("<html><b>" + from + "</b>:<br/>" +
                    (content.length() > 50 ? content.substring(0, 50) + "..." : content) + "</html>");
            messageLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            JButton openBtn = new JButton("Otwórz czat");
            openBtn.addActionListener(e -> {
                notification.dispose();
                new PrivateChatFrame(currentUser, from, chatClient);
            });

            notification.add(messageLabel, BorderLayout.CENTER);
            notification.add(openBtn, BorderLayout.SOUTH);
            notification.setVisible(true);
        }
    }

    private void showNewGroupMessageNotification(String groupName, String from, String content) {
        // Pokazuje powiadomienie o nowej wiadomości w grupie
        if (!isActive()) {
            Toolkit.getDefaultToolkit().beep();

            JDialog notification = new JDialog(this, "Nowa wiadomość w grupie", false);
            notification.setLayout(new BorderLayout());
            notification.setSize(350, 150);
            notification.setLocationRelativeTo(this);

            JLabel messageLabel = new JLabel("<html><b>" + from + "</b> w <b>" + groupName + "</b>:<br/>" +
                    (content.length() > 50 ? content.substring(0, 50) + "..." : content) + "</html>");
            messageLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            JButton openBtn = new JButton("Otwórz grupę");
            openBtn.addActionListener(e -> {
                notification.dispose();
                if (chatClient != null && chatClient.isConnected()) {
                    new GroupChatFrame(currentUser, groupName, chatClient);
                } else {
                    new GroupChatFrame(currentUser, groupName, chatClient);
                }
            });

            notification.add(messageLabel, BorderLayout.CENTER);
            notification.add(openBtn, BorderLayout.SOUTH);
            notification.setVisible(true);
        }
    }

    private void logout() {
        // Zatrzymaj timer
        if (refreshTimer != null) {
            refreshTimer.stop();
        }

        // Zamknij połączenie sieciowe
        if (chatClient != null) {
            chatClient.disconnect();
        }

        LoggerService.write("Użytkownik " + currentUser.getUsername() + " wylogował się");

        dispose();
        new LoginFrame();
    }

    @Override
    public void dispose() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        if (chatClient != null) {
            chatClient.disconnect();
        }
        super.dispose();
    }
}