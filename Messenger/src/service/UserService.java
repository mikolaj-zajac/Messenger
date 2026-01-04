package service;
import model.User;
import java.io.*;
import java.util.*;

public class UserService {
    private static final String FILE = "data/users.txt";
    private List<User> users = new ArrayList<>();

    public UserService() {
        load();
    }

    public void load() {
        try (BufferedReader br = new BufferedReader(new FileReader(FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(";");
                users.add(new User(p[0], p[1]));
            }
        } catch (IOException e) {
            System.out.println("Brak pliku users.txt");
        }
    }

    public boolean register(String u, String p) {
        for (User user : users)
            if (user.getUsername().equals(u))
                return false;

        User user = new User(u, p);
        users.add(user);

        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE, true))) {
            pw.println(user.toFileString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public User login(String u, String p) {
        for (User user : users)
            if (user.getUsername().equals(u) && user.checkPassword(p))
                return user;
        return null;
    }

    public void reload() {
        users.clear();
        load();
    }

}
