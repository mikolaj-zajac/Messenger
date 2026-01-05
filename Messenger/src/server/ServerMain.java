package server;

public class ServerMain {
    public static void main(String[] args) {
        System.out.println("=== Messenger Chat Server ===");
        System.out.println("Starting on port 8080...");
        System.out.println("Server directory: " + System.getProperty("user.dir"));

        // Dodajemy shutdown hook dla czystego zamknięcia
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Server shutting down...");
            System.out.println("Total connections handled: " + ChatServer.getOnlineCount());
        }));

        try {
            ChatServer.main(args);
        } catch (Exception e) {
            System.err.println("Server crashed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}