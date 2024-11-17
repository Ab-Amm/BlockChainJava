package com.example.blockchainjava.Util.Network;

import com.example.blockchainjava.Model.Transaction.Transaction;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
        output = new ObjectOutputStream(socket.getOutputStream());
        input = new ObjectInputStream(socket.getInputStream());
    }

    public void sendTransaction(Transaction transaction) throws IOException {
        output.writeObject(transaction);
        output.flush();
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
