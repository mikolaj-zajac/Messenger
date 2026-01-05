package gui;

import model.User;
import service.LoggerService;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GroupChatFrame extends JFrame {
    private User currentUser;
    private String groupName;

    private JTextArea messagesArea;
    private JTextArea messageInput;
    private javax.swing.Timer refreshTimer;
    private static final String GROUP_MESSAGES_FILE = "Messenger/data/groups/messages.txt";
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    public GroupChatFrame(User currentUser, String groupName) {
        this.currentUser = currentUser;
        this.groupName = groupName;

        setTitle("Grupa: " + groupName);
        setSize(600, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        initUI();
        loadMessages();
        startAutoRefresh();
        setVisible(true);
    }

    private void initUI() {
        // Główny panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Nagłówek
        JLabel titleLabel = new JLabel("Grupa: " + groupName, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Obszar wiadomości
        messagesArea = new JTextArea();
        messagesArea.setEditable(false);
        messagesArea.setFont(new Font("Arial", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(messagesArea);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Panel wprowadzania
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));

        messageInput = new JTextArea(3, 30);
        messageInput.setFont(new Font("Arial", Font.PLAIN, 12));

        JButton sendButton = new JButton("Wyślij");
        sendButton.addActionListener(e -> sendMessage());

        inputPanel.add(new JScrollPane(messageInput), BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void sendMessage() {
        String text = messageInput.getText().trim();
        if (!text.isEmpty()) {
            String timestamp = timeFormat.format(new Date());
            String messageLine = timestamp + " | " + currentUser.getUsername() + ": " + text;

            // Wyświetl w oknie
            messagesArea.append(messageLine + "\n");

            // Zapisz do pliku
            saveMessage(messageLine);

            // Wyczyść pole
            messageInput.setText("");

            LoggerService.write("Grupa '" + groupName + "': " + currentUser.getUsername() + " napisał: " + text);
        }
    }

    private void saveMessage(String message) {
        try {
            // Utwórz katalog jeśli nie istnieje
            new File("Messenger/data/groups").mkdirs();

            // Zapisz do pliku z nazwą grupy
            String filename = "Messenger/data/groups/" + groupName + ".txt";
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true))) {
                writer.write(message);
                writer.newLine();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Błąd zapisu: " + e.getMessage());
        }
    }

    private void loadMessages() {
        String filename = "Messenger/data/groups/" + groupName + ".txt";
        File file = new File(filename);

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                messagesArea.setText(""); // Wyczyść
                String line;
                while ((line = reader.readLine()) != null) {
                    messagesArea.append(line + "\n");
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Błąd odczytu: " + e.getMessage());
            }
        }
    }

    private void startAutoRefresh() {
        // Odświeżaj co 2 sekundy
        refreshTimer = new javax.swing.Timer(2000, e -> checkForNewMessages());
        refreshTimer.start();
    }

    private void checkForNewMessages() {
        String filename = "Messenger/data/groups/" + groupName + ".txt";
        File file = new File(filename);

        if (file.exists()) {
            long lastModified = file.lastModified();
            long currentTime = System.currentTimeMillis();

            // Jeśli plik był modyfikowany w ciągu ostatnich 5 sekund
            if (currentTime - lastModified < 5000) {
                loadMessages();
            }
        }
    }

    @Override
    public void dispose() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        super.dispose();
    }
}