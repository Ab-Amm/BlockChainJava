package com.example.blockchainjava.Util.Network;

import com.example.blockchainjava.Model.Transaction.Transaction;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SocketClient {
    private final String host;
    private final int port;
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;

    public SocketClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        socket.connect(new InetSocketAddress(host, port), 30000); // Timeout de connexion : 5 secondes
        socket.setSoTimeout(30000);
        output = new ObjectOutputStream(socket.getOutputStream());
        input = new ObjectInputStream(socket.getInputStream());
    }

    public void sendTransaction(Transaction transaction) {
        try {
            // Convert transaction object to JSON
            String transactionJson = new ObjectMapper().writeValueAsString(transaction);

            System.out.println("Sending transaction JSON: " + transactionJson);

            // Send the JSON over the socket
            output.write(transactionJson.getBytes()); // Envoyer les octets bruts du JSON
            output.flush();
        } catch (Exception e) {
            System.out.println("Error sending transaction: " + e.getMessage());
            e.printStackTrace();
        }
    }



    public String receiveResponse() throws IOException, ClassNotFoundException {
        return (String) input.readObject();
    }

    public void close() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
