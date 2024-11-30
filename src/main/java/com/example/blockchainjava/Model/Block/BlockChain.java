package com.example.blockchainjava.Model.Block;

import com.example.blockchainjava.Controller.ValidatorDashboardController;
import com.example.blockchainjava.Model.DAO.BlockDAO;
import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Model.Transaction.TransactionStatus;
import com.example.blockchainjava.Observer.BlockchainUpdateObserver;
import com.example.blockchainjava.Util.Network.SocketServer;

//import java.sql.Connection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import com.example.blockchainjava.Model.DAO.DatabaseConnection;

public class BlockChain {
    private final List<Block> chain;
    private final BlockDAO blockDAO;
    private final List<BlockchainUpdateObserver> observers;
    private final Connection connection;
    private void loadChainFromDatabase() {
        List<Block> blocksFromDB = blockDAO.getAllBlocks();
        chain.addAll(blocksFromDB);
    }

    public BlockChain() {
        this.chain = new ArrayList<>();
        this.blockDAO = new BlockDAO();
        this.observers = new ArrayList<>();
        this.connection = DatabaseConnection.getConnection();
        loadChainFromDatabase();
    }


    public void addBlock(Transaction transaction, String validatorSignature) {
        String previousHash = chain.isEmpty() ? "0" : chain.getLast().getCurrentHash();
        Block newBlock = new Block(previousHash, transaction, validatorSignature);
        chain.add(newBlock);
        blockDAO.saveBlock(newBlock);
        updateTransactionWithBlockIdAndStatus(transaction, newBlock);
        notifyObservers();
    }
    private void updateTransactionWithBlockIdAndStatus(Transaction transaction, Block newBlock) {
        String updateSql = "UPDATE transactions SET sender_id= ? , receiver_key = ? , amount = ? , block_id = ?, status = ? , created_at = ? , signature = ? , WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
            // Récupérer l'id du bloc créé et le mettre à jour dans la transaction

            stmt.setInt(1, transaction.getSenderId());  // sender_id
            stmt.setString(2, transaction.getReceiverKey());  // receiver_key
            stmt.setDouble(3, transaction.getAmount());  // amount
            stmt.setLong(4, newBlock.getBlockId());  // block_id (nouvel ID de bloc)
            stmt.setString(5, TransactionStatus.COMPLETED.name());  // status (changé en COMPLETED)
            stmt.setTimestamp(6, Timestamp.valueOf(transaction.getCreatedAt()));  // created_at (en format Timestamp)
            stmt.setString(7, transaction.getSignature());  // signature
            stmt.setInt(8, transaction.getId());  // id (transaction ID)
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update transaction status and block_id", e);
        }
    }

    private void notifyObservers() {
        for (BlockchainUpdateObserver observer : observers) {
            observer.onBlockchainUpdate(this);
        }
    }

    public Block getLatestBlock() {
        return chain.getLast();
    }


    public List<Block> getBlocks() {
        return chain;
    }


    public List<Transaction> getPendingTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        for (Block block : chain) {
            transactions.add(block.getTransaction());
        }
        return transactions;
    }

    public void addBlock(Block block) {
        chain.add(block);
    }

    public void addObserver(ValidatorDashboardController validatorDashboardController) {
        observers.add(validatorDashboardController);
    }

    public Boolean getBalanceBool(Integer sender) {
        for (Block block : chain) {
            if (block.getTransaction().getSenderId().equals(sender)) {
                return true;
            }
        }
        return false;
    }
    public double getBalance(Integer sender) {
        double balance = 0.0;
        for (Block block : chain) {
            if (block.getTransaction().getSenderId().equals(sender)) {
                balance =  balance + block.getTransaction().getAmount();
            }
        }
        return balance;
    }

    public boolean containsTransaction(Transaction transaction) {
        for (Block block : chain) {
            if (block.getTransaction().equals(transaction)) {
                return true;
            }
        }
        return false;
    }

}
