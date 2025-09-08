package Client;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.util.Scanner;

public class Client {
    private String nombreClient;
    private SSLSocket sslSocket;
    private Scanner consola;
    private PrintWriter output;
    private BufferedReader input;

    public Client(String nombreClient, String host, int port) {
        this.nombreClient = nombreClient;
        this.consola = new Scanner(System.in);
        try {
            // Crear socket seguro SSL
            
            System.setProperty("javax.net.ssl.trustStore", "C:\\Users\\Juan Camilo\\clienttruststore.jks");
            System.setProperty("javax.net.ssl.trustStorePassword", "password");

            
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            this.sslSocket = (SSLSocket) factory.createSocket(host, port);

            this.output = new PrintWriter(new OutputStreamWriter(sslSocket.getOutputStream(), "UTF-8"), true);
            this.input = new BufferedReader(new InputStreamReader(sslSocket.getInputStream(), "UTF-8"));

            System.out.println("Cliente " + nombreClient + " conectado al servidor " + host + ":" + port);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error conectando al servidor: " + e.getMessage());
        }
    }

    // Enviar metadata del paciente
    public void sendMetadata(String metadata) {
        output.println(metadata);
        output.flush();
    }

    // Enviar archivo FASTA completo
    public void sendFasta(String filePath) {
        try {
            File file = new File(filePath);
            long length = file.length();

            output.println("START_FASTA " + length);
            output.flush();

            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
                 OutputStream os = sslSocket.getOutputStream()) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = bis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error enviando FASTA: " + e.getMessage());
        }
    }

    // Recibir respuesta del servidor
    public String receiveResponse() {
        try {
            String response = input.readLine();
            if (response != null) {
                System.out.println("Servidor responde: " + response);
            }
            return response;
        } catch (IOException e) {
            e.printStackTrace();
            return "Error recibiendo respuesta";
        }
    }

    // Cerrar conexión
    public void close() {
        try {
            if (sslSocket != null) sslSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

