package Logging;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Clase LogManager
 * -------------------
 * Esta clase se encarga de manejar un archivo de logs (registro de eventos).
 * 
 * Funciona como un diario del sistema, guardando:
 * Mensajes informativos (INFO)
 * Mensajes de error (ERROR)
 * 
 */
public class LogManager {
    private String logFile; // Ruta del archivo donde se escriben los logs

    /**
     * Constructor
     *
     * @param logFile Ruta (path) del archivo donde se guardarán los logs.
     */
    public LogManager(String logFile) {
        this.logFile = logFile;
    }

    /**
     * Registra un mensaje de tipo INFO en el log.
     * 
     * @param msg Mensaje a registrarse
     */
    public synchronized void logInfo(String msg) {
        writeLog("INFO", msg);
    }

    /**
     * Registra un mensaje de tipo ERROR en el log.
 
     * @param msg Mensaje a registrarse
     */
    public synchronized void logError(String msg) {
        writeLog("ERROR", msg);
    }

    /**
     * Método privado que realmente escribe en el archivo.
     * Añade la fecha, el nivel de log si es INFO o ERROR y el mensaje.
     *
     * @param level 
     * @param msg  
     */
    private void writeLog(String level, String msg) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(LocalDateTime.now() + " [" + level + "] " + msg);
            writer.newLine(); // salto de línea para cada entrada
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

