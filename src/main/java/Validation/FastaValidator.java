package validation;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Clase utilitaria para validar y procesar archivos FASTA.
 * 
 * Proporciona métodos estáticos para:
 * Verificar que un archivo FASTA tenga formato válido.
 * Calcular un checksum SHA-256 de un archivo FASTA.
 * Normaliza la secuencia de un archivo FASTA.
 * 
 * Esta clase es útil en sistemas genómicos para garantizar la integridad
 * de los datos y para realizar comparaciones de secuencias de manera confiable.
 */

public class FastaValidator {

     /**
     * Verifica que el archivo FASTA tenga un formato válido.
     * Un archivo válido debe lo siguiente:
     * Tener La primera línea que empieza con '>'.
     * Y Las siguientes líneas contener únicamente A, C, G, T o N (mayusculas).
     * 
     * @param filePath ruta del archivo FASTA
     * @return true si el archivo es válido, false si no lo es o ocurre un error de lectura
     */

    public static boolean isValidFormat(String filePath) {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
            String line = reader.readLine();

            
            if (line == null || !line.startsWith(">")) {
                return false;
            }

            // Validar que las siguientes líneas contengan solo A, C, G, T, N
            while ((line = reader.readLine()) != null) {
                line = line.toUpperCase().replaceAll("[^ACGTN]", "");
                if (line.isEmpty()) continue; 
            }

            return true;

        } catch (IOException e) {
            System.err.println("Error leyendo FASTA: " + e.getMessage());
            return false;
        }
    }

    /**
     * Calcula el checksum SHA-256 de un archivo.
     * Esto permite verificar la integridad del archivo FASTA.
     * 
     * @param filePath ruta del archivo a procesar
     * @return cadena hexadecimal con el hash SHA-256, o null si ocurre un error
     */
    
    public static String calculateChecksum(String filePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream fis = Files.newInputStream(Paths.get(filePath))) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();

        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
    
        /**
     * Normaliza el contenido de un FASTA.
     * Convierte todos los caracteres a mayúsculas y elimina cualquier carácter que no sea A, C, G, T o N.
     * 
     * Esto es útil para comparaciones de secuencias y detección de enfermedades.
     * 
     * @param fastaContent contenido del archivo FASTA como String
     * @return secuencia normalizada lista para análisis
     */
    
        public static String normalize(String fastaContent) {
            if (fastaContent == null) return "";
            return fastaContent
                    .toUpperCase()
                    .replaceAll("[^ACGTN]", ""); // elimina cualquier cosa que no sea A,C,G,T,N
        }   
}
