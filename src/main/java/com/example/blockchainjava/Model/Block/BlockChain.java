package com.example.blockchainjava.Model.Block;

import com.example.blockchainjava.Controller.ValidatorDashboardController;
import com.example.blockchainjava.Model.DAO.BlockDAO;
import com.example.blockchainjava.Model.DAO.TransactionDAO;
import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Model.Transaction.TransactionStatus;
import com.example.blockchainjava.Observer.BlockchainUpdateObserver;
import com.example.blockchainjava.Util.Network.SocketServer;

//import java.sql.Connection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.example.blockchainjava.Model.DAO.DatabaseConnection;

public class BlockChain {
    private final List<Block> chain;
    private final BlockDAO blockDAO;
    private final List<BlockchainUpdateObserver> observers;
    private final Connection connection;
    public void loadChainFromDatabase() {
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
    private int generateNewBlockId() {
        // Vous pouvez récupérer l'ID du dernier bloc de la base de données et l'incrémenter
        return blockDAO.getLastBlockId() + 1;
    }

    public void addBlock(Transaction transaction, String validatorSignature) {
        String previousHash = chain.isEmpty() ? "0" : chain.getLast().getCurrentHash();
        int newBlockId = generateNewBlockId();
        Block newBlock = new Block(newBlockId,previousHash, transaction, validatorSignature);
        chain.add(newBlock);
        blockDAO.saveBlock(newBlock);
        updateTransactionWithBlockIdAndStatus(transaction, newBlock);

        // Mettre à jour les soldes des utilisateurs dans la base de données
        TransactionDAO transactionDAO = new TransactionDAO();
        boolean balancesUpdated = transactionDAO.processTransactionBalances(transaction);
        if (!balancesUpdated) {
            throw new RuntimeException("Failed to update user balances after adding block.");
        }


        notifyObservers();
    }
    private void updateTransactionWithBlockIdAndStatus(Transaction transaction, Block newBlock) {
        System.out.println("voici bach necrasiw");
        System.out.println(transaction);
        String updateSql = "UPDATE transactions SET sender_id= ?, receiver_key = ?, amount = ?, block_id = ?, status = ?, signature=? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
            // Récupérer l'id du bloc créé et le mettre à jour dans la transaction

            stmt.setInt(1, transaction.getSenderId());  // sender_id
            stmt.setString(2, transaction.getReceiverKey());  // receiver_key
            stmt.setDouble(3, transaction.getAmount());  // amount
            stmt.setInt(4, newBlock.getBlockId());  // block_id (nouvel ID de bloc)
            stmt.setString(5, TransactionStatus.VALIDATED.name());  // status (changé en VALIDATED)
            stmt.setString(6,transaction.getSignature());
            stmt.setInt(7, transaction.getId());  // id (transaction ID)

            int rowsUpdated = stmt.executeUpdate();  // Utiliser executeUpdate() pour les requêtes de mise à jour

            if (rowsUpdated > 0) {
                System.out.println("Transaction mise à jour avec succès");
            } else {
                System.out.println("Aucune transaction mise à jour avec l'ID : " + transaction.getId());
            }

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
            Transaction transaction = block.getTransaction();
            // Vérifier si la transaction est en attente (PENDING)
            if (transaction.getStatus() == TransactionStatus.PENDING) {
                transactions.add(transaction);
            }
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
