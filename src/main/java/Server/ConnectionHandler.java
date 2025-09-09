package Server;

import Model.Patient;
import Model.Disease;
import Model.DetectionReport;
import Storage.CsvManager;
import validation.FastaValidator;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConnectionHandler implements Runnable {
    private Socket socket;
    private BufferedReader inputStream;
    private PrintWriter outputStream;
    private DiseaseDatabase diseaseDatabase;
    private CsvManager csvManager;

    public ConnectionHandler(Socket socket, DiseaseDatabase diseaseDatabase){
        this.socket = socket;
        this.diseaseDatabase = diseaseDatabase;
        this.csvManager = new CsvManager("data/patients.csv", "data/reports.csv");

        try {
            this.inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            this.outputStream = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error inicializando handler: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            String request;
            while ((request = inputStream.readLine()) != null) {
                processRequest(request);
            }
        } catch (IOException e) {
            System.err.println("Error en handler: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 🔑 Procesar comando del protocolo
    public void processRequest(String request) {
        if (request.startsWith("CREATE_PATIENT")) {
            handleCreatePatient();
        } else if (request.startsWith("RETRIEVE_PATIENT")) {
            String[] parts = request.split(" ");
            if (parts.length == 2) {
                handleRetrievePatient(parts[1]);
            } else {
                outputStream.println("ERROR 400 BAD_REQUEST");
            }
        } else if (request.startsWith("UPDATE_PATIENT")) {
            handleUpdatePatient(); // <-- aquí llamas al método real
        } else if (request.startsWith("DELETE_PATIENT")) {
            String[] parts = request.split(" ");
            if (parts.length == 2) {
                handleDeletePatient(parts[1]); // <-- aquí llamas al método real
            } else {
                outputStream.println("ERROR 400 BAD_REQUEST");
            }
        } else {
            outputStream.println("ERROR 400 UNKNOWN_COMMAND");
        }
    }

    // ✅ Crear paciente
    private void handleCreatePatient() {
            try {
                // Leer metadata hasta END_METADATA
                String line;
                Patient patient = null;
                String fastaChecksum = null;
                long fastaSize = 0;

                // Simplicidad: acumulamos en StringBuilder
                StringBuilder metadata = new StringBuilder();
                while ((line = inputStream.readLine()) != null && !line.equals("END_METADATA")) {
                    metadata.append(line).append("\n");

                    if (line.startsWith("checksum_fasta:")) {
                        fastaChecksum = line.split(":")[1].trim();
                    }
                    if (line.startsWith("file_size_bytes:")) {
                        fastaSize = Long.parseLong(line.split(":")[1].trim());
                    }
                }

                // Leer línea START_FASTA usando BufferedReader
                String fastaHeader = inputStream.readLine();
                if (fastaHeader == null || !fastaHeader.startsWith("START_FASTA")) {
                    outputStream.println("ERROR 422 INVALID_FASTA_HEADER");
                    return;
                }

                long nbytes = Long.parseLong(fastaHeader.split(" ")[1]);
                File patientFasta = new File("data/patient_" + System.currentTimeMillis() + ".fasta");

                try (FileOutputStream fos = new FileOutputStream(patientFasta)) {
                    InputStream is = socket.getInputStream();

                    byte[] buffer = new byte[4096];
                    long remaining = nbytes;

                    while (remaining > 0) {
                        int read = is.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                        if (read == -1) {
                            break; // fin de stream
                        }
                        fos.write(buffer, 0, read);
                        remaining -= read;
                    }

                    fos.flush();
                }

                if (!FastaValidator.isValidFormat(patientFasta.getAbsolutePath())) {
                    outputStream.println("ERROR 422 INVALID_FASTA");
                    return;
                }

                String realChecksum = FastaValidator.calculateChecksum(patientFasta.getAbsolutePath());
                if (fastaChecksum != null && !fastaChecksum.equals(realChecksum)) {
                    outputStream.println("ERROR 422 CHECKSUM_MISMATCH");
                    return;
                }
                
                String documentID = null;
                for (String metaLine : metadata.toString().split("\n")) {
                    if (metaLine.startsWith("document_id:")) {
                        documentID = metaLine.split(":")[1].trim();
                        break;
                    }
                }
                
                if (documentID == null) {
                    outputStream.println("ERROR 400 MISSING_DOCUMENT_ID");
                    return;
                }

                int patientId;
                try {
                    patientId = Integer.parseInt(documentID); 
                } catch (NumberFormatException e) {
                    outputStream.println("ERROR 400 INVALID_DOCUMENT_ID");
                    return;
                }
                
                // Crear paciente usando la metadata real
                String fullName = null;
                String ageStr = null;
                String sex = null;
                String email = null;
                String notes = null;

                for (String metaLine : metadata.toString().split("\n")) {
                    if (metaLine.startsWith("full_name:")) fullName = metaLine.split(":")[1].trim();
                    else if (metaLine.startsWith("age:")) ageStr = metaLine.split(":")[1].trim();
                    else if (metaLine.startsWith("sex:")) sex = metaLine.split(":")[1].trim();
                    else if (metaLine.startsWith("contact_email:")) email = metaLine.split(":")[1].trim();
                    else if (metaLine.startsWith("clinical_notes:")) notes = metaLine.split(":")[1].trim();
                }

                // Convertir age a int
                int age = 0;
                try {
                    age = Integer.parseInt(ageStr);
                } catch (NumberFormatException e) {
                    outputStream.println("ERROR 400 INVALID_AGE");
                    return;
                }

                // Ahora sí creamos el paciente
                patient = new Patient(
                        fullName,
                        documentID,
                        age,
                        sex,
                        email,
                        java.time.LocalDateTime.now(),
                        notes,
                        realChecksum,
                        fastaSize
                );

                patient.setPatientID(patientId);

                csvManager.appendPatient(patient);
                outputStream.println("OK patient_id: " + patient.getPatientID());

                List<DetectionReport> reports = detectDiseases(patient, patientFasta.getAbsolutePath());
                for (DetectionReport r : reports) {
                    csvManager.appendReport(r);
                    outputStream.println("DETECTION " + r.toString());
                }

            } catch (IOException e) {
                e.printStackTrace();
                outputStream.println("ERROR 500 SERVER_ERROR");
            }
        }

        private void handleUpdatePatient() {
        try {
            String line;
            String patientId = null;
            Map<String, String> updates = new HashMap<>();
            long fastaSize = 0;

            // Leer metadata hasta END_METADATA
            while ((line = inputStream.readLine()) != null && !line.equals("END_METADATA")) {
                if (line.startsWith("patient_id:")) {
                    patientId = line.split(":")[1].trim();
                } else if (line.startsWith("file_size_bytes:")) {
                    fastaSize = Long.parseLong(line.split(":")[1].trim());
                } else {
                    String[] parts = line.split(":");
                    if (parts.length == 2) {
                        updates.put(parts[0].trim(), parts[1].trim());
                    }
                }
            }

            if (patientId == null) {
                outputStream.println("ERROR 400 MISSING_PATIENT_ID");
                return;
            }

            Patient p = csvManager.getPatientById(patientId);
            if (p == null) {
                outputStream.println("ERROR 404 NOT_FOUND");
                return;
            }

            // Aplicar cambios
            updates.forEach((k,v) -> {
                switch(k) {
                    case "full_name": p.setFullName(v); break;
                    case "age": p.setAge(Integer.parseInt(v)); break;
                    case "sex": p.setSex(v); break;
                    case "contact_email": p.setContactEmail(v); break;
                    case "clinical_notes": p.setClinicalNotes(v); break;
                }
            });

            // Si hay FASTA nuevo
            if (fastaSize > 0) {
                String fastaHeader = inputStream.readLine();
                if (fastaHeader == null || !fastaHeader.startsWith("START_FASTA")) {
                    outputStream.println("ERROR 422 INVALID_FASTA_HEADER");
                    return;
                }

                long nbytes = Long.parseLong(fastaHeader.split(" ")[1]);
                File patientFasta = new File("data/patient_" + patientId + "_updated.fasta");
                try (FileOutputStream fos = new FileOutputStream(patientFasta)) {
                    InputStream is = socket.getInputStream();
                    byte[] buffer = new byte[4096];
                    long remaining = nbytes;
                    while (remaining > 0) {
                        int read = is.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                        if (read == -1) break;
                        fos.write(buffer, 0, read);
                        remaining -= read;
                    }
                    fos.flush();
                }

                if (!FastaValidator.isValidFormat(patientFasta.getAbsolutePath())) {
                    outputStream.println("ERROR 422 INVALID_FASTA");
                    return;
                }

                String realChecksum = FastaValidator.calculateChecksum(patientFasta.getAbsolutePath());
                p.setChecksumFasta(realChecksum);
                p.setFileSizeBytes(fastaSize);
            }

            csvManager.updatePatient(p);
            outputStream.println("OK patient updated");

        } catch (IOException e) {
            e.printStackTrace();
            outputStream.println("ERROR 500 SERVER_ERROR");
        }
    }

    private void handleDeletePatient(String patientId) {
        try {
            Patient p = csvManager.getPatientById(patientId);
            if (p != null) {
                csvManager.deactivatePatient(patientId);
                outputStream.println("OK patient deleted");
            } else {
                outputStream.println("ERROR 404 NOT_FOUND");
            }
        } catch (Exception e) {
            e.printStackTrace();
            outputStream.println("ERROR 500 SERVER_ERROR");
        }
    }


    private void handleRetrievePatient(String patientId) {
        Patient p = csvManager.getPatientById(patientId);
        if (p != null) {
            outputStream.println("OK");
            outputStream.println("patient_id: " + p.getPatientID());
            outputStream.println("full_name: " + p.getFullName());
            outputStream.println("document_id: " + p.getDocumentID());
            outputStream.println("age: " + p.getAge());
            outputStream.println("sex: " + p.getSex());
            outputStream.println("contact_email: " + p.getContactEmail());
            outputStream.println("registration_date: " + p.getRegistrationDate());
            outputStream.println("clinical_notes: " + p.getClinicalNotes());
            outputStream.println("checksum_fasta: " + p.getChecksumFasta());
            outputStream.println("file_size_bytes: " + p.getFileSizeBytes());
        } else {
            outputStream.println("ERROR 404 NOT_FOUND");
        }
    }

    // ✅ Comparar genoma paciente vs enfermedades
    public List<DetectionReport> detectDiseases(Patient patient, String fastaFilePath) {
        List<DetectionReport> reports = new ArrayList<>();

        try {
            String content = new String(Files.readAllBytes(new File(fastaFilePath).toPath()));
            String sequence = FastaValidator.normalize(content);

            for (Disease d : diseaseDatabase.getAll()) {
                if (sequence.contains(d.getSequence())) {
                    reports.add(new DetectionReport(
                            String.valueOf(patient.getPatientID()),
                            d.getDiseaseId(),
                            d.getSeverity(),
                            "Coincidencia encontrada con " + d.getName()
                    ));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return reports;
    }
}
