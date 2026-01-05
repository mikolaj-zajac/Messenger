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

                        // TU DODASZ SPRAWDZENIE HASŁA Z PLIKU
                        // Na razie akceptujemy wszystkie
                        this.username = user;
                        userSessions.put(username, this);

                        out.println("LOGIN_SUCCESS:" + username);
                        out.println("ONLINE_USERS:" + String.join(",", ChatServer.getOnlineUsers()));
                        ChatServer.broadcast("USER_JOINED:" + username, this);
                        break;
                    }
                }
            }

            // Krok 2: Obsługa wiadomości
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("[" + username + "]: " + message);

                if (message.startsWith("PRIVATE:")) {
                    // Format: PRIVATE:toUser:messageContent
                    String[] parts = message.split(":", 3);
                    if (parts.length == 3) {
                        String toUser = parts[1];
                        String content = parts[2];
                        ChatServer.sendPrivate(username, toUser, content);
                    }
                } else if (message.startsWith("GET_ONLINE")) {
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
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("User " + username + " disconnected");
    }

    public String getUsername() {
        return username;
    }

    private boolean verifyPassword(String username, String password) {
        try {
            // Wczytaj plik users.txt z serwera
            File usersFile = new File("Messenger/data/raport.txt");
            BufferedReader br = new BufferedReader(new FileReader(usersFile));
            String line;

            while ((line = br.readLine()) != null) {
                if (!line.startsWith("group:")) {
                    String[] parts = line.split(";");
                    if (parts[0].equals(username) && parts[1].equals(password)) {
                        br.close();
                        return true;
                    }
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}