package Server;

import Model.Patient;
import Model.Disease;
import Model.DetectionReport;
import Storage.CsvManager;
import validation.FastaValidator;

import javax.net.ssl.SSLSocket;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ConnectionHandler implements Runnable {
    private SSLSocket socket;
    private BufferedReader inputStream;
    private PrintWriter outputStream;
    private DiseaseDatabase diseaseDatabase;
    private CsvManager csvManager;

    public ConnectionHandler(SSLSocket socket, DiseaseDatabase diseaseDatabase) {
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
            // simplificado
            outputStream.println("UPDATE not implemented yet");
        } else if (request.startsWith("DELETE_PATIENT")) {
            // simplificado
            outputStream.println("DELETE not implemented yet");
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

            // Luego esperar START_FASTA
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
                    if (read == -1) break;
                    fos.write(buffer, 0, read);
                    remaining -= read;
                }
            }

            // ✅ Validar formato
            if (!FastaValidator.isValidFormat(patientFasta.getAbsolutePath())) {
                outputStream.println("ERROR 422 INVALID_FASTA");
                return;
            }

            // ✅ Validar checksum
            String realChecksum = FastaValidator.calculateChecksum(patientFasta.getAbsolutePath());
            if (fastaChecksum != null && !fastaChecksum.equals(realChecksum)) {
                outputStream.println("ERROR 422 CHECKSUM_MISMATCH");
                return;
            }

            // ⚡ Simulación: crear paciente con datos básicos
            // (más adelante podemos mapear metadata real a objeto)
            patient = new Patient(
                    "Paciente X",
                    "DOC-" + System.currentTimeMillis(),
                    30,
                    "M",
                    "test@example.com",
                    java.time.LocalDateTime.now(),
                    "Notas clínicas",
                    realChecksum,
                    fastaSize
            );
            patient.setPatientID((int) (Math.random() * 10000));

            csvManager.appendPatient(patient);

            outputStream.println("OK patient_id: " + patient.getPatientID());

            // ✅ Detectar enfermedades inmediatamente
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

    // ✅ Recuperar paciente
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
