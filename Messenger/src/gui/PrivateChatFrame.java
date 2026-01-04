package gui;

import model.User;

import javax.swing.*;
import java.awt.*;

public class PrivateChatFrame extends JFrame {

    private User currentUser;
    private String receiver;

    private JTextArea messagesArea;
    private JTextArea messageInput;

    public PrivateChatFrame(User currentUser, String receiver) {
        this.currentUser = currentUser;
        this.receiver = receiver;

        setTitle("Czat: " + receiver);
        setSize(500, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        initUI();
        setVisible(true);
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new BorderLayout());

        JButton backBtn = new JButton("← Powrót");
        backBtn.setFocusPainted(false);
        backBtn.addActionListener(e -> dispose());

        JLabel chatWithLabel = new JLabel("Rozmowa z: " + receiver);
        chatWithLabel.setFont(new Font("Arial", Font.BOLD, 16));
        chatWithLabel.setHorizontalAlignment(SwingConstants.CENTER);

        topPanel.add(backBtn, BorderLayout.WEST);
        topPanel.add(chatWithLabel, BorderLayout.CENTER);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        messagesArea = new JTextArea();
        messagesArea.setEditable(false);
        messagesArea.setLineWrap(true);
        messagesArea.setWrapStyleWord(true);

        JScrollPane messagesScroll = new JScrollPane(messagesArea);
        messagesScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        mainPanel.add(messagesScroll, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(8, 8));

        messageInput = new JTextArea(3, 30);
        messageInput.setLineWrap(true);
        messageInput.setWrapStyleWord(true);

        JScrollPane inputScroll = new JScrollPane(messageInput);
        inputScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        JButton sendBtn = new JButton("Wyślij");
        sendBtn.setFocusPainted(false);
        // sendBtn.addActionListener(e -> sendMessage());

        inputPanel.add(inputScroll, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);

        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }
}
