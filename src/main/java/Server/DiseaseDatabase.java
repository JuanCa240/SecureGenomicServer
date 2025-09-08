package Server;


import Model.Disease;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class DiseaseDatabase {
    private List<Disease> diseases;

    public DiseaseDatabase() {
        this.diseases = new ArrayList<>();
    }

    // Cargar enfermedades desde un directorio con archivos FASTA
    public void loadDiseases(String folderPath) {
        diseases.clear(); // limpiar lista por si acaso

        try {
            Files.list(Paths.get(folderPath))
                    .filter(path -> path.toString().endsWith(".fasta") || path.toString().endsWith(".fa"))
                    .forEach(this::parseFastaFile);

        } catch (IOException e) {
            System.err.println("Error cargando enfermedades: " + e.getMessage());
        }
    }

    // Parsear un archivo FASTA y crear objeto Disease
    private void parseFastaFile(Path path) {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String header = reader.readLine(); // primera línea
            if (header == null || !header.startsWith(">")) {
                System.err.println("Archivo FASTA inválido: " + path);
                return;
            }

            // Ejemplo cabecera: >D001|COVID19|8
            String[] parts = header.substring(1).split("\\|");
            if (parts.length < 3) {
                System.err.println("Cabecera FASTA inválida en: " + path);
                return;
            }

            String diseaseId = parts[0];
            String name = parts[1];
            int severity = Integer.parseInt(parts[2]);

            // Leer secuencia completa
            StringBuilder sequence = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sequence.append(line.trim());
            }

            Disease disease = new Disease(diseaseId, name, severity, sequence.toString());
            diseases.add(disease);

            System.out.println("Enfermedad cargada: " + diseaseId + " (" + name + ")");
        } catch (IOException e) {
            System.err.println("Error leyendo archivo: " + path + " - " + e.getMessage());
        }
    }

    // Devolver todas las enfermedades
    public List<Disease> getAll() {
        return diseases;
    }

    // Buscar enfermedad por ID
    public Disease findById(String id) {
        return diseases.stream()
                .filter(d -> d.getDiseaseId().equals(id))
                .findFirst()
                .orElse(null);
        
    }
}
