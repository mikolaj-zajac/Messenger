package server;

import java.io.*;
import java.net.*;
import java.util.*;
import service.AutomaticServerFinder;

public class SimpleServer {
    private static final int CHAT_PORT = 12345;
    private static Map<String, ClientHandler> clients = new HashMap<>();
    private static final String MESSAGES_FILE = "server_messages.txt";
    private static final String ONLINE_FILE = "server_online.txt";

    public static void main(String[] args) throws IOException {
        System.out.println("=== Messenger Server ===");
        System.out.println("Uruchamianie serwera discovery...");
        AutomaticServerFinder.startDiscoveryServer();

        System.out.println("Uruchamianie serwera czatu na porcie: " + CHAT_PORT);

        // Utwórz pliki serwera
        new File(MESSAGES_FILE).delete();
        new File(ONLINE_FILE).delete();

        ServerSocket serverSocket = new ServerSocket(CHAT_PORT);
        System.out.println("Serwer gotowy! Czekam na połączenia...");

        // Czyszczenie starych połączeń co 60 sekund
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                cleanupOldConnections();
            }
        }, 0, 60000);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Nowe połączenie z: " + clientSocket.getInetAddress());
            new Thread(new ClientHandler(clientSocket)).start();
        }
    }

    private static synchronized void cleanupOldConnections() {
        Iterator<Map.Entry<String, ClientHandler>> iterator = clients.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ClientHandler> entry = iterator.next();
            if (!entry.getValue().isAlive()) {
                System.out.println("Usuwam nieaktywnego klienta: " + entry.getKey());
                iterator.remove();
                removeFromOnlineFile(entry.getKey());
            }
        }
    }

    private static synchronized void broadcast(String message, String exclude) {
        for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
            if (!entry.getKey().equals(exclude)) {
                entry.getValue().send(message);
            }
        }
    }

    private static synchronized void sendToUser(String username, String message) {
        ClientHandler client = clients.get(username);
        if (client != null) {
            client.send(message);
        }
    }

    private static synchronized void saveMessageToFile(String from, String to, String message) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(MESSAGES_FILE, true))) {
            String timestamp = System.currentTimeMillis() + "|" + from + "|" + to + "|" + message;
            writer.write(timestamp);
            writer.newLine();
            System.out.println("Zapisano wiadomość: " + from + " -> " + to);
        } catch (IOException e) {
            System.err.println("Błąd zapisu wiadomości: " + e.getMessage());
        }
    }

    private static synchronized List<String> getMessagesForUser(String username) {
        List<String> messages = new ArrayList<>();
        File file = new File(MESSAGES_FILE);

        if (!file.exists()) {
            return messages;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|", 4);
                if (parts.length == 4) {
                    String to = parts[2];
                    String from = parts[1];
                    if (to.equals(username) || from.equals(username)) {
                        messages.add(line);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Błąd odczytu wiadomości: " + e.getMessage());
        }

        return messages;
    }

    private static synchronized void addToOnlineFile(String username) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ONLINE_FILE, true))) {
            writer.write(username + "|" + System.currentTimeMillis());
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Błąd zapisu online: " + e.getMessage());
        }
    }

    private static synchronized void removeFromOnlineFile(String username) {
        try {
            File tempFile = new File(ONLINE_FILE + ".tmp");
            File originalFile = new File(ONLINE_FILE);

            if (!originalFile.exists()) {
                return;
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(originalFile));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    if (parts.length > 0 && !parts[0].equals(username)) {
                        writer.write(line);
                        writer.newLine();
                    }
                }
            }

            originalFile.delete();
            tempFile.renameTo(originalFile);

        } catch (IOException e) {
            System.err.println("Błąd usuwania z online: " + e.getMessage());
        }
    }

    private static synchronized List<String> getOnlineUsers() {
        List<String> onlineUsers = new ArrayList<>();
        File file = new File(ONLINE_FILE);

        if (!file.exists()) {
            return onlineUsers;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            long currentTime = System.currentTimeMillis();

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 2) {
                    String username = parts[0];
                    long timestamp = Long.parseLong(parts[1]);

                    // Jeśli aktywny w ciągu ostatnich 5 minut
                    if (currentTime - timestamp < 300000) {
                        onlineUsers.add(username);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Błąd odczytu online: " + e.getMessage());
        }

        return onlineUsers;
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;
        private long lastActivity;
        private boolean running = true;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.lastActivity = System.currentTimeMillis();
        }

        public boolean isAlive() {
            return (System.currentTimeMillis() - lastActivity) < 300000; // 5 minut
        }

        public void send(String message) {
            if (out != null) {
                out.println(message);
                out.flush();
            }
        }

        @Override
        public void run() {
            try {
                // Ustaw timeout na socket (5 minut)
                socket.setSoTimeout(300000);

                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println("HELLO:Podaj swój login");

                String loginLine = in.readLine();
                if (loginLine != null && loginLine.startsWith("LOGIN:")) {
                    username = loginLine.substring(6);

                    synchronized (SimpleServer.class) {
                        clients.put(username, this);
                        addToOnlineFile(username);
                    }

                    out.println("LOGIN_OK:" + username);
                    System.out.println(username + " zalogował się");

                    // Wyślij historię wiadomości
                    List<String> userMessages = getMessagesForUser(username);
                    for (String msg : userMessages) {
                        out.println("HISTORY:" + msg);
                    }

                    // Wyślij listę online użytkowników
                    List<String> onlineUsers = getOnlineUsers();
                    StringBuilder onlineList = new StringBuilder("ONLINE_LIST:");
                    for (String user : onlineUsers) {
                        if (!user.equals(username)) {
                            onlineList.append(user).append(",");
                        }
                    }
                    out.println(onlineList.toString());

                    // Powiadom innych o nowym użytkowniku
                    broadcast("USER_ONLINE:" + username, username);

                    // Główna pętla odbioru wiadomości
                    String message;
                    while (running && (message = in.readLine()) != null) {
                        lastActivity = System.currentTimeMillis();

                        if (message.equals("PING")) {
                            out.println("PONG");
                        }
                        else if (message.startsWith("PRIVATE:")) {
                            String[] parts = message.split(":", 3);
                            if (parts.length == 3) {
                                String to = parts[1];
                                String msg = parts[2];

                                // Zapisz wiadomość
                                saveMessageToFile(username, to, msg);

                                // Wyślij do odbiorcy jeśli online
                                sendToUser(to, "PRIVATE_MSG:" + username + ":" + msg);
                                out.println("MSG_SENT:" + to);
                            }
                        }
                        else if (message.equals("GET_HISTORY")) {
                            List<String> history = getMessagesForUser(username);
                            for (String msg : history) {
                                out.println("HISTORY:" + msg);
                            }
                        }
                        else if (message.equals("LOGOUT")) {
                            break;
                        }
                        else {
                            broadcast("CHAT:" + username + ":" + message, username);
                        }
                    }
                }

            } catch (SocketTimeoutException e) {
                System.out.println("Timeout dla " + username + " - brak aktywności");
            } catch (IOException e) {
                System.out.println("Błąd połączenia z " + username + ": " + e.getMessage());
            } finally {
                cleanup();
            }
        }

        private void cleanup() {
            running = false;
            if (username != null) {
                synchronized (SimpleServer.class) {
                    clients.remove(username);
                    removeFromOnlineFile(username);
                }
                broadcast("USER_OFFLINE:" + username, username);
                System.out.println(username + " rozłączył się");
            }

            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}