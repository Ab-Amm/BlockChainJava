package com.example.blockchainjava.Util.Network;

import com.example.blockchainjava.Model.Block.Block;
import com.example.blockchainjava.Model.Block.BlockChain;
import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Model.Transaction.TransactionStatus;
import com.example.blockchainjava.Model.User.Validator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class SocketServer {
    private final BlockChain blockchain;
    private final Validator validator;
    private final ObjectMapper objectMapper;

    public SocketServer(BlockChain blockchain, Validator validator) {
        this.blockchain = blockchain;
        this.validator = validator;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void start(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port: " + port);

            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                     PrintWriter writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true)) {

                    String transactionJson = reader.readLine();
                    if (transactionJson == null) {
                        writer.println("Error: No data received");
                        continue;
                    }

                    Transaction transaction = objectMapper.readValue(transactionJson, Transaction.class);

                    if (validateTransaction(transaction)) {
                        Block newBlock = new Block(blockchain.getLatestBlock().getCurrentHash(), transaction, validator.sign(transaction));
                        blockchain.addBlock(newBlock);
                        writer.println("Transaction accepted");
                    } else {
                        writer.println("Transaction rejected: Validation failed");
                    }
                } catch (IOException | IllegalArgumentException e) {
                    System.err.println("Error processing client request: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Server failed to start: " + e.getMessage());
        }
    }

    private boolean validateTransaction(Transaction transaction) {
        if (transaction == null || transaction.getAmount() <= 0 || transaction.getSenderId() == null || transaction.getReceiverKey() == null) {
            return false;
        }
        if (transaction.getSenderId().equals(transaction.getReceiverKey())) {
            return false;
        }
        if (!TransactionStatus.PENDING.equals(transaction.getStatus())) {
            return false;
        }
        return !blockchain.containsTransaction(transaction) &&
                blockchain.getBalance(transaction.getSenderId()) >= transaction.getAmount();
    }
}
