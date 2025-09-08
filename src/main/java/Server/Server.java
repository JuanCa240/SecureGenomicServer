package Server;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.SSLSocket;

public class Server {
    private int port;
    private SSLServerSocket sslServerSocket;
    private ExecutorService executorService;
    private DiseaseDatabase diseaseDatabase;

    public Server(int port) {
        this.port = port;
        this.executorService = Executors.newCachedThreadPool(); // pool dinámico
        this.diseaseDatabase = new DiseaseDatabase();
    }

    // Inicializa y arranca el servidor
    public void start() {
        try {
            
            System.setProperty("javax.net.ssl.keyStore", "C:\\Users\\Juan Camilo\\serverkeystore.jks");
            System.setProperty("javax.net.ssl.keyStorePassword", "password");
            
            SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            sslServerSocket = (SSLServerSocket) factory.createServerSocket(port);

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
                var socket = sslServerSocket.accept();
                System.out.println("Nuevo cliente conectado.");

                // Crear un handler por cliente
                ConnectionHandler handler = new ConnectionHandler((SSLSocket) socket, diseaseDatabase);
                executorService.submit(handler);

            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Error aceptando conexión: " + e.getMessage());
            }
        }
    }
}
