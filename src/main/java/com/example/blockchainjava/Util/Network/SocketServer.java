package com.example.blockchainjava.Util.Network;

import com.example.blockchainjava.Model.Block.Block;
import com.example.blockchainjava.Model.Block.BlockChain;
import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Model.Transaction.TransactionStatus;
import com.example.blockchainjava.Model.User.Validator;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketServer {
    private final BlockChain blockchain;
    private final ServerSocket serverSocket;
    private final Validator validator;

    public SocketServer(BlockChain blockchain, ServerSocket serverSocket, Validator validator) {
        this.blockchain = blockchain;
        this.serverSocket = serverSocket;
        this.validator = validator;
    }

    public void start() {
        System.out.println("SocketServer started on port: " + serverSocket.getLocalPort());
        while (true) {
            try (Socket clientSocket = serverSocket.accept()) {
                System.out.println("Accepted connection from: " + clientSocket.getInetAddress());

                // Process client request
                try (ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
                     ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream())) {

                    Transaction transaction = (Transaction) input.readObject();
                    System.out.println("Received transaction: " + transaction);

                    if (validateTransaction(transaction)) {
                        // Create a new block and add to the blockchain
                        Block block = new Block(blockchain.getLatestBlock().getCurrentHash(), transaction, validator.sign(transaction));
                        blockchain.addBlock(block);
                        System.out.println("Transaction validated and added to blockchain.");

                        // Send success response
                        output.writeObject("Transaction accepted");
                    } else {
                        // Send failure response
                        output.writeObject("Transaction rejected: Validation failed");
                    }
                    output.flush();
                }
            } catch (Exception e) {
                System.err.println("Error processing client request: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private boolean validateTransaction(Transaction transaction) {
        try {
            if (transaction == null) {
                System.err.println("Transaction is null.");
                return false;
            }

            if (transaction.getSenderId() == null || transaction.getReceiverKey() == null) {
                System.err.println("Transaction sender or receiver is null.");
                return false;
            }

            if (transaction.getAmount() <= 0) {
                System.err.println("Transaction amount must be greater than zero.");
                return false;
            }

            if (blockchain.containsTransaction(transaction)) {
                System.err.println("Transaction already exists in the blockchain.");
                return false;
            }

            if (blockchain.getBalance(transaction.getSenderId()) < transaction.getAmount()) {
                System.err.println("Sender has insufficient balance.");
                return false;
            }

            if (transaction.getSenderId().equals(transaction.getReceiverKey())) {
                System.err.println("Sender and receiver cannot be the same.");
                return false;
            }

            if (!TransactionStatus.PENDING.equals(transaction.getStatus())) {
                System.err.println("Transaction status is not PENDING.");
                return false;
            }

            // Additional validation logic can be added here
            return true;

        } catch (Exception e) {
            System.err.println("Error during transaction validation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
