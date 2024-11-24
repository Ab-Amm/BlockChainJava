package com.example.blockchainjava.Util.Network;

import com.example.blockchainjava.Model.Transaction.Transaction;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    // Méthode pour envoyer une transaction sous forme JSON
    public void sendTransaction(Transaction transaction) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String transactionJson = objectMapper.writeValueAsString(transaction);
            System.out.println("Sending transaction JSON: " + transactionJson);

            sendData(transactionJson); // Utilise la méthode sendData pour envoyer le JSON
        } catch (IOException e) {
            System.err.println("Error sending transaction: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Méthode générique pour envoyer des données sous forme de chaîne
    public void sendData(String data) {
        try {
            System.out.println("Sending data: " + data);

            writer.write(data);
            writer.newLine(); // Marqueur de fin de message pour séparer les messages
            writer.flush();
        } catch (IOException e) {
            System.err.println("Error sending data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String receiveResponse() {
        try {
            String response = reader.readLine();
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