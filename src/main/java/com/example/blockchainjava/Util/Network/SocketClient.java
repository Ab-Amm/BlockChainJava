package com.example.blockchainjava.Util.Network;

import com.example.blockchainjava.Model.Transaction.Transaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SocketClient {
    private final String host;
    private final int port;
    private Socket socket;
    private BufferedWriter writer;
    private BufferedReader reader;

    public SocketClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 30000);
        socket.setSoTimeout(30000);
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public void sendTransaction(Transaction transaction) {
        try {
            // Initialiser l'ObjectMapper et enregistrer le module pour Java 8 Date/Time API
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.findAndRegisterModules();
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false); // Format ISO-8601 pour les dates

            // Convertir l'objet transaction en JSON
            String transactionJson = objectMapper.writeValueAsString(transaction);

            // Envoyer le JSON via le PrintWriter
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(transactionJson);

            // Si vous utilisez également un BufferedWriter
            writer.write(transactionJson);
            writer.newLine(); // Ajout d'une ligne pour séparer les messages si nécessaire
            writer.flush();

            // Log de confirmation
            System.out.println("JSON sent to server: " + transactionJson);
        } catch (IOException e) {
            System.err.println("Error sending transaction: " + e.getMessage());
            e.printStackTrace();
        }
    }



    public String receiveResponse() {
        try {
            socket.setSoTimeout(30000); // Timeout de 30 secondes
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Lire la réponse une seule fois
            String response = in.readLine();

            // Afficher et retourner la même réponse
            System.out.println("Received response: " + response);
            return response;

        } catch (IOException e) {
            System.err.println("Error receiving response: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


    public void close() {
        try {
            if (writer != null) writer.close();
            if (reader != null) reader.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing socket: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
