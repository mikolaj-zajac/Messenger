package server;

import java.io.*;
import java.net.*;
import java.util.*;
import service.AutomaticServerFinder;

public class SimpleServer {
    private static final int CHAT_PORT = 12345;
    private static Map<String, ClientHandler> clients = new HashMap<>();

    public static void main(String[] args) throws IOException {
        System.out.println("=== Messenger Server ===");
        System.out.println("Uruchamianie serwera discovery...");
        AutomaticServerFinder.startDiscoveryServer();

        System.out.println("Uruchamianie serwera czatu na porcie: " + CHAT_PORT);

        ServerSocket serverSocket = new ServerSocket(CHAT_PORT);
        System.out.println("Serwer gotowy!");
        System.out.println("Adresy serwera:");

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            List<NetworkInterface> interfaceList = Collections.list(interfaces);

            for (NetworkInterface iface : interfaceList) {
                if (iface.isLoopback() || !iface.isUp()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                List<InetAddress> addressList = Collections.list(addresses);

                for (InetAddress addr : addressList) {
                    if (addr instanceof Inet4Address) {
                        System.out.println("  - " + addr.getHostAddress() + ":" + CHAT_PORT);
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        // Czyszczenie starych połączeń co 30 sekund
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                cleanupOldConnections();
            }
        }, 0, 30000);

        while (true) {
            Socket clientSocket = serverSocket.accept();
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

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;
        private long lastActivity;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.lastActivity = System.currentTimeMillis();
        }

        public boolean isAlive() {
            return (System.currentTimeMillis() - lastActivity) < 60000; // 60 sekund
        }

        public void send(String message) {
            if (out != null) {
                out.println(message);
            }
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println("HELLO:Podaj swój login");

                String loginLine = in.readLine();
                if (loginLine != null && loginLine.startsWith("LOGIN:")) {
                    username = loginLine.substring(6);

                    synchronized (SimpleServer.class) {
                        clients.put(username, this);
                    }

                    out.println("LOGIN_OK:" + username);

                    // Powiadom innych o nowym użytkowniku
                    broadcast("USER_ONLINE:" + username, username);

                    // Wyślij listę online użytkowników
                    StringBuilder onlineList = new StringBuilder("ONLINE_LIST:");
                    synchronized (SimpleServer.class) {
                        for (String user : clients.keySet()) {
                            if (!user.equals(username)) {
                                onlineList.append(user).append(",");
                            }
                        }
                    }
                    out.println(onlineList.toString());

                    System.out.println(username + " dołączył do czatu");

                    // Obsługa wiadomości
                    String message;
                    while ((message = in.readLine()) != null) {
                        lastActivity = System.currentTimeMillis();

                        if (message.equals("PING")) {
                            out.println("PONG");
                        }
                        else if (message.startsWith("PRIVATE:")) {
                            String[] parts = message.split(":", 3);
                            if (parts.length == 3) {
                                String to = parts[1];
                                String msg = parts[2];

                                sendToUser(to, "PRIVATE_MSG:" + username + ":" + msg);
                                out.println("MSG_SENT:" + to);
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

            } catch (IOException e) {
                System.out.println("Błąd połączenia z " + username);
            } finally {
                cleanup();
            }
        }

        private void cleanup() {
            if (username != null) {
                synchronized (SimpleServer.class) {
                    clients.remove(username);
                }
                broadcast("USER_OFFLINE:" + username, username);
                System.out.println(username + " wyszedł z czatu");
            }

            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}