package gui;

import model.User;
import network.ChatClient;
import service.LoggerService;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PrivateChatFrame extends JFrame {
    private User currentUser;
    private String receiver;
    private ChatClient chatClient;

    private JTextPane messagesArea; // ZMIENIONE: JTextPane zamiast JTextArea
    private JTextArea messageInput;
    private JButton sendButton;
    private JLabel statusLabel;
    private static final String CHAT_DIR = "data/chats/";

    public PrivateChatFrame(User currentUser, String receiver, ChatClient chatClient) {
        this.currentUser = currentUser;
        this.receiver = receiver;
        this.chatClient = chatClient;
        initUI();
        loadChatHistory();
        setVisible(true);
    }

    public PrivateChatFrame(User currentUser, String receiver) {
        this(currentUser, receiver, null);
    }

    private void initUI() {
        setTitle("Czat z " + receiver);
        setSize(600, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(new Color(240, 240, 240));

        JPanel topPanel = createTopPanel();
        mainPanel.add(topPanel, BorderLayout.NORTH);

        JPanel messagesPanel = createMessagesPanel();
        mainPanel.add(messagesPanel, BorderLayout.CENTER);

        JPanel inputPanel = createInputPanel();
        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        add(mainPanel);
        messageInput.requestFocus();
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBackground(Color.WHITE);
        topPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JButton backBtn = new JButton("← Powrót");
        backBtn.setFont(new Font("Arial", Font.PLAIN, 12));
        backBtn.setFocusPainted(false);
        backBtn.setBackground(new Color(240, 240, 240));
        backBtn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        backBtn.addActionListener(e -> dispose());

        JLabel chatInfoLabel = new JLabel();
        chatInfoLabel.setFont(new Font("Arial", Font.BOLD, 16));

        if (chatClient != null && chatClient.isConnected()) {
            chatInfoLabel.setText("🟢 Rozmowa z: " + receiver);
            chatInfoLabel.setForeground(new Color(34, 139, 34));
        } else {
            chatInfoLabel.setText("⚫ Rozmowa z: " + receiver + " (offline)");
            chatInfoLabel.setForeground(Color.DARK_GRAY);
        }

        chatInfoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        statusLabel = new JLabel();
        updateStatusLabel();

        topPanel.add(backBtn, BorderLayout.WEST);
        topPanel.add(chatInfoLabel, BorderLayout.CENTER);
        topPanel.add(statusLabel, BorderLayout.EAST);

        return topPanel;
    }

    private JPanel createMessagesPanel() {
        JPanel messagesPanel = new JPanel(new BorderLayout());
        messagesPanel.setBackground(Color.WHITE);
        messagesPanel.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));

        // Używamy JTextPane zamiast JTextArea dla formatowania
        messagesArea = new JTextPane();
        messagesArea.setEditable(false);
        messagesArea.setFont(new Font("Arial", Font.PLAIN, 14));
        messagesArea.setBackground(new Color(250, 250, 250));
        messagesArea.setMargin(new Insets(10, 10, 10, 10));

        // Ustawienie edytora tekstu dla lepszego formatowania
        StyledDocument doc = messagesArea.getStyledDocument();

        // Styl dla normalnego tekstu
        Style defaultStyle = doc.addStyle("default", null);
        StyleConstants.setFontFamily(defaultStyle, "Arial");
        StyleConstants.setFontSize(defaultStyle, 14);

        JScrollPane messagesScroll = new JScrollPane(messagesArea);
        messagesScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        messagesScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        messagesScroll.getVerticalScrollBar().setUnitIncrement(16);
        messagesScroll.setBorder(null);

        messagesPanel.add(messagesScroll, BorderLayout.CENTER);

        return messagesPanel;
    }

    private JPanel createInputPanel() {
        JPanel inputPanel = new JPanel(new BorderLayout(10, 10));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        messageInput = new JTextArea(3, 40);
        messageInput.setLineWrap(true);
        messageInput.setWrapStyleWord(true);
        messageInput.setFont(new Font("Arial", Font.PLAIN, 14));
        messageInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        messageInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown()) {
                    e.consume();
                    sendMessage();
                }
            }
        });

        JScrollPane inputScroll = new JScrollPane(messageInput);
        inputScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        inputScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        inputScroll.setBorder(null);

        sendButton = new JButton("Wyślij");
        sendButton.setFont(new Font("Arial", Font.BOLD, 14));
        sendButton.setFocusPainted(false);
        sendButton.setPreferredSize(new Dimension(100, 50));
        sendButton.setBackground(new Color(70, 130, 180));
        sendButton.setForeground(Color.WHITE);
        sendButton.setOpaque(true);
        sendButton.setBorderPainted(false);
        sendButton.addActionListener(e -> sendMessage());

        getRootPane().setDefaultButton(sendButton);

        inputPanel.add(inputScroll, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        return inputPanel;
    }

    private void updateStatusLabel() {
        if (chatClient != null && chatClient.isConnected()) {
            statusLabel.setText("🟢 Online");
            statusLabel.setForeground(new Color(34, 139, 34));
            sendButton.setEnabled(true);
            sendButton.setBackground(new Color(70, 130, 180));
        } else {
            statusLabel.setText("🔴 Offline");
            statusLabel.setForeground(Color.RED);
            sendButton.setEnabled(true);
            sendButton.setBackground(new Color(169, 169, 169));
        }
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
    }

    private void sendMessage() {
        String content = messageInput.getText().trim();
        if (content.isEmpty()) {
            return;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        if (chatClient != null && chatClient.isConnected()) {
            chatClient.sendPrivateMessage(receiver, content);
            appendMessageToUI("Ty", content, timestamp, true);
            saveMessageToFile(currentUser.getUsername(), receiver, content, timestamp);

            LoggerService.write("Wiadomość od " + currentUser.getUsername() +
                    " do " + receiver + ": " + content);
        } else {
            appendMessageToUI("Ty", content, timestamp, true);
            saveMessageToFile(currentUser.getUsername(), receiver, content, timestamp);

            JOptionPane.showMessageDialog(this,
                    "Wiadomość zapisana lokalnie.\n" +
                            "Odbiorca otrzyma ją gdy oboje będziecie online.",
                    "Tryb offline",
                    JOptionPane.INFORMATION_MESSAGE);
        }

        messageInput.setText("");
        messageInput.requestFocus();
        scrollToBottom();
    }

    private void appendMessageToUI(String sender, String content, String timestamp, boolean isOwnMessage) {
        SwingUtilities.invokeLater(() -> {
            try {
                StyledDocument doc = messagesArea.getStyledDocument();

                // Dodaj pustą linię między wiadomościami (opcjonalnie)
                if (doc.getLength() > 0) {
                    doc.insertString(doc.getLength(), "\n", null);
                }

                // Styl dla nadawcy i czasu
                Style senderStyle = doc.addStyle("senderStyle", null);
                if (isOwnMessage) {
                    StyleConstants.setForeground(senderStyle, new Color(18, 140, 126)); // Zielony jak WhatsApp
                } else {
                    StyleConstants.setForeground(senderStyle, new Color(7, 94, 84)); // Ciemniejszy zielony
                }
                StyleConstants.setBold(senderStyle, true);
                StyleConstants.setFontSize(senderStyle, 14);

                // Styl dla czasu
                Style timeStyle = doc.addStyle("timeStyle", null);
                StyleConstants.setForeground(timeStyle, Color.GRAY);
                StyleConstants.setFontSize(timeStyle, 11);
                StyleConstants.setItalic(timeStyle, true);

                // Styl dla treści wiadomości
                Style contentStyle = doc.addStyle("contentStyle", null);
                StyleConstants.setForeground(contentStyle, Color.BLACK);
                StyleConstants.setFontSize(contentStyle, 14);

                // Dodaj nadawcę
                String senderText = sender + " ";
                doc.insertString(doc.getLength(), senderText, senderStyle);

                // Dodaj czas
                doc.insertString(doc.getLength(), timestamp + "\n", timeStyle);

                // Dodaj treść wiadomości
                doc.insertString(doc.getLength(), content + "\n", contentStyle);

                // Dodaj separator (opcjonalnie)
                Style separatorStyle = doc.addStyle("separatorStyle", null);
                StyleConstants.setForeground(separatorStyle, new Color(220, 220, 220));
                doc.insertString(doc.getLength(), "----------------------------------------\n", separatorStyle);

                scrollToBottom();

            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            messagesArea.setCaretPosition(messagesArea.getDocument().getLength());
        });
    }

    private void saveMessageToFile(String from, String to, String content, String timestamp) {
        File chatDir = new File(CHAT_DIR);
        if (!chatDir.exists()) {
            chatDir.mkdirs();
        }

        String fileName = getChatFileName(from, to);
        File chatFile = new File(CHAT_DIR + fileName);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(chatFile, true))) {
            writer.write(timestamp + ";" + from + ";" + to + ";" + content);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Błąd zapisu wiadomości: " + e.getMessage());
        }
    }

    private void loadChatHistory() {
        String fileName = getChatFileName(currentUser.getUsername(), receiver);
        File chatFile = new File(CHAT_DIR + fileName);

        if (!chatFile.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(chatFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";", 4);
                if (parts.length == 4) {
                    String timestamp = parts[0];
                    String sender = parts[1];
                    String recipient = parts[2];
                    String content = parts[3];

                    boolean isOwnMessage = sender.equals(currentUser.getUsername());
                    appendMessageToUI(sender, content, timestamp, isOwnMessage);
                }
            }
        } catch (IOException e) {
            System.err.println("Błąd wczytywania historii: " + e.getMessage());
        }
    }

    private String getChatFileName(String user1, String user2) {
        if (user1.compareTo(user2) < 0) {
            return user1 + "_" + user2 + ".txt";
        } else {
            return user2 + "_" + user1 + ".txt";
        }
    }

    public void receiveMessage(String from, String content) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        appendMessageToUI(from, content, timestamp, false);
        saveMessageToFile(from, currentUser.getUsername(), content, timestamp);

        Toolkit.getDefaultToolkit().beep();
    }

    @Override
    public void dispose() {
        if (chatClient != null) {
            // Opcjonalnie: powiadom serwer o opuszczeniu czatu
        }
        super.dispose();
    }
}