package gui;

import model.User;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class GroupChatFrame extends JFrame {

    private User currentUser;
    private String groupName;

    private JTextArea messagesArea;
    private JTextArea messageInput;

    public GroupChatFrame(User currentUser, String groupName) {
        this.currentUser = currentUser;
        this.groupName = groupName;

        setTitle("Grupa: " + groupName);
        setSize(550, 600);
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

        String membersText = getGroupMembersText();

        JLabel titleLabel = new JLabel("<html>👥 " + groupName + "<br><small>Członkowie: " + membersText + "</small></html>");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        topPanel.add(backBtn, BorderLayout.WEST);
        topPanel.add(titleLabel, BorderLayout.CENTER);

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

        // sendBtn.addActionListener(e -> sendGroupMessage());

        inputPanel.add(inputScroll, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);

        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private String getGroupMembersText() {
        File file = new File("data/users.txt");

        if (!file.exists()) return "";

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("group:" + groupName + ";")) {
                    String membersPart = line.split(";", 2)[1]; // jan,anna,piotr
                    return membersPart.replace(",", ", ");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

}
