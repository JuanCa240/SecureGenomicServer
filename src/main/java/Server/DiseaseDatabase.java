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

    /**
     * Constructor que inicializa la base de datos de enfermedades vacía.
     * @param folderPath
     */
    
    public void loadDiseases(String folderPath) {
        diseases.clear();

        try {
            DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(folderPath));
            for (Path path : stream) {
                String fileName = path.toString();
                if (fileName.endsWith(".fasta") || fileName.endsWith(".fa")) {
                    parseFastaFile(path);
                }
            }
        } catch (IOException e) {
            System.err.println("Error cargando enfermedades: " + e.getMessage());
        }
    }

    /**
     * Parsea un archivo FASTA y crea un objeto Disease a partir de su contenido.
     * 
     * @param path ruta del archivo FASTA
     */
    private void parseFastaFile(Path path) {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String header = reader.readLine();
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

    
    /**
     * Devuelve todas las enfermedades almacenadas en la base de datos.
     * 
     * @return lista de enfermedades
     */
    
    public List<Disease> getAll() {
        return diseases;
    }

     /**
     * Busca una enfermedad por su identificador único.
     * 
     * @param id identificador de la enfermedad
     * @return objeto Disease si existe, null en caso contrario
     */
    
    public Disease findById(String id) {
        for (Disease d : diseases) {
            if (d.getDiseaseId().equals(id)) {
                return d; 
            }
        }
        return null; 
    }
}
