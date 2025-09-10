
package Client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * Clase Client
 * ------------------
 * Representa un cliente que se conecta al servidor para enviar y recibir
 * información sobre pacientes. 
 *
 * Se encarga de Comunicación (TCP/SSL):
 * Abrir la conexión con el servidor.
 * Enviar metadatos de pacientes.
 * Enviar archivos FASTA completos.
 * Recibir respuestas del servidor.
 * Cerrar la conexión de manera segura.
 *
 * Este seria el puente que permite que el cliente hable con el servidor.
 */
public class Client {
    private String nombreClient;     
    private Socket socket;           // Conexión TCP al servidor
    private Scanner consola;         
    private PrintWriter output;      
    private BufferedReader input;   

    /**
     * Constructor del cliente.
     * 
     * @param nombreClient Nombre identificador del cliente.
     * @param host Dirección del servidor (ej: "localhost").
     * @param port Puerto del servidor (ej: 8443).
     *
     * Intenta conectarse al servidor. Si algo falla, muestra el error.
     */
    public Client(String nombreClient, String host, int port) {
        this.nombreClient = nombreClient;
        this.consola = new Scanner(System.in);
        try {
            // Crear el socket y establecer conexión
            this.socket = new Socket(host, port);

            // Inicializar canales de comunicación
            this.output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            this.input = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

            System.out.println("Cliente " + nombreClient + " conectado al servidor " + host + ":" + port);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error conectando al servidor: " + e.getMessage());
        }
    }

    /**
     * Envía metadatos (información textual) al servidor.
     * 
     * @param metadata String con la metadata a enviar (ej: "CREATE_PATIENT ...").
     */
    public void sendMetadata(String metadata) {
        output.println(metadata);
        output.flush();
    }

    /**
     * Envía un archivo FASTA completo al servidor.
     * 
     * @param filePath Ruta del archivo FASTA a enviar.
     *
     * Primero avisa al servidor del tamaño del archivo, luego envía el contenido en bloques.
     */
    public void sendFasta(String filePath) {
        File file = new File(filePath);
        long length = file.length();

        try {
            // Avisar al servidor que viene un archivo y su tamaño
            output.println("START_FASTA " + length);
            output.flush();

            // Enviar en bloques de 4KB
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
                OutputStream os = socket.getOutputStream();
                byte[] buffer = new byte[4096];
                long remaining = length;
                int read;

                while (remaining > 0 && (read = bis.read(buffer, 0, (int)Math.min(buffer.length, remaining))) != -1) {
                    os.write(buffer, 0, read);
                    remaining -= read;
                }
                os.flush(); // asegurar que todo llegó
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error enviando FASTA: " + e.getMessage());
        }
    }

    /**
     * Recibe la respuesta completa del servidor.
     * 
     * @return La respuesta como String. Puede contener varias líneas.
     *
     * Este método es útil porque no siempre la respuesta es una sola línea.
     */
    public String receiveFullResponse() {
        try {
            StringBuilder sb = new StringBuilder();
            String line;

            // Leer mientras haya datos disponibles
            while (input.ready() && (line = input.readLine()) != null) {
                sb.append(line).append("\n");
            }

            String response = sb.toString().trim();
            if (!response.isEmpty()) {
                System.out.println("📩 Servidor responde:\n" + response);
            }
            return response;
        } catch (IOException e) {
            e.printStackTrace();
            return "Error recibiendo respuesta";
        }
    }

    /**
     * Cierra la conexión con el servidor.
     * Siempre se debe llamar al final para liberar recursos.
     */
    public void close() {
        try {
            if (socket != null) socket.close();
            System.out.println("Conexión cerrada");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

