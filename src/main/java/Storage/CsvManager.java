package Storage;

import Model.Patient;
import Model.DetectionReport;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

public class CsvManager {
    private Path patientsFile;
    private Path reportsFile;

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
                        "patientID,fullName,documentID,age,sex,contactEmail,registrationDate,clinicalNotes,checksumFasta,fileSizeBytes,active"
                ));
            }
            if (!Files.exists(reportsFile)) {
                Files.createDirectories(reportsFile.getParent());
                Files.createFile(reportsFile);
                // Escribir encabezado
                Files.write(reportsFile, Collections.singletonList(
                        "patientId,diseaseId,severity,detectedAt,description"
                ));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ✅ Guardar paciente
    public synchronized void appendPatient(Patient p) {
        String line = p.getPatientID() + "," +
                p.getFullName() + "," +
                p.getDocumentID() + "," +
                p.getAge() + "," +
                p.getSex() + "," +
                p.getContactEmail() + "," +
                p.getRegistrationDate() + "," +
                p.getClinicalNotes().replace(",", ";") + "," +
                p.getChecksumFasta() + "," +
                p.getFileSizeBytes() + "," +
                "true";

        try (BufferedWriter writer = Files.newBufferedWriter(patientsFile, StandardOpenOption.APPEND)) {
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ✅ Guardar reporte
    public synchronized void appendReport(DetectionReport r) {
        try (BufferedWriter writer = Files.newBufferedWriter(reportsFile, StandardOpenOption.APPEND)) {
            writer.write(r.toString());
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ✅ Buscar paciente por ID
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

    // ✅ Actualizar paciente (simplificado: sobrescribe archivo)
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
                            p.getRegistrationDate() + "," +
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

        // ✅ ahora el reader ya está cerrado, se puede escribir sin bloquear
        try {
            Files.write(patientsFile, lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // ✅ Desactivar paciente (borrado lógico)
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

        // ✅ escribir después de cerrar el reader
        try {
            Files.write(patientsFile, lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

     
    // ✅ Convertir línea CSV en objeto Patient
    private Patient parsePatient(String[] parts) {
        try {
            Patient p = new Patient(
                    parts[1], // fullName
                    parts[2], // documentID
                    Integer.parseInt(parts[3]), // age
                    parts[4], // sex
                    parts[5], // contactEmail
                    LocalDateTime.parse(parts[6]), // registrationDate
                    parts[7], // clinicalNotes
                    parts[8], // checksumFasta
                    Long.parseLong(parts[9]) // fileSizeBytes
            );
            p.setPatientID(Integer.parseInt(parts[0]));
            return p;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
