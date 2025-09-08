package Logging;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

public class LogManager {
    private String logFile;

    public LogManager(String logFile) {
        this.logFile = logFile;
    }

    // ✅ Registrar mensaje informativo
    public synchronized void logInfo(String msg) {
        writeLog("INFO", msg);
    }

    // ✅ Registrar mensaje de error
    public synchronized void logError(String msg) {
        writeLog("ERROR", msg);
    }

    // ✅ Escribir en archivo
    private void writeLog(String level, String msg) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(LocalDateTime.now() + " [" + level + "] " + msg);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
    }
}
