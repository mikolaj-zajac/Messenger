package service;

import java.io.*;
import java.net.*;
import java.util.function.BiConsumer;

public class SimpleChatClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private boolean connected = false;
    private Thread listener;
    private BiConsumer<String, String> messageCallback;
    private Thread pingThread;

    public boolean connect(String username) {
        // 1. Spróbuj znaleźć serwer w sieci
        String serverIP = service.AutomaticServerFinder.findServerInNetwork(5000);

        if (serverIP == null) {
            System.out.println("Nie znaleziono serwera w sieci. Uruchamiam tryb lokalny.");
            return false; // Tryb lokalny
        }

        return connectToServer(serverIP, 12345, username);
    }

    private boolean connectToServer(String serverIP, int port, String username) {
        try {
            this.username = username;
            System.out.println("Łączę z serwerem: " + serverIP + ":" + port);

            socket = new Socket(serverIP, port);
            socket.setSoTimeout(10000); // 10 sekund timeout

            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Odbierz powitanie
            String hello = in.readLine();
            System.out.println("Serwer: " + hello);

            // Zaloguj się
            out.println("LOGIN:" + username);

            // Odbierz odpowiedź
            String response = in.readLine();
            System.out.println("Serwer: " + response);

            if (response != null && response.startsWith("LOGIN_OK")) {
                connected = true;
                startListening();
                startPingThread();
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
                        if (parts.length == 3 && messageCallback != null) {
                            messageCallback.accept(parts[1], parts[2]);
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
                    else if (message.equals("PONG")) {
                        // Ignoruj pong
                    }
                }
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout połączenia");
            } catch (IOException e) {
                System.out.println("Utracono połączenie z serwerem");
            } finally {
                connected = false;
            }
        });
        listener.start();
    }

    private void startPingThread() {
        pingThread = new Thread(() -> {
            while (connected) {
                try {
                    Thread.sleep(15000); // Co 15 sekund
                    if (connected) {
                        out.println("PING");
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        pingThread.setDaemon(true);
        pingThread.start();
    }

    public void sendPrivateMessage(String toUser, String text) {
        if (connected) {
            out.println("PRIVATE:" + toUser + ":" + text);
        }
    }

    public void setMessageCallback(BiConsumer<String, String> callback) {
        this.messageCallback = callback;
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