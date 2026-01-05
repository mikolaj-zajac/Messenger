package gui;

import model.User;
import service.LoggerService;
import service.UserService;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PrivateChatFrame extends JFrame {
    private User currentUser;
    private String otherUser;
    private UserService userService;

    private JTextArea chatArea;
    private JTextField messageField;
    private javax.swing.Timer refreshTimer;
    private javax.swing.Timer statusTimer;
    private static final String MESSAGES_DIR = "Messenger/data/private/";
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private JLabel statusLabel;

    public PrivateChatFrame(User currentUser, String otherUser) {
        this.currentUser = currentUser;
        this.otherUser = otherUser;
        this.userService = new UserService();

        setTitle("Czat z " + otherUser);
        setSize(500, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        initUI();
        loadMessages();
        startAutoRefresh();
        startStatusChecker();
        updateOwnOnlineStatus();
        setVisible(true);
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Górny panel z nazwą i statusem
        JPanel topPanel = new JPanel(new BorderLayout());

        JButton backBtn = new JButton("←");
        backBtn.setFocusPainted(false);
        backBtn.addActionListener(e -> dispose());

        statusLabel = new JLabel(otherUser, SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        updateStatusLabel();

        JButton refreshBtn = new JButton("🔄");
        refreshBtn.setToolTipText("Odśwież");
        refreshBtn.setFocusPainted(false);
        refreshBtn.addActionListener(e -> {
            loadMessages();
            updateStatusLabel();
        });

        topPanel.add(backBtn, BorderLayout.WEST);
        topPanel.add(statusLabel, BorderLayout.CENTER);
        topPanel.add(refreshBtn, BorderLayout.EAST);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Obszar czatu
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 12));
        chatArea.setBackground(new Color(250, 250, 250));

        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Panel wprowadzania
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));

        messageField = new JTextField();
        messageField.setFont(new Font("Arial", Font.PLAIN, 12));
        messageField.addActionListener(e -> sendMessage());

        JButton sendButton = new JButton("Wyślij");
        sendButton.setFocusPainted(false);
        sendButton.setBackground(new Color(70, 130, 180));
        sendButton.setForeground(Color.WHITE);
        sendButton.addActionListener(e -> sendMessage());

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void updateStatusLabel() {
        boolean isOnline = userService.isUserOnline(otherUser);

        if (isOnline) {
            statusLabel.setText("🟢 " + otherUser + " (online)");
            statusLabel.setForeground(new Color(0, 150, 0));
            statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        } else {
            statusLabel.setText("⚫ " + otherUser + " (offline)");
            statusLabel.setForeground(Color.GRAY);
            statusLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        }
    }

    private void updateOwnOnlineStatus() {
        userService.updateUserOnlineStatus(currentUser.getUsername());
    }

    private void sendMessage() {
        String text = messageField.getText().trim();
        if (!text.isEmpty()) {
            String timestamp = timeFormat.format(new Date());
            String from = currentUser.getUsername();
            String to = otherUser;

            // Format wiadomości do zapisu
            String messageData = timestamp + "|" + from + "|" + to + "|" + text;

            // Wyświetl w oknie
            chatArea.append("[" + timestamp + "] Ja: " + text + "\n");

            // Zapisz do pliku
            saveMessage(messageData);

            // Wyczyść pole
            messageField.setText("");

            // Zaktualizuj swój status online
            updateOwnOnlineStatus();

            LoggerService.write("Prywatnie: " + currentUser.getUsername() + " -> " + otherUser + ": " + text);

            // Przewiń na dół
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        }
    }

    private void saveMessage(String messageData) {
        try {
            new File(MESSAGES_DIR).mkdirs();
            String filename = getChatFilename(currentUser.getUsername(), otherUser);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true))) {
                writer.write(messageData);
                writer.newLine();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Błąd zapisu: " + e.getMessage());
        }
    }

    private String getChatFilename(String user1, String user2) {
        String[] users = {user1, user2};
        java.util.Arrays.sort(users);
        return MESSAGES_DIR + users[0] + "_" + users[1] + ".txt";
    }

    private void loadMessages() {
        String filename = getChatFilename(currentUser.getUsername(), otherUser);
        File file = new File(filename);

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                chatArea.setText("");
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\|", 4);
                    if (parts.length == 4) {
                        String time = parts[0];
                        String from = parts[1];
                        String to = parts[2];
                        String text = parts[3];

                        String displayName = from.equals(currentUser.getUsername()) ? "Ja" : otherUser;
                        chatArea.append("[" + time + "] " + displayName + ": " + text + "\n");
                    }
                }
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Błąd odczytu: " + e.getMessage());
            }
        }
    }

    private void startAutoRefresh() {
        refreshTimer = new javax.swing.Timer(2000, e -> {
            checkForNewMessages();
            updateOwnOnlineStatus(); // Zawsze aktualizuj swój status gdy okno jest otwarte
        });
        refreshTimer.start();
    }

    private void startStatusChecker() {
        statusTimer = new javax.swing.Timer(3000, e -> updateStatusLabel());
        statusTimer.start();
    }

    private void checkForNewMessages() {
        String filename = getChatFilename(currentUser.getUsername(), otherUser);
        File file = new File(filename);

        if (file.exists()) {
            long lastModified = file.lastModified();
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastModified < 3000) {
                loadMessages();

                if (!isActive()) {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        }
    }

    @Override
    public void dispose() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        if (statusTimer != null) {
            statusTimer.stop();
        }
        super.dispose();
    }
}