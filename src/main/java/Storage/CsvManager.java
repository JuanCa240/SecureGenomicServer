package Storage;

import Model.Patient;
import Model.DetectionReport;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * CsvManager
 * -------------------
 * Esta clase se encarga de manejar el almacenamiento de información de pacientes
 * y reportes de detección en archivos CSV.
 * 
 * Funciona como un gestor persistente simple que permite:
 *  Crear archivos CSV si no existen.
 *  Guardar pacientes y reportes de deteccion.
 *  Buscar, actualizar y desactivar pacientes.
 * 
 * Todos los métodos que escriben en archivos están con la función synchronized para
 * permitir uso seguro de múltiples hilos.
 */

public class CsvManager {
    private Path patientsFile;
    private Path reportsFile;

     /**
     * Constructor de CsvManager.
     * Crea los archivos CSV con encabezados si no existen.
     * 
     * @param patientsFilePath ruta del archivo de pacientes
     * @param reportsFilePath ruta del archivo de reportes
     */

    public CsvManager(String patientsFilePath, String reportsFilePath) {
        this.patientsFile = Paths.get(patientsFilePath);
        this.reportsFile = Paths.get(reportsFilePath);

        try {
            // Crear archivos si no existen
            if (!Files.exists(patientsFile)) {
                Files.createDirectories(patientsFile.getParent());
                Files.createFile(patientsFile);
                // Escribir encabezado
                Files.write(patientsFile, Collections.singletonList(
    "Patient ID,Document ID,Full Name,Age,Sex,Contact Email,Registration Date,Clinical Notes,File Size (bytes),FASTA Checksum,Active"));

            }
            if (!Files.exists(reportsFile)) {
                Files.createDirectories(reportsFile.getParent());
                Files.createFile(reportsFile);
                // Escribir encabezado
                Files.write(reportsFile, Collections.singletonList(
                    "Patient ID,Disease ID,Severity,Detection Date,Description"
                ));

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Guarda un paciente en el archivo CSV.
     * Reemplaza las comas en notas clínicas para evitar romper el formato CSV.
     *
     * @param p objeto Patient a guardar
     */
    
    public synchronized void appendPatient(Patient p) {
        String line = p.getPatientID() + "," +
        p.getDocumentID() + "," +
        p.getFullName() + "," +
        p.getAge() + "," +
        p.getSex() + "," +
        p.getContactEmail() + "," +
        p.getRegistrationDate().toLocalDate() + " " + p.getRegistrationDate().toLocalTime().withNano(0) + "," +
        p.getClinicalNotes().replace(",", ";") + "," +
        p.getFileSizeBytes() + "," +
        p.getChecksumFasta() + "," +
        "true";


        try (BufferedWriter writer = Files.newBufferedWriter(patientsFile, StandardOpenOption.APPEND)) {
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Guarda un reporte de detección en el CSV de reportes.
     * Reemplaza comas en la descripción.
     * 
     * @param r objeto DetectionReport a guardar
     */
    
    public synchronized void appendReport(DetectionReport r) {
        try (BufferedWriter writer = Files.newBufferedWriter(reportsFile, StandardOpenOption.APPEND)) {
            String line = r.getPatientId() + "," +
                          r.getDiseaseId() + "," +
                          r.getSeverity() + "," +
                          r.getDetectedAt().toLocalDate() + " " + r.getDetectedAt().toLocalTime().withNano(0) + "," +
                          r.getDescription().replace(",", ";");
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Busca un paciente activo por su ID.
     * Retorna null si no se encuentra o está inactivo.
     * 
     * @param id ID del paciente
     * @return Patient encontrado o null
     */
    
    public Patient getPatientById(String id) {
        try (BufferedReader reader = Files.newBufferedReader(patientsFile)) {
            String line;
            reader.readLine(); // saltar encabezado
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 11 && parts[0].equals(id) && parts[10].equals("true")) {
                    return parsePatient(parts);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Actualiza los datos de un paciente en el CSV.
     * Sobrescribe la línea correspondiente.
     * 
     * @param p objeto Patient con datos actualizados
     */
    
    public synchronized void updatePatient(Patient p) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(patientsFile)) {
            String header = reader.readLine();
            lines.add(header);

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts[0].equals(String.valueOf(p.getPatientID()))) {
                    // Reemplazar por versión nueva
                    lines.add(p.getPatientID() + "," +
                            p.getFullName() + "," + 
                            p.getDocumentID() + "," +
                            p.getAge() + "," +
                            p.getSex() + "," +
                            p.getContactEmail() + "," +
                            p.getRegistrationDate().toLocalDate() + " " + p.getRegistrationDate().toLocalTime().withNano(0) + "," +
                            p.getClinicalNotes().replace(",", ";") + "," +
                            p.getChecksumFasta() + "," +
                            p.getFileSizeBytes() + "," +
                            "true");
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //  ahora el reader ya está cerrado, se puede escribir sin bloquear
        try {
            Files.write(patientsFile, lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Desactiva un paciente en el CSV (borrado lógico).
     * Cambia la columna "Active" a false.
     * 
     * @param id ID del paciente a desactivar
     */
    
    public synchronized void deactivatePatient(String id) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(patientsFile)) {
            String header = reader.readLine();
            lines.add(header);

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 11) continue; // ignorar líneas corruptas
                if (parts[0].equals(id)) {
                    parts[10] = "false"; // marcar inactivo
                    lines.add(String.join(",", parts));
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // escribir después de cerrar el reader
        try {
            Files.write(patientsFile, lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
 
    /**
     * Convierte un arreglo de campos CSV en un objeto Patient.
     * Maneja parseo de tipos numéricos y fechas.
     * Retorna null si ocurre algún error.
     * 
     * @param parts 
     * @return 
     */
    
    private Patient parsePatient(String[] parts) {
        try {
            Patient p = new Patient(
                    parts[1],                      // fullName
                    parts[2],                      // documentID
                    Integer.parseInt(parts[3]),    // age
                    parts[4],                      // sex
                    parts[5],                      // contactEmail
                    LocalDateTime.parse(parts[6]), // registrationDate
                    parts[7],                      // clinicalNotes
                    parts[8],                      // checksumFasta
                    Long.parseLong(parts[9])       // fileSizeBytes
            );
            p.setPatientID(Integer.parseInt(parts[0]));
            return p;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
