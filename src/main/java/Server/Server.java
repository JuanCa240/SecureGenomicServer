package Server;

import Storage.CsvManager;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Clase Server
 * ----------------------
 * Representa el servidor principal del sistema de detección genómica.
 * 
 * Funciones principales:
 * - Escucha conexiones entrantes en un puerto dado.
 * - Carga la base de enfermedades (desde archivos FASTA).
 * - Administra los pacientes y reportes usando CsvManager.
 * - Atiende múltiples clientes concurrentes mediante hilos (ExecutorService).
 * 
 */


public class Server {
    private CsvManager csvManager; 
    private int port;
    private ServerSocket serverSocket;          // Socket de servidor (bloquea hasta recibir cliente)
    private ExecutorService executorService;    // Pool de hilos para clientes concurrentes
    private DiseaseDatabase diseaseDatabase;    // Base de enfermedades cargada al inicio
    
     /**
     * Constructor del servidor.
     * Inicializa el pool de hilos, la base de enfermedades y el manejador CSV.
     *
     * @param port Puerto donde escuchará el servidor.
     */

    public Server(int port) {
        this.port = port;
        this.executorService = Executors.newCachedThreadPool(); // Crea hilos dinamicamente
        this.diseaseDatabase = new DiseaseDatabase();
        this.csvManager = new CsvManager("data/patients.csv", "data/reports.csv");

    }

     /**
     * Inicia el servidor:
     * - Crea el ServerSocket en el puerto configurado.
     * - Carga la base de enfermedades desde el directorio data/diseases.
     * - Pone al servidor en modo escucha para aceptar clientes.
     */
    
    public void start() {
        try {
             // Se crea el socket del servidor en el puerto especificado
            serverSocket = new ServerSocket(port);
            System.out.println("Servidor escuchando en el puerto " + port);

             // Cargar enfermedades antes de aceptar conexiones
            diseaseDatabase.loadDiseases("data/diseases"); 
            System.out.println("Base de enfermedades cargada: " + diseaseDatabase.getAll().size() + " enfermedades.");

            // Aceptar conexiones entrantes de clientes
            acceptConnections();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error al iniciar el servidor: " + e.getMessage());
        }
    }

     /**
     * Método principal del bucle de escucha.
     * Acepta conexiones entrantes y lanza un ConnectionHandler
     * en un hilo separado por cada cliente conectado.
     *
     * Esto permite que varios clientes interactúen en paralelo.
     */
    
    private void acceptConnections() {
        while (true) {
            try {
                
                // Esperar (bloqueante) a que un cliente se conecte
                Socket socket = serverSocket.accept();
                System.out.println("Nuevo cliente conectado.");

                // Crear un manejador de la conexión para este cliente
                ConnectionHandler handler = new ConnectionHandler(socket, diseaseDatabase, csvManager);

                // Ejecutar el handler en un hilo solo
                executorService.submit(handler);

            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Error aceptando conexión: " + e.getMessage());
            }
        }
    }
}

