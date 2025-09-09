package Main;

import Client.Client;
import Server.Server;
import Model.Patient;
import java.io.File;

import java.time.LocalDateTime;
import java.util.Scanner;
import validation.FastaValidator;

public class Main {
    private static Scanner scanner = new Scanner(System.in);
    private static Server server;
    private static Client client;

    public static void main(String[] args) {
        while (true) {
            System.out.println("=====================================");
            System.out.println("      SISTEMA GENÓMICO SEGURO         ");
            System.out.println("=====================================");
            System.out.println("1) Iniciar Servidor");
            System.out.println("2) Conectar Cliente");
            System.out.println("3) Crear Paciente (desde Cliente)");
            System.out.println("4) Consultar Paciente (desde Cliente)");
            System.out.println("5) Actualizar Paciente (desde Cliente)");
            System.out.println("6) Eliminar Paciente (desde Cliente)");

            System.out.println("7) Salir");
            System.out.print("Seleccione una opción: ");
            String opcion = scanner.nextLine();

            switch (opcion) {
                case "1":
                    iniciarServidor();
                    break;
                case "2":
                    conectarCliente();
                    break;
                case "3":
                    crearPaciente();
                    break;
                case "4":
                    consultarPaciente();
                    break;
                case "5":
                    actualizarPaciente();
                    break;
                case "6":
                    eliminarPaciente();
                    break;
                case "7":
                    System.out.println("Saliendo del sistema...");
                    if (client != null) client.close();
                    System.exit(0);
                default:
                    System.out.println("Opción inválida. Intente de nuevo.");
            }
        }
    }

    // ✅ Opción 1: Iniciar Servidor
    private static void iniciarServidor() {
        if (server != null) {
            System.out.println("Servidor ya está en ejecución.");
            return;
        }
        System.out.print("Ingrese puerto para el servidor (ej: 8443): ");
        int port = Integer.parseInt(scanner.nextLine());

        server = new Server(port);

        // Arrancamos en un hilo aparte para no bloquear el menú
        new Thread(() -> server.start()).start();

        System.out.println("Servidor iniciado en puerto " + port);
    }

    // ✅ Opción 2: Conectar Cliente
    private static void conectarCliente() {
        if (client != null) {
            System.out.println("Cliente ya conectado.");
            return;
        }
        System.out.print("Ingrese host (ej: localhost): ");
        String host = scanner.nextLine();
        System.out.print("Ingrese puerto: ");
        int port = Integer.parseInt(scanner.nextLine());

        client = new Client("Cliente-1", host, port);
    }

    // ✅ Opción 3: Crear Paciente
    private static void crearPaciente() {
        if (client == null) {
            System.out.println("Primero debe conectar un cliente.");
            return;
        }

        System.out.print("Nombre completo: ");
        String name = scanner.nextLine();
        System.out.print("Documento ID: ");
        String docId = scanner.nextLine();
        System.out.print("Edad: ");
        int age = Integer.parseInt(scanner.nextLine());
        System.out.print("Sexo (M/F): ");
        String sex = scanner.nextLine();
        System.out.print("Email: ");
        String email = scanner.nextLine();
        System.out.print("Notas clínicas: ");
        String notes = scanner.nextLine();
        System.out.print("Ruta del archivo FASTA: ");
        String fastaPath = scanner.nextLine();

        // ⚡ Por simplicidad, no calculamos checksum aquí: lo hace el servidor
        Patient p = new Patient(name, docId, age, sex, email,
                LocalDateTime.now(), notes, "pending", 0);

        // Enviar metadata al servidor
        StringBuilder metadata = new StringBuilder();
        metadata.append("CREATE_PATIENT\n");
        metadata.append("full_name: ").append(p.getFullName()).append("\n");
        metadata.append("document_id: ").append(p.getDocumentID()).append("\n");
        metadata.append("age: ").append(p.getAge()).append("\n");
        metadata.append("sex: ").append(p.getSex()).append("\n");
        metadata.append("contact_email: ").append(p.getContactEmail()).append("\n");
        metadata.append("registration_date: ").append(p.getRegistrationDate()).append("\n");
        metadata.append("clinical_notes: ").append(p.getClinicalNotes()).append("\n");
        
        String checksum = FastaValidator.calculateChecksum(fastaPath);
        metadata.append("checksum_fasta: ").append(checksum).append("\n");

        metadata.append("file_size_bytes: ").append(new java.io.File(fastaPath).length()).append("\n");
        metadata.append("END_METADATA");

        client.sendMetadata(metadata.toString());
        client.sendFasta(fastaPath);

        // Leer respuesta
        client.receiveResponse();
    }

    // ✅ Opción 4: Consultar paciente
    private static void consultarPaciente() {
        if (client == null) {
            System.out.println("Primero debe conectar un cliente.");
            return;
        }
        System.out.print("Ingrese patientID: ");
        String pid = scanner.nextLine();

        client.sendMetadata("RETRIEVE_PATIENT " + pid);
        client.receiveResponse();
    }
    
    private static void actualizarPaciente() {
        if (client == null) {
            System.out.println("Primero debe conectar un cliente.");
            return;
        }

        System.out.print("Ingrese patientID a actualizar: ");
        String patientId = scanner.nextLine();

        System.out.print("Nombre completo (dejar vacío si no cambia): ");
        String name = scanner.nextLine();
        System.out.print("Edad (dejar vacío si no cambia): ");
        String ageStr = scanner.nextLine();
        System.out.print("Sexo (M/F, dejar vacío si no cambia): ");
        String sex = scanner.nextLine();
        System.out.print("Email (dejar vacío si no cambia): ");
        String email = scanner.nextLine();
        System.out.print("Notas clínicas (dejar vacío si no cambia): ");
        String notes = scanner.nextLine();
        System.out.print("Ruta del nuevo archivo FASTA (dejar vacío si no cambia): ");
        String fastaPath = scanner.nextLine();

        StringBuilder metadata = new StringBuilder();
        metadata.append("UPDATE_PATIENT\n");
        metadata.append("patient_id: ").append(patientId).append("\n");
        if (!name.isEmpty()) metadata.append("full_name: ").append(name).append("\n");
        if (!ageStr.isEmpty()) metadata.append("age: ").append(ageStr).append("\n");
        if (!sex.isEmpty()) metadata.append("sex: ").append(sex).append("\n");
        if (!email.isEmpty()) metadata.append("contact_email: ").append(email).append("\n");
        if (!notes.isEmpty()) metadata.append("clinical_notes: ").append(notes).append("\n");

        if (!fastaPath.isEmpty()) {
            File file = new File(fastaPath);
            metadata.append("file_size_bytes: ").append(file.length()).append("\n");
            metadata.append("START_FASTA ").append(file.length());
        }

        metadata.append("\nEND_METADATA");

        client.sendMetadata(metadata.toString());

        if (!fastaPath.isEmpty()) {
            client.sendFasta(fastaPath);
        }

        client.receiveResponse();
    }
    
    private static void eliminarPaciente() {
        if (client == null) {
            System.out.println("Primero debe conectar un cliente.");
            return;
        }

        System.out.print("Ingrese patientID a eliminar: ");
        String patientId = scanner.nextLine();

        client.sendMetadata("DELETE_PATIENT " + patientId);
        client.receiveResponse();
    }
}
