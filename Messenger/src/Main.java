import java.io.File;

public class Main {
    public static void main(String[] args) {
        // Wyczyść stare pliki online przy starcie
        cleanupOldOnlineFiles();

        new gui.LoginFrame();
    }

    private static void cleanupOldOnlineFiles() {
        File onlineDir = new File("Messenger/data/online");
        if (onlineDir.exists()) {
            File[] files = onlineDir.listFiles();
            if (files != null) {
                long currentTime = System.currentTimeMillis();
                for (File file : files) {
                    // Usuń pliki starsze niż 5 minut
                    if (currentTime - file.lastModified() > 300000) {
                        file.delete();
                    }
                }
            }
        }
    }
}