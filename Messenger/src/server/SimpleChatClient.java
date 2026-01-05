package service;

import java.io.*;
import java.net.*;
import java.util.function.BiConsumer;
import java.util.ArrayList;
import java.util.List;

public class SimpleChatClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private boolean connected = false;
    private Thread listener;
    private BiConsumer<String, String> privateMessageCallback;
    private BiConsumer<String, String> historyCallback;
    private List<String> messageHistory = new ArrayList<>();

    public boolean connect(String username) {
        // Próbuj automatycznego wykrycia serwera
        String serverIP = AutomaticServerFinder.findServerInNetwork(3000);

        if (serverIP == null) {
            System.out.println("Nie znaleziono serwera. Uruchamiam tryb lokalny.");
            return false;
        }

        return connectToServer(serverIP, 12345, username);
    }

    private boolean connectToServer(String serverIP, int port, String username) {
        try {
            this.username = username;
            System.out.println("Łączenie z serwerem: " + serverIP + ":" + port);

            socket = new Socket(serverIP, port);
            socket.setSoTimeout(10000);

            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Odbierz powitanie
            String hello = in.readLine();
            System.out.println("Serwer: " + hello);

            // Zaloguj
            out.println("LOGIN:" + username);

            // Odbierz odpowiedź
            String response = in.readLine();
            System.out.println("Serwer: " + response);

            if (response != null && response.startsWith("LOGIN_OK")) {
                connected = true;
                startListening();

                // Pobierz historię wiadomości
                out.println("GET_HISTORY");

                return true;
            }

        } catch (Exception e) {
            System.out.println("Nie można połączyć z serwerem: " + e.getMessage());
        }
        return false;
    }

    private void startListening() {
        listener = new Thread(() -> {
            try {
                String message;
                while (connected && (message = in.readLine()) != null) {
                    System.out.println("Otrzymano: " + message);

                    if (message.startsWith("PRIVATE_MSG:")) {
                        String[] parts = message.split(":", 3);
                        if (parts.length == 3 && privateMessageCallback != null) {
                            // Format: PRIVATE_MSG:nadawca:wiadomość
                            privateMessageCallback.accept(parts[1], parts[2]);
                            messageHistory.add(System.currentTimeMillis() + "|" + parts[1] + "|" + username + "|" + parts[2]);
                        }
                    }
                    else if (message.startsWith("HISTORY:")) {
                        // Format: HISTORY:timestamp|from|to|message
                        String historyData = message.substring(8);
                        messageHistory.add(historyData);

                        if (historyCallback != null) {
                            String[] parts = historyData.split("\\|", 4);
                            if (parts.length == 4) {
                                historyCallback.accept(parts[1], parts[3]);
                            }
                        }
                    }
                    else if (message.startsWith("USER_ONLINE:")) {
                        String user = message.substring(12);
                        System.out.println(user + " jest teraz online");
                    }
                    else if (message.startsWith("USER_OFFLINE:")) {
                        String user = message.substring(13);
                        System.out.println(user + " wyszedł");
                    }
                    else if (message.startsWith("ONLINE_LIST:")) {
                        String users = message.substring(12);
                        System.out.println("Online użytkownicy: " + users);
                    }
                }
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout połączenia");
            } catch (IOException e) {
                System.out.println("Utracono połączenie z serwerem: " + e.getMessage());
            } finally {
                connected = false;
            }
        });
        listener.start();
    }

    public void sendPrivateMessage(String toUser, String text) {
        if (connected && out != null) {
            out.println("PRIVATE:" + toUser + ":" + text);
        }
    }

    public List<String> getMessageHistory() {
        return new ArrayList<>(messageHistory);
    }

    public void setPrivateMessageCallback(BiConsumer<String, String> callback) {
        this.privateMessageCallback = callback;
    }

    public void setHistoryCallback(BiConsumer<String, String> callback) {
        this.historyCallback = callback;
    }

    public void disconnect() {
        connected = false;
        if (out != null) {
            out.println("LOGOUT");
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public String getUsername() {
        return username;
    }
}