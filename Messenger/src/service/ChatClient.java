package service;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ChatClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private List<String> onlineUsers = new ArrayList<>();
    private Thread messageListener;
    private boolean connected = false;
    private BiConsumer<String, String> privateMessageCallback;
    private Consumer<String[]> groupMessageCallback; // Nowy callback dla grup

    public ChatClient(String serverAddress, int port, String username, String password) throws IOException {
        this.username = username;

        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(serverAddress, port), 3000);
            socket.setSoTimeout(5000);

            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String response = in.readLine();
            System.out.println("Server: " + response);

            out.println("LOGIN:" + username + ":" + password);

            response = in.readLine();
            System.out.println("Login response: " + response);

            if (response != null && response.startsWith("LOGIN_SUCCESS")) {
                connected = true;
                startMessageListener();
                requestOnlineUsers();
                System.out.println("Connected to server as " + username);
            } else {
                throw new IOException("Login failed: " + response);
            }

        } catch (Exception e) {
            closeConnection();
            throw new IOException("Cannot connect to server: " + e.getMessage());
        }
    }

    public void setPrivateMessageCallback(BiConsumer<String, String> callback) {
        this.privateMessageCallback = callback;
    }

    public void setGroupMessageCallback(Consumer<String[]> callback) {
        this.groupMessageCallback = callback;
    }

    private void startMessageListener() {
        messageListener = new Thread(() -> {
            try {
                String message;
                while (connected && (message = in.readLine()) != null) {
                    System.out.println("Received from server: " + message);

                    if (message.startsWith("PRIVATE:")) {
                        String[] parts = message.split(":", 3);
                        if (parts.length == 3) {
                            String fromUser = parts[1];
                            String content = parts[2];
                            saveMessageToFile(fromUser, username, content);
                            if (privateMessageCallback != null) {
                                privateMessageCallback.accept(fromUser, content);
                            }
                        }
                    }
                    // DODANE: Obsługa wiadomości grupowych
                    else if (message.startsWith("GROUP_MSG:")) {
                        String[] parts = message.split(":", 4);
                        if (parts.length == 4) {
                            String groupName = parts[1];
                            String fromUser = parts[2];
                            String content = parts[3];

                            // Zapisz do pliku
                            saveGroupMessageToFile(fromUser, groupName, content);

                            // Wywołaj callback
                            if (groupMessageCallback != null) {
                                groupMessageCallback.accept(new String[]{groupName, fromUser, content});
                            }
                        }
                    }
                    else if (message.startsWith("USER_JOINED:")) {
                        String newUser = message.substring(12);
                        if (!onlineUsers.contains(newUser) && !newUser.equals(username)) {
                            onlineUsers.add(newUser);
                            System.out.println("User joined: " + newUser);
                        }
                    } else if (message.startsWith("USER_LEFT:")) {
                        String leftUser = message.substring(10);
                        onlineUsers.remove(leftUser);
                        System.out.println("User left: " + leftUser);
                    } else if (message.startsWith("ONLINE_USERS:")) {
                        updateOnlineUsers(message.substring(13));
                    }
                }
            } catch (SocketTimeoutException e) {
                System.out.println("Socket timeout");
            } catch (IOException e) {
                System.out.println("Connection lost: " + e.getMessage());
            } finally {
                connected = false;
            }
        });
        messageListener.setDaemon(true);
        messageListener.start();
    }

    public void sendGroupMessage(String groupName, String message) {
        if (out != null && connected) {
            out.println("GROUP:" + groupName + ":" + message);
            System.out.println("Sent to group " + groupName + ": " + message);
            // Zapisz też lokalnie
            saveGroupMessageToFile(username, groupName, message);
        }
    }

    public void sendPrivateMessage(String toUser, String message) {
        if (out != null && connected) {
            out.println("PRIVATE:" + toUser + ":" + message);
            System.out.println("Sent to " + toUser + ": " + message);
            saveMessageToFile(username, toUser, message);
        }
    }

    private void saveMessageToFile(String from, String to, String content) {
        try {
            File dir = new File("data");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File messagesFile = new File("Messenger/data/messages.txt");
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(messagesFile, true))) {
                bw.write(from + ";" + to + ";" + content);
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving message to file: " + e.getMessage());
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
            System.err.println("Error saving group message to file: " + e.getMessage());
        }
    }

    private void updateOnlineUsers(String usersList) {
        onlineUsers.clear();
        if (!usersList.isEmpty()) {
            String[] users = usersList.split(",");
            for (String user : users) {
                if (!user.isEmpty() && !user.equals(username)) {
                    onlineUsers.add(user);
                }
            }
        }
    }

    private void requestOnlineUsers() {
        if (out != null && connected) {
            out.println("GET_ONLINE");
        }
    }

    public List<String> getOnlineUsers() {
        return new ArrayList<>(onlineUsers);
    }

    public void logout() {
        if (out != null && connected) {
            out.println("LOGOUT");
        }
        closeConnection();
    }

    private void closeConnection() {
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (messageListener != null) {
                messageListener.interrupt();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed() && socket.isConnected();
    }

    public String getUsername() {
        return username;
    }
}