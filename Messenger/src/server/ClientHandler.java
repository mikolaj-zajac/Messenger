package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private Set<ClientHandler> clients;
    private Map<String, ClientHandler> userSessions;
    private static final String USERS_FILE = "Messenger/data/users.txt";

    public ClientHandler(Socket socket, Set<ClientHandler> clients,
                         Map<String, ClientHandler> userSessions) {
        this.socket = socket;
        this.clients = clients;
        this.userSessions = userSessions;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Krok 1: Logowanie
            out.println("HELLO:Please login with format: LOGIN:username:password");
            String loginLine;

            while ((loginLine = in.readLine()) != null) {
                if (loginLine.startsWith("LOGIN:")) {
                    String[] parts = loginLine.split(":");
                    if (parts.length >= 3) {
                        String user = parts[1];
                        String pass = parts[2];

                        if (verifyPassword(user, pass)) {
                            this.username = user;
                            userSessions.put(username, this);

                            out.println("LOGIN_SUCCESS:" + username);
                            out.println("ONLINE_USERS:" + String.join(",", ChatServer.getOnlineUsers()));
                            ChatServer.broadcast("USER_JOINED:" + username, this);
                            System.out.println("User " + username + " logged in successfully");
                            break;
                        } else {
                            out.println("LOGIN_FAILED:Invalid username or password");
                        }
                    }
                }
            }

            // Krok 2: Obsługa wiadomości
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("[" + username + "]: " + message);

                if (message.startsWith("PRIVATE:")) {
                    String[] parts = message.split(":", 3);
                    if (parts.length == 3) {
                        String toUser = parts[1];
                        String content = parts[2];
                        ChatServer.sendPrivate(username, toUser, content);
                        saveMessageToFile(username, toUser, content);
                    }
                }
                // DODANE: Obsługa wiadomości grupowych
                else if (message.startsWith("GROUP:")) {
                    String[] parts = message.split(":", 3);
                    if (parts.length == 3) {
                        String groupName = parts[1];
                        String content = parts[2];

                        // Pobierz członków grupy
                        List<String> members = getGroupMembers(groupName);
                        if (members != null && members.contains(username)) {
                            // Wyślij do wszystkich członków grupy którzy są online
                            for (String member : members) {
                                if (!member.equals(username) && userSessions.containsKey(member)) {
                                    ClientHandler memberHandler = userSessions.get(member);
                                    memberHandler.sendMessage("GROUP_MSG:" + groupName + ":" + username + ":" + content);
                                }
                            }
                            // Zapisz wiadomość do pliku
                            saveGroupMessageToFile(username, groupName, content);
                            System.out.println("Group message in " + groupName + " from " + username);
                        }
                    }
                }
                else if (message.startsWith("GET_ONLINE")) {
                    out.println("ONLINE_USERS:" + String.join(",", ChatServer.getOnlineUsers()));
                } else if (message.equals("LOGOUT")) {
                    break;
                }
            }

        } catch (IOException e) {
            System.out.println("Client disconnected: " + username);
        } finally {
            cleanup();
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    private void cleanup() {
        clients.remove(this);
        if (username != null) {
            userSessions.remove(username);
            ChatServer.broadcast("USER_LEFT:" + username, this);
            System.out.println("User " + username + " disconnected");
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return username;
    }

    private boolean verifyPassword(String username, String password) {
        try {
            File usersFile = new File(USERS_FILE);
            if (!usersFile.exists()) {
                System.err.println("Users file not found: " + USERS_FILE);
                return false;
            }

            BufferedReader br = new BufferedReader(new FileReader(usersFile));
            String line;

            while ((line = br.readLine()) != null) {
                if (!line.startsWith("group:")) {
                    String[] parts = line.split(";");
                    if (parts.length >= 2 && parts[0].equals(username) && parts[1].equals(password)) {
                        br.close();
                        return true;
                    }
                }
            }
            br.close();
        } catch (IOException e) {
            System.err.println("Error reading users file: " + e.getMessage());
        }
        return false;
    }

    private List<String> getGroupMembers(String groupName) {
        List<String> members = new ArrayList<>();
        try {
            File usersFile = new File(USERS_FILE);
            BufferedReader br = new BufferedReader(new FileReader(usersFile));
            String line;

            while ((line = br.readLine()) != null) {
                if (line.startsWith("group:" + groupName + ";")) {
                    String membersPart = line.split(";", 2)[1];
                    String[] memberArray = membersPart.split(",");
                    for (String member : memberArray) {
                        members.add(member.trim());
                    }
                    br.close();
                    return members;
                }
            }
            br.close();
        } catch (IOException e) {
            System.err.println("Error reading group members: " + e.getMessage());
        }
        return null;
    }

    private void saveMessageToFile(String from, String to, String content) {
        try {
            File messagesDir = new File("data");
            if (!messagesDir.exists()) {
                messagesDir.mkdirs();
            }

            File messagesFile = new File("Messenger/data/messages.txt");
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(messagesFile, true))) {
                bw.write(from + ";" + to + ";" + content);
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving message: " + e.getMessage());
        }
    }

    private void saveGroupMessageToFile(String from, String groupName, String content) {
        try {
            File groupsDir = new File("Messenger/data/groups");
            if (!groupsDir.exists()) {
                groupsDir.mkdirs();
            }

            File groupFile = new File("Messenger/data/groups/" + groupName + "_messages.txt");
            long timestamp = System.currentTimeMillis();
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(groupFile, true))) {
                bw.write(timestamp + ";" + from + ";" + content);
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving group message: " + e.getMessage());
        }
    }
}