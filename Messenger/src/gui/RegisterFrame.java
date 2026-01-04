package gui;

import service.LoggerService;
import service.UserService;

import javax.swing.*;
import java.awt.*;

public class RegisterFrame extends JFrame {

    private JTextField userField = new JTextField(15);
    private JPasswordField passField = new JPasswordField(15);
    private UserService service = new UserService();

    public RegisterFrame() {
        setTitle("Rejestracja");
        setSize(400, 300);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null); // wyśrodkowanie

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Zarejestruj się");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setHorizontalAlignment(SwingConstants.CENTER);

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        panel.add(title, c);

        c.gridwidth = 1;

        // Pole login
        c.gridx = 0;
        c.gridy = 1;
        panel.add(new JLabel("Login:"), c);

        c.gridx = 1;
        panel.add(userField, c);

        // Pole hasło
        c.gridx = 0;
        c.gridy = 2;
        panel.add(new JLabel("Hasło:"), c);

        c.gridx = 1;
        panel.add(passField, c);

        JButton registerBtn = new JButton("Zarejestruj");
        JButton cancelBtn = new JButton("Anuluj");

        registerBtn.setFocusPainted(false);
        cancelBtn.setFocusPainted(false);

        c.gridx = 0;
        c.gridy = 3;
        panel.add(registerBtn, c);

        c.gridx = 1;
        panel.add(cancelBtn, c);

        registerBtn.addActionListener(e -> register());
        cancelBtn.addActionListener(e -> dispose());

        add(panel);
        setVisible(true);
    }

    private void register() {
        String username = userField.getText();
        String password = new String(passField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Wszystkie pola muszą być wypełnione",
                    "Błąd",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean success = service.register(username, password);

        if (success) {
            JOptionPane.showMessageDialog(this,
                    "Rejestracja zakończona sukcesem!");

            LoggerService.write("Użytkownik " + username + " zarejstrował się");
            dispose();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Użytkownik już istnieje",
                    "Błąd",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
