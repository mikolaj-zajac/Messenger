package network;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private MessageListener listener;

    public interface MessageListener {
        void onMessageReceived(String from, String content);
        void onOnlineUsersUpdated(List<String> users);
        void onConnectionStatusChanged(boolean connected);
        void onGroupMessageReceived(String groupName, String from, String content); // DODAJ TĘ LINIĘ
    }

    public ChatClient(MessageListener listener) {
        this.listener = listener;
    }

    public boolean connect(String serverAddress, int port, String username, String password) {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(serverAddress, port), 15000);

            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Logowanie
            out.println("LOGIN:" + username + ":" + password);

            // Oczekiwanie na odpowiedź
            String response = in.readLine();
            if (response != null && response.startsWith("LOGIN_SUCCESS")) {
                this.username = username;
                startMessageReader();
                listener.onConnectionStatusChanged(true);
                return true;
            }
        } catch (IOException e) {
            System.err.println("Connection failed: " + e.getMessage());
        }
        return false;
    }

    private void startMessageReader() {
        new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Server says: " + message);

                    if (message.startsWith("PRIVATE:")) {
                        String[] parts = message.split(":", 3);
                        if (parts.length == 3) {
                            String from = parts[1];
                            String content = parts[2];
                            listener.onMessageReceived(from, content);
                        }
                    } else if (message.startsWith("ONLINE_USERS:")) {
                        String usersStr = message.substring(13);
                        List<String> users = Arrays.asList(usersStr.split(","));
                        listener.onOnlineUsersUpdated(users);
                    } else if (message.startsWith("USER_JOINED:")) {
                        // Nowy użytkownik online
                    } else if (message.startsWith("USER_LEFT:")) {
                        // Użytkownik wyszedł
                    }
                }
            } catch (IOException e) {
                System.out.println("Connection lost");
                listener.onConnectionStatusChanged(false);
            }
        }).start();
    }

    public void sendPrivateMessage(String toUser, String content) {
        if (out != null) {
            out.println("PRIVATE:" + toUser + ":" + content);
        }
    }

    public void requestOnlineUsers() {
        if (out != null) {
            out.println("GET_ONLINE");
        }
    }

    public void disconnect() {
        try {
            if (out != null) out.println("LOGOUT");
            if (socket != null) socket.close();
            listener.onConnectionStatusChanged(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return username;
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}