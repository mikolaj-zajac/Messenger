package service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoggerService {

    private static final String REPORT_FILE = "Messenger/data/raport.txt";

    public static void write(String message) {
        File file = new File(REPORT_FILE);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
            String timeStamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            bw.write("[" + timeStamp + "] " + message);
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
