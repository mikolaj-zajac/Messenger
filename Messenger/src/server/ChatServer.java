package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int PORT = 8080;
    private static Set<ClientHandler> clients = new CopyOnWriteArraySet<>();
    private static Map<String, ClientHandler> userSessions = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("=== Messenger Server starting on port " + PORT + " ===");
        System.out.println("Current directory: " + System.getProperty("user.dir"));

        // Sprawdź czy plik users.txt istnieje
        File usersFile = new File("Messenger/data/users.txt");
        if (!usersFile.exists()) {
            System.err.println("WARNING: users.txt not found in data/ directory");
            System.err.println("Creating empty users file...");
            try {
                usersFile.getParentFile().mkdirs();
                usersFile.createNewFile();
            } catch (IOException e) {
                System.err.println("Failed to create users file: " + e.getMessage());
            }
        }

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server ready! Waiting for connections...");
            System.out.println("URL: " + getLocalIP() + ":" + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection from: " + clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket, clients, userSessions);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String getLocalIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }

    public static void broadcast(String message, ClientHandler exclude) {
        for (ClientHandler client : clients) {
            if (client != exclude) {
                client.sendMessage(message);
            }
        }
    }

    public static void sendPrivate(String fromUser, String toUser, String content) {
        ClientHandler recipient = userSessions.get(toUser);
        if (recipient != null) {
            recipient.sendMessage("PRIVATE:" + fromUser + ":" + content);
        } else {
            System.out.println("User " + toUser + " is offline");
        }
    }

    public static List<String> getOnlineUsers() {
        return new ArrayList<>(userSessions.keySet());
    }

    public static int getOnlineCount() {
        return userSessions.size();
    }
}