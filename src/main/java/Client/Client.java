package Client;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private String nombreClient;
    private Socket socket;
    private Scanner consola;
    private PrintWriter output;
    private BufferedReader input;

    public Client(String nombreClient, String host, int port) {
        this.nombreClient = nombreClient;
        this.consola = new Scanner(System.in);
        try {
            // Crear socket seguro SSL
            this.socket = new Socket(host, port);

            this.output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            this.input = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

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
        File file = new File(filePath);
        long length = file.length();

        try {
            output.println("START_FASTA " + length);
            output.flush();

            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
                OutputStream os = socket.getOutputStream();
                byte[] buffer = new byte[4096];
                long remaining = length;
                int read;

                while (remaining > 0 && (read = bis.read(buffer, 0, (int)Math.min(buffer.length, remaining))) != -1) {
                    os.write(buffer, 0, read);
                    remaining -= read;
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
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

