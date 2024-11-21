package com.example.blockchainjava.Util.Network;

import com.example.blockchainjava.Model.Block.Block;
import com.example.blockchainjava.Model.Block.BlockChain;
import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Model.Transaction.TransactionStatus;
import com.example.blockchainjava.Model.User.Validator;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static java.lang.foreign.MemorySegment.NULL;

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
        while (true) {
            try (Socket clientSocket = serverSocket.accept()) {
                ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
                Transaction transaction = (Transaction) input.readObject();

                // Validate transaction
                if (validateTransaction(transaction)) {
                    // Add to blockchain
                    Block block = new Block(blockchain.getLatestBlock().getCurrentHash(), transaction, validator.sign(transaction));
                    blockchain.addBlock(block);

                    // Send response back to client
                    sendResponse(clientSocket, "Transaction accepted");
                }
            } catch (Exception e) {
                // Handle errors
            }
        }
    }

    private void sendResponse(Socket clientSocket, String transactionAccepted) {
        try {
            ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
            output.writeObject(transactionAccepted);
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean validateTransaction(Transaction transaction) {
        // Check if the transaction is not null and has valid fields
        if (transaction == null || transaction.getSenderId() == null ) {
            return false;
        }

        // Verify that transaction amount is positive
        if (transaction.getAmount() <= 0) {
            return false;
        }

        // Verify that transaction is not already in the blockchain
        if (blockchain.containsTransaction(transaction)) {
            return false;
        }

        // Verify that transaction sender has enough balance
        if (!(blockchain.getBalance(transaction.getSenderId()) < transaction.getAmount())) {
            return false;
        }

        // Verify that transaction sender is not the same as the receiver
        if (transaction.getSenderId().equals(transaction.getReceiverKey())) {
            return false;
        }

        // Verify that transaction status is PENDING
        if (!transaction.getStatus().equals(TransactionStatus.PENDING)) {
            return false;
        }

        // Additional validation logic can be added here

        return true;
    }
}