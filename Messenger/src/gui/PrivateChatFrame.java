package gui;

import model.User;
import service.LoggerService;
import service.SimpleChatClient;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class PrivateChatFrame extends JFrame {
    private User currentUser;
    private String otherUser;
    private SimpleChatClient networkClient;

    private JTextArea chatArea;
    private JTextField messageField;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private JLabel statusLabel;
    private Timer connectionCheckTimer;

    public PrivateChatFrame(User currentUser, String otherUser, SimpleChatClient networkClient) {
        this.currentUser = currentUser;
        this.otherUser = otherUser;
        this.networkClient = networkClient;

        setTitle("Czat z " + otherUser);
        setSize(500, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        initUI();

        if (networkClient != null && networkClient.isConnected()) {
            setupNetworkCallbacks();
            startConnectionChecker();
        }

        loadMessagesFromHistory();
        setVisible(true);
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Górny panel
        JPanel topPanel = new JPanel(new BorderLayout());

        JButton backBtn = new JButton("←");
        backBtn.setFocusPainted(false);
        backBtn.addActionListener(e -> dispose());

        statusLabel = new JLabel(otherUser, SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        updateConnectionStatus();

        JButton reconnectBtn = new JButton("↻");
        reconnectBtn.setToolTipText("Sprawdź połączenie");
        reconnectBtn.setFocusPainted(false);
        reconnectBtn.addActionListener(e -> checkConnection());

        topPanel.add(backBtn, BorderLayout.WEST);
        topPanel.add(statusLabel, BorderLayout.CENTER);
        topPanel.add(reconnectBtn, BorderLayout.EAST);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Obszar czatu
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 12));

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

    private void setupNetworkCallbacks() {
        networkClient.setPrivateMessageCallback(this::onPrivateMessage);
        networkClient.setHistoryCallback((from, message) -> {
            if (from.equals(otherUser) || from.equals(currentUser.getUsername())) {
                displayMessage(from, message);
            }
        });
    }

    private void startConnectionChecker() {
        connectionCheckTimer = new Timer();
        connectionCheckTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                checkConnection();
            }
        }, 0, 30000); // Co 30 sekund
    }

    private void checkConnection() {
        SwingUtilities.invokeLater(() -> {
            if (networkClient != null && networkClient.isConnected()) {
                statusLabel.setText("🟢 " + otherUser + " (online)");
                statusLabel.setForeground(new Color(0, 150, 0));
            } else {
                statusLabel.setText("🔴 " + otherUser + " (offline)");
                statusLabel.setForeground(Color.RED);
                chatArea.append("[System] Brak połączenia z serwerem\n");
            }
        });
    }

    private void updateConnectionStatus() {
        if (networkClient != null && networkClient.isConnected()) {
            statusLabel.setText("🟢 " + otherUser + " (online)");
            statusLabel.setForeground(new Color(0, 150, 0));
        } else {
            statusLabel.setText("⚫ " + otherUser + " (offline)");
            statusLabel.setForeground(Color.GRAY);
        }
    }

    private void sendMessage() {
        String text = messageField.getText().trim();
        if (!text.isEmpty()) {
            String timestamp = timeFormat.format(new Date());

            // Wyświetl lokalnie
            displayMessage(currentUser.getUsername(), text);

            // Wyślij przez serwer
            if (networkClient != null && networkClient.isConnected()) {
                networkClient.sendPrivateMessage(otherUser, text);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Brak połączenia z serwerem. Wiadomość nie została wysłana.",
                        "Błąd",
                        JOptionPane.WARNING_MESSAGE);
            }

            messageField.setText("");
            LoggerService.write(currentUser.getUsername() + " -> " + otherUser + ": " + text);
        }
    }

    private void onPrivateMessage(String from, String text) {
        if (from.equals(otherUser)) {
            SwingUtilities.invokeLater(() -> {
                displayMessage(from, text);

                // Powiadomienie
                if (!isActive()) {
                    Toolkit.getDefaultToolkit().beep();
                }
            });
        }
    }

    private void displayMessage(String from, String text) {
        String timestamp = timeFormat.format(new Date());
        String displayName = from.equals(currentUser.getUsername()) ? "Ja" : from;

        chatArea.append("[" + timestamp + "] " + displayName + ": " + text + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void loadMessagesFromHistory() {
        if (networkClient != null && networkClient.isConnected()) {
            // Wiadomości są już ładowane przez historyCallback
            chatArea.append("[System] Połączono z serwerem\n");
        } else {
            chatArea.append("[System] Tryb lokalny - tylko wiadomości offline\n");
        }
    }

    @Override
    public void dispose() {
        if (connectionCheckTimer != null) {
            connectionCheckTimer.cancel();
        }
        super.dispose();
        LoggerService.write("Zamknięto czat " + currentUser.getUsername() + " z " + otherUser);
    }
}