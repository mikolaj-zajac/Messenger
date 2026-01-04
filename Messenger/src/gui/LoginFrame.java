package gui;

import service.LoggerService;
import service.UserService;
import model.User;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {

    private JTextField userField = new JTextField(15);
    private JPasswordField passField = new JPasswordField(15);
    private UserService service = new UserService();

    public LoginFrame() {
        setTitle("Messenger – logowanie");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // wyśrodkowanie

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Zaloguj się");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setHorizontalAlignment(SwingConstants.CENTER);

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        panel.add(title, c);

        c.gridwidth = 1;

        c.gridx = 0;
        c.gridy = 1;
        panel.add(new JLabel("Login:"), c);

        c.gridx = 1;
        panel.add(userField, c);

        c.gridx = 0;
        c.gridy = 2;
        panel.add(new JLabel("Hasło:"), c);

        c.gridx = 1;
        panel.add(passField, c);

        JButton loginBtn = new JButton("Zaloguj");
        JButton registerBtn = new JButton("Rejestracja");

        loginBtn.setFocusPainted(false);
        registerBtn.setFocusPainted(false);

        c.gridx = 0;
        c.gridy = 3;
        panel.add(loginBtn, c);

        c.gridx = 1;
        panel.add(registerBtn, c);

        loginBtn.addActionListener(e -> {
            service.reload();
            User u = service.login(
                    userField.getText(),
                    new String(passField.getPassword())
            );
            if (u != null) {
                new ChatFrame(u);
                LoggerService.write("Użytkownik " + userField.getText() + " zalogował się");
                dispose();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Nieprawidłowy login lub hasło",
                        "Błąd logowania",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        registerBtn.addActionListener(e -> new RegisterFrame());

        add(panel);
        setVisible(true);
    }
}
