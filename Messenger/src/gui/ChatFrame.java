package gui;

import model.User;
import service.LoggerService;

import javax.swing.*;
import java.awt.*;
import java.io.*;

public class ChatFrame extends JFrame {

    private User currentUser;
    private JPanel usersPanel = new JPanel();
    private static final String USERS_FILE = "data/users.txt";

    public ChatFrame(User user) {
        this.currentUser = user;

        setTitle("Messenger - " + user.getUsername());
        setSize(400, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.gridx = 0;
        c.weightx = 1.0;

        JLabel title = new JLabel("Wybierz użytkownika do czatu");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setHorizontalAlignment(SwingConstants.CENTER);

        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0;
        mainPanel.add(title, c);

        usersPanel.setLayout(new BoxLayout(usersPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(usersPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        c.gridy = 1;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1.0;
        mainPanel.add(scrollPane, c);

        JButton createGroupBtn = new JButton("Utwórz grupę");
        createGroupBtn.setFocusPainted(false);
        createGroupBtn.addActionListener(e ->
                new CreateGroupFrame(currentUser, this)
        );

        c.gridy = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0;
        mainPanel.add(createGroupBtn, c);

        JButton logoutBtn = new JButton("Wyloguj");
        logoutBtn.setFocusPainted(false);
        logoutBtn.addActionListener(e -> {
            LoggerService.write("Użytkownik " + currentUser.getUsername() + " wylogował się");
            dispose();
            new LoginFrame();
        });

        c.gridy = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0;
        mainPanel.add(logoutBtn, c);

        add(mainPanel);
        setVisible(true);

        loadUsers();
    }

    public void refresh() {
        loadUsers();
    }

    private void loadUsers() {
        usersPanel.removeAll();

        try (BufferedReader br = new BufferedReader(new FileReader(USERS_FILE))) {

            br.lines()
                    .filter(line -> !line.trim().isEmpty())
                    .forEach(line -> {
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
                                JButton groupBtn = new JButton("👥 grupa: " + groupName);
                                groupBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
                                groupBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
                                groupBtn.setFocusPainted(false);

                                groupBtn.setBackground(new Color(200, 230, 255));
                                groupBtn.setOpaque(true);

                                 groupBtn.addActionListener(e ->
                                     new GroupChatFrame(currentUser, groupName)
                                 );

                                usersPanel.add(groupBtn);
                                usersPanel.add(Box.createVerticalStrut(5));
                            }
                        }
                        else {
                            String username = line.split(";")[0];

                            if (!username.equals(currentUser.getUsername())) {
                                JButton userBtn = new JButton("👤 " + username);
                                userBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
                                userBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
                                userBtn.setFocusPainted(false);

                                userBtn.addActionListener(e ->
                                        new PrivateChatFrame(currentUser, username)
                                );

                                usersPanel.add(userBtn);
                                usersPanel.add(Box.createVerticalStrut(5));
                            }
                        }
                    });

            usersPanel.revalidate();
            usersPanel.repaint();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Nie można wczytać listy użytkowników",
                    "Błąd",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

}
