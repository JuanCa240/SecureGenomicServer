package Server;

import Logging.LogManager;
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

/**
 * La clase "ConnectionHandler vendría siendo la clase controladora
 * ConnectionHandler maneja la comunicación entre el servidor y un cliente conectado. 
 * Implementa Runnable, ya que cada cliente se gestiona en un hilo independiente.
 */

public class ConnectionHandler implements Runnable {
    private Socket socket;                      // Representa la conexión con el cliente
    private BufferedReader inputStream;         //Flujo de entrada para recibir datos del cliente
    private PrintWriter outputStream;           //Flujo de salida para enviar datos al cliente
    private DiseaseDatabase diseaseDatabase;    //Administrador de operaciones sobre el CSV que almacena pacientes y reportes
    private CsvManager csvManager;
    private LogManager logManager;
    
    /**
     * Constructor: inicializa el handler de conexión.
     * 
     * @param socket          Socket del cliente
     * @param diseaseDatabase Base de datos de enfermedades
     * @param csvManager      Gestor de almacenamiento CSV
     */

    public ConnectionHandler(Socket socket, DiseaseDatabase diseaseDatabase,CsvManager csvManager){
        this.socket = socket;
        this.diseaseDatabase = diseaseDatabase;
        this.csvManager = csvManager; 
        this.logManager = new LogManager("data/server.log");

        try {
            // Se crean los flujos de entrada/salida con codificación UTF-8
            this.inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            this.outputStream = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            
            logManager.logInfo("Nueva conexión establecida con cliente: " + socket.getInetAddress());
        } catch (IOException e) {
            e.printStackTrace();
             logManager.logError("Error inicializando handler: " + e.getMessage());

        }
    }

     /**
     * Método principal que se ejecuta en el hilo del cliente.
     * Se queda escuchando solicitudes hasta que el cliente cierre la conexión.
     */
    
    @Override
    public void run() {
        try {
            String request;
             // Lee continuamente las solicitudes del cliente
            while ((request = inputStream.readLine()) != null) {
                logManager.logInfo("Solicitud recibida: " + request);
                processRequest(request);
            }
        } catch (IOException e) {
             logManager.logError("Error en handler: " + e.getMessage());
        } finally {
            try {
               // Cierra el socket al finalizar
                socket.close();
                logManager.logInfo("Conexión cerrada con cliente.");
            } catch (IOException e) {
                 logManager.logError("Error cerrando socket: " + e.getMessage());
            }
        }
    }

    /**
    * Procesa las solicitudes que llegan desde el cliente.
    * Cada solicitud se recibe como un String (request) y se analiza 
    * para determinar qué operación del protocolo ejecutar.
    * 
    * @param request cadena con la solicitud enviada por el cliente.
    */
    
    public void processRequest(String request) {
        // Crea paciente
        if (request.startsWith("CREATE_PATIENT")) { // --> Crear un nuevo paciente (llama a handleCreatePatient)
            logManager.logInfo("Procesando creación de paciente...");
            handleCreatePatient();
            
         //Consultar paciente por ID    
        } else if (request.startsWith("RETRIEVE_PATIENT")) { // --> Consultar un paciente por su ID.
            String[] parts = request.split(" ");
            if (parts.length == 2) {
                logManager.logInfo("Consultando paciente ID: " + parts[1]);
                handleRetrievePatient(parts[1]); // partes[1] es el ID
            } else {
                logManager.logError("Error 400: solicitud inválida en RETRIEVE_PATIENT");
                outputStream.println("ERROR 400 BAD_REQUEST"); // El formato es inválido
            }
            
        // Actualizar paciente
        } else if (request.startsWith("UPDATE_PATIENT")) {  // --> Actualizar la información de un paciente existente.
            logManager.logInfo("Actualización de paciente en proceso...");
            handleUpdatePatient();  // Actualizar la información de un paciente existente
           
        // Eliminar paciente por el ID (borrado lógico)
        } else if (request.startsWith("DELETE_PATIENT")) { // --> Eliminar (lógicamente) un paciente por su ID.
            String[] parts = request.split(" ");
            if (parts.length == 2) {
                 logManager.logInfo("Eliminando paciente ID: " + parts[1]);
                handleDeletePatient(parts[1]); // partes[1] es el ID.
            } else {
                logManager.logError("Error 400: solicitud inválida en DELETE_PATIENT");
                outputStream.println("ERROR 400 BAD_REQUEST");
            }
            
        // Comando no reconocido
        } else {
            logManager.logError("Comando desconocido recibido: " + request);
            outputStream.println("ERROR 400 UNKNOWN_COMMAND");
        }
    }

     /**
     * Crea un nuevo paciente a partir de la metadata y el archivo FASTA recibido.
     * Valida formato, checksum, y almacena en el CSV.
     */
        private void handleCreatePatient() {
            try {
                logManager.logInfo("Iniciando creación de paciente...");

                String line;
                Patient patient = null;
                String fastaChecksum = null;
                long fastaSize = 0;

                // Acumula la metadata recibida hasta "END_METADATA"
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

                // Validación del encabezado FASTA
                String fastaHeader = inputStream.readLine();
                if (fastaHeader == null || !fastaHeader.startsWith("START_FASTA")) {
                    outputStream.println("ERROR 422 INVALID_FASTA_HEADER");
                    logManager.logError("Fallo creación paciente: encabezado FASTA inválido.");
                    return;
                }

                long nbytes = Long.parseLong(fastaHeader.split(" ")[1]);
                File patientFasta = new File("data/patient_" + System.currentTimeMillis() + ".fasta");

                // Guardar archivo FASTA
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

                // Validaciones
                if (!FastaValidator.isValidFormat(patientFasta.getAbsolutePath())) {
                    outputStream.println("ERROR 422 INVALID_FASTA");
                    logManager.logError("Fallo creación paciente: archivo FASTA inválido.");
                    return;
                }

                String realChecksum = FastaValidator.calculateChecksum(patientFasta.getAbsolutePath());
                if (fastaChecksum != null && !fastaChecksum.equals(realChecksum)) {
                    outputStream.println("ERROR 422 CHECKSUM_MISMATCH");
                    logManager.logError("Fallo creación paciente: checksum no coincide.");
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
                    logManager.logError("Fallo creación paciente: falta document_id.");
                    return;
                }

                int patientId;
                try {
                    patientId = Integer.parseInt(documentID);
                } catch (NumberFormatException e) {
                    outputStream.println("ERROR 400 INVALID_DOCUMENT_ID");
                    logManager.logError("Fallo creación paciente: document_id inválido.");
                    return;
                }

                // Extraer más metadata
                String fullName = null, ageStr = null, sex = null, email = null, notes = null;
                for (String metaLine : metadata.toString().split("\n")) {
                    if (metaLine.startsWith("full_name:")) fullName = metaLine.split(":")[1].trim();
                    else if (metaLine.startsWith("age:")) ageStr = metaLine.split(":")[1].trim();
                    else if (metaLine.startsWith("sex:")) sex = metaLine.split(":")[1].trim();
                    else if (metaLine.startsWith("contact_email:")) email = metaLine.split(":")[1].trim();
                    else if (metaLine.startsWith("clinical_notes:")) notes = metaLine.split(":")[1].trim();
                }

                int age = 0;
                try {
                    age = Integer.parseInt(ageStr);
                } catch (NumberFormatException e) {
                    outputStream.println("ERROR 400 INVALID_AGE");
                    logManager.logError("Fallo creación paciente: edad inválida.");
                    return;
                }

                patient = new Patient(fullName, documentID, age, sex, email,
                        java.time.LocalDateTime.now(), notes, realChecksum, fastaSize);
                patient.setPatientID(patientId);

                csvManager.appendPatient(patient);
                outputStream.println("201 CREATED patient_id: " + patient.getPatientID());
                logManager.logInfo("Paciente creado exitosamente con ID: " + patientId);

                // Detectar enfermedades
                List<DetectionReport> reports = detectDiseases(patient, patientFasta.getAbsolutePath());
                for (DetectionReport r : reports) {
                    csvManager.appendReport(r);
                    outputStream.println("DETECTION " + r.toString());
                }

            } catch (IOException e) {
                e.printStackTrace();
                logManager.logError("Error en creación de paciente: " + e.getMessage());
                outputStream.println("ERROR 500 SERVER_ERROR");
            }
        }
   
        /**
        * Actualiza la información de un paciente existente.
        * Permite modificar metadata (nombre, edad, sexo, email, notas) 
        * y opcionalmente reemplazar su archivo FASTA.
        */

        private void handleUpdatePatient() {
            try {
                logManager.logInfo("Iniciando actualización de paciente...");

                String line;
                String patientId = null;
                Map<String, String> updates = new HashMap<>();
                long fastaSize = 0;

                while ((line = inputStream.readLine()) != null && !line.equals("END_METADATA")) {
                    if (line.startsWith("patient_id:")) 
                        patientId = line.split(":")[1].trim();
                    else if (line.startsWith("file_size_bytes:")) 
                        fastaSize = Long.parseLong(line.split(":")[1].trim());
                    else {
                        String[] parts = line.split(":");
                        if (parts.length == 2) updates.put(parts[0].trim(), parts[1].trim());
                    }
                }

                if (patientId == null) {
                    outputStream.println("ERROR 400 MISSING_PATIENT_ID");
                    logManager.logError("Fallo actualización: falta patient_id.");
                    return;
                }

                Patient p = csvManager.getPatientById(patientId);
                if (p == null) {
                    outputStream.println("ERROR 404 NOT_FOUND");
                    logManager.logError("Fallo actualización: paciente no encontrado (ID " + patientId + ")");
                    return;
                }

                for (Map.Entry<String, String> entry : updates.entrySet()) {
                    String k = entry.getKey();
                    String v = entry.getValue();
                    switch (k) {
                        case "full_name": p.setFullName(v); break;
                        case "age": p.setAge(Integer.parseInt(v)); break;
                        case "sex": p.setSex(v); break;
                        case "contact_email": p.setContactEmail(v); break;
                        case "clinical_notes": p.setClinicalNotes(v); break;
                    }
                }

                if (fastaSize > 0) {
                    String fastaHeader = inputStream.readLine();
                    if (fastaHeader == null || !fastaHeader.startsWith("START_FASTA")) {
                        outputStream.println("ERROR 422 INVALID_FASTA_HEADER");
                        logManager.logError("Fallo actualización: encabezado FASTA inválido.");
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
                        logManager.logError("Fallo actualización: archivo FASTA inválido.");
                        return;
                    }

                    String realChecksum = FastaValidator.calculateChecksum(patientFasta.getAbsolutePath());
                    p.setChecksumFasta(realChecksum);
                    p.setFileSizeBytes(fastaSize);
                }

                csvManager.updatePatient(p);
                outputStream.println("OK patient updated");
                logManager.logInfo("Paciente actualizado exitosamente: ID " + patientId);

            } catch (IOException e) {
                e.printStackTrace();
                logManager.logError("Error en actualización de paciente: " + e.getMessage());
                outputStream.println("ERROR 500 SERVER_ERROR");
            }
        }

        
     /**
     * Elimina (desactiva) un paciente del sistema según su ID.
     */

    private void handleDeletePatient(String patientId) {
        try {
            Patient p = csvManager.getPatientById(patientId);
            if (p != null) {
                csvManager.deactivatePatient(patientId);
                outputStream.println("OK patient deleted");
                logManager.logInfo("Paciente eliminado lógicamente: ID " + patientId);
            } else {
                outputStream.println("ERROR 404 NOT_FOUND");
                logManager.logError("Fallo eliminación: paciente no encontrado (ID " + patientId + ")");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logManager.logError("Error en eliminación de paciente: " + e.getMessage());
            outputStream.println("ERROR 500 SERVER_ERROR");
        }
    }



    /**
     * Recupera la información de un paciente por su ID.
     */
    
    private void handleRetrievePatient(String patientId) {
        Patient p = csvManager.getPatientById(patientId);
        if (p != null) {
            outputStream.println("OK\n" + p.toString());
            logManager.logInfo("Paciente consultado: ID " + patientId);
        } else {
            outputStream.println("ERROR 404 NOT_FOUND");
            logManager.logError("Fallo consulta: paciente no encontrado (ID " + patientId + ")");
        }
    }


    /**
     * Compara la secuencia genética de un paciente contra todas las enfermedades
     * almacenadas en la base de datos.
     * @param patient
     * @param fastaFilePath
     * @return 
     */

    public List<DetectionReport> detectDiseases(Patient patient, String fastaFilePath) {
        List<DetectionReport> reports = new ArrayList<>();

        try {
            
            // Leer y normalizar la secuencia genética del paciente
            String content = new String(Files.readAllBytes(new File(fastaFilePath).toPath()));
            String sequence = FastaValidator.normalize(content);

            //  Comparar contra todas las enfermedades registradas
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
