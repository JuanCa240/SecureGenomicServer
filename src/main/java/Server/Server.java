package Server;

import Storage.CsvManager;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {
    private CsvManager csvManager; 
    private int port;
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private DiseaseDatabase diseaseDatabase;

    public Server(int port) {
        this.port = port;
        this.executorService = Executors.newCachedThreadPool(); // pool dinámico
        this.diseaseDatabase = new DiseaseDatabase();
        this.csvManager = new CsvManager("data/patients.csv", "data/reports.csv");

    }

    // Inicializa y arranca el servidor
    public void start() {
        try {
            serverSocket = new ServerSocket(port);

            System.out.println("Servidor escuchando en el puerto " + port);

            // Cargar base de enfermedades al inicio
            diseaseDatabase.loadDiseases("data/diseases"); 
            System.out.println("Base de enfermedades cargada: " + diseaseDatabase.getAll().size() + " enfermedades.");

            acceptConnections();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error al iniciar el servidor: " + e.getMessage());
        }
    }

    // Acepta múltiples clientes concurrentes
    private void acceptConnections() {
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                System.out.println("Nuevo cliente conectado.");

                // Crear un handler por cliente
                ConnectionHandler handler = new ConnectionHandler(socket, diseaseDatabase, csvManager);

                executorService.submit(handler);

            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Error aceptando conexión: " + e.getMessage());
            }
        }
    }
}

