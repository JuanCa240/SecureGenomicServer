package validation;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FastaValidator {

    // ✅ Verificar si el archivo FASTA tiene formato válido
    public static boolean isValidFormat(String filePath) {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
            String line = reader.readLine();

            // Validar que la primera línea empiece con '>'
            if (line == null || !line.startsWith(">")) {
                return false;
            }

            // Validar que las siguientes líneas contengan solo A, C, G, T, N
            while ((line = reader.readLine()) != null) {
                if (!line.matches("^[ACGTN]+$")) {
                    return false;
                }
            }
            return true;

        } catch (IOException e) {
            System.err.println("Error leyendo FASTA: " + e.getMessage());
            return false;
        }
    }

    // ✅ Calcular checksum SHA-256 de un archivo
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

            // Convertir hash en hexadecimal
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
    
        // ✅ Normalizar el contenido de un FASTA (mayúsculas, sin espacios ni saltos)
        public static String normalize(String fastaContent) {
            if (fastaContent == null) return "";
            return fastaContent
                    .toUpperCase()
                    .replaceAll("[^ACGTN]", ""); // elimina cualquier cosa que no sea A,C,G,T,N
        }   
}
