package gui;

import model.User;
import service.LoggerService;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CreateGroupFrame extends JFrame {

    private static final String USERS_FILE = "Messenger/data/users.txt";

    private User currentUser;
    private ChatFrame chatFrame;
    private JTextField groupNameField;
    private List<JCheckBox> userCheckboxes = new ArrayList<>();

    public CreateGroupFrame(User currentUser, ChatFrame chatFrame) {
        this.currentUser = currentUser;
        this.chatFrame = chatFrame;

        setTitle("Utwórz grupę");
        setSize(400, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        initUI();
        setVisible(true);
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Tworzenie nowej grupy");
        title.setFont(new Font("Arial", Font.BOLD, 16));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(title, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));

        groupNameField = new JTextField();
        groupNameField.setBorder(BorderFactory.createTitledBorder("Nazwa grupy"));
        centerPanel.add(groupNameField, BorderLayout.NORTH);

        JPanel usersPanel = new JPanel();
        usersPanel.setLayout(new BoxLayout(usersPanel, BoxLayout.Y_AXIS));
        loadUsers(usersPanel);

        centerPanel.add(new JScrollPane(usersPanel), BorderLayout.CENTER);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton cancelBtn = new JButton("Anuluj");
        cancelBtn.addActionListener(e -> dispose());

        JButton createBtn = new JButton("Utwórz");
        createBtn.addActionListener(e -> createGroup());

        bottomPanel.add(cancelBtn);
        bottomPanel.add(createBtn);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        add(mainPanel);
    }

    private void loadUsers(JPanel usersPanel) {
        try (BufferedReader br = new BufferedReader(new FileReader(USERS_FILE))) {
            br.lines()
                    .filter(l -> !l.startsWith("group:"))
                    .map(l -> l.split(";")[0])
                    .filter(u -> !u.equals(currentUser.getUsername()))
                    .forEach(username -> {
                        JCheckBox cb = new JCheckBox(username);
                        userCheckboxes.add(cb);
                        usersPanel.add(cb);
                    });
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Nie można wczytać użytkowników",
                    "Błąd",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createGroup() {
        String groupName = groupNameField.getText().trim();

        if (groupName.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Podaj nazwę grupy",
                    "Błąd",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<String> members = new ArrayList<>();
        members.add(currentUser.getUsername());

        userCheckboxes.stream()
                .filter(JCheckBox::isSelected)
                .map(JCheckBox::getText)
                .forEach(members::add);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(USERS_FILE, true))) {
            bw.write("group:" + groupName + ";" + String.join(",", members));
            bw.newLine();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Nie można zapisać grupy",
                    "Błąd",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this,
                "Grupa utworzona",
                "OK",
                JOptionPane.INFORMATION_MESSAGE);

        LoggerService.write("Utworzono grupę o nazwie: " + groupName + ", członkowie grupy: " + String.join(", ", members));
        chatFrame.refresh();
        dispose();
    }
}
