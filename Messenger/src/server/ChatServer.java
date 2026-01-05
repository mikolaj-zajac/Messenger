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

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server ready! Waiting for connections...");
            System.out.println("URL: " + InetAddress.getLocalHost().getHostAddress() + ":" + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection from: " + clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket, clients, userSessions);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
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
            // Użytkownik offline - zapisz do bazy?
            System.out.println("User " + toUser + " is offline");
        }
    }

    public static List<String> getOnlineUsers() {
        return new ArrayList<>(userSessions.keySet());
    }
}