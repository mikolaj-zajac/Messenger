package service;

import model.User;
import java.io.*;
import java.util.*;

public class UserService {
    private static final String FILE = "Messenger/data/users.txt";
    private static final String ONLINE_DIR = "Messenger/data/online/";
    private List<User> users = new ArrayList<>();

    public UserService() {
        load();
    }

    public void load() {
        try (BufferedReader br = new BufferedReader(new FileReader(FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("group:")) {
                    String[] p = line.split(";");
                    users.add(new User(p[0], p[1]));
                }
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

    // NOWA METODA: Sprawdź czy użytkownik jest online
    public boolean isUserOnline(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }

        File onlineFile = new File(ONLINE_DIR + username + ".online");
        if (!onlineFile.exists()) {
            return false;
        }

        // Użytkownik jest online jeśli plik był modyfikowany w ciągu ostatnich 30 sekund
        long lastModified = onlineFile.lastModified();
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastModified) < 30000; // 30 sekund
    }

    // NOWA METODA: Zaktualizuj status online użytkownika
    public void updateUserOnlineStatus(String username) {
        try {
            File onlineDir = new File(ONLINE_DIR);
            if (!onlineDir.exists()) {
                onlineDir.mkdirs();
            }

            File onlineFile = new File(ONLINE_DIR + username + ".online");
            onlineFile.createNewFile(); // Utwórz lub zaktualizuj plik
        } catch (IOException e) {
            System.err.println("Błąd aktualizacji statusu online: " + e.getMessage());
        }
    }

    // NOWA METODA: Usuń status online
    public void removeUserOnlineStatus(String username) {
        File onlineFile = new File(ONLINE_DIR + username + ".online");
        if (onlineFile.exists()) {
            onlineFile.delete();
        }
    }

    // NOWA METODA: Pobierz listę online użytkowników
    public List<String> getOnlineUsers() {
        List<String> onlineUsers = new ArrayList<>();
        File onlineDir = new File(ONLINE_DIR);

        if (!onlineDir.exists()) {
            return onlineUsers;
        }

        File[] files = onlineDir.listFiles((dir, name) -> name.endsWith(".online"));
        if (files != null) {
            long currentTime = System.currentTimeMillis();
            for (File file : files) {
                if (currentTime - file.lastModified() < 30000) { // 30 sekund
                    String username = file.getName().replace(".online", "");
                    onlineUsers.add(username);
                } else {
                    // Usuń stary plik
                    file.delete();
                }
            }
        }

        return onlineUsers;
    }
}