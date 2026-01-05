package server;

public class ServerMain {
    public static void main(String[] args) {
        System.out.println("=== Messenger Chat Server ===");
        System.out.println("Starting on port 8080...");

        // Dodajemy shutdown hook dla czystego zamknięcia
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Server shutting down...");
        }));

        ChatServer.main(args);
    }
}