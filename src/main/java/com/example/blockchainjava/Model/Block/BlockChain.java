package com.example.blockchainjava.Model.Block;

import com.example.blockchainjava.Controller.ValidatorDashboardController;
import com.example.blockchainjava.Model.DAO.BlockDAO;
import com.example.blockchainjava.Model.DAO.TransactionDAO;
import com.example.blockchainjava.Model.DAO.UserDAO;
import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Model.Transaction.TransactionStatus;
import com.example.blockchainjava.Model.User.Client;
import com.example.blockchainjava.Model.User.User;
import com.example.blockchainjava.Observer.BlockchainUpdateObserver;
import com.example.blockchainjava.Util.Network.SocketServer;

//import java.sql.Connection;
import java.security.PublicKey;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.example.blockchainjava.Model.DAO.DatabaseConnection;
import com.example.blockchainjava.Util.Security.HashUtil;
import com.example.blockchainjava.Util.Security.SecurityUtils;

public class BlockChain {
    private final List<Block> chain;
    private final BlockDAO blockDAO;
    private final List<BlockchainUpdateObserver> observers;
    private final Connection connection;
    private TransactionDAO transactionDAO;
    private UserDAO userDAO;
    public void loadChainFromDatabase() {
        List<Block> blocksFromDB = blockDAO.getAllBlocks();
        chain.addAll(blocksFromDB);
    }

    public BlockChain() {
        this.chain = new ArrayList<>();
        this.blockDAO = new BlockDAO();
        this.observers = new ArrayList<>();
        this.connection = DatabaseConnection.getConnection();
        this.transactionDAO = new TransactionDAO();
        this.userDAO = new UserDAO();
        loadChainFromDatabase();
    }
    private String calculateBlockHash(Block block) {
        return HashUtil.sha256(
                block.getPreviousHash() +
                        block.getTransaction().getId() +
                        block.getTimestamp().toString() +
                        block.getValidatorSignature()
        );
    }

    public boolean verifyBlockchain() {
        try {
            List<Block> blockchain = blockDAO.getAllBlocks(); // Récupérer tous les blocs depuis la base de données
            if (blockchain.isEmpty()) {
                System.out.println("La blockchain est vide.");
                return true;
            }

            String previousHash = "0"; // Hash initial pour le premier bloc
            for (Block block : blockchain) {
                // Recalculer le hachage actuel
                String recalculatedHash = block.calculateHash();

                // Vérifier l'intégrité du bloc
                if (!block.getCurrentHash().equals(recalculatedHash)) {
                    System.err.println("Le hash du bloc ID " + block.getBlockId() + " est invalide.");
                    restoreBlockData(block);
                    return false;
                }

                // Vérifier que le previous_hash correspond au current_hash du bloc précédent
                if (!block.getPreviousHash().equals(previousHash)) {
                    System.err.println("Le previous_hash du bloc ID " + block.getBlockId()  + " est invalide.");
                    restoreBlockData(block);
                    return false;
                }

                // Vérifier les transactions dans le bloc
                List<Transaction> transactions = transactionDAO.getTransactionsByBlockId(block.getBlockId() );
                for (Transaction transaction : transactions) {
                    // Vérifier la signature de la transaction
                    PublicKey senderPublicKey = SecurityUtils.decodePublicKey(
                            userDAO.getPublicKeyByUserId(transaction.getSenderId())
                    );
                    boolean isSignatureValid = SecurityUtils.verifySignature(
                            transaction.getDataToSign(),
                            transaction.getSignature(),
                            senderPublicKey
                    );

                    if (!isSignatureValid) {
                        System.err.println("La signature de la transaction ID " + transaction.getId() + " est invalide.");
                        restoreTransactionData(transaction);
                        return false;
                    }

                    // Vérifier que le solde de l'expéditeur est suffisant
                    User sender = userDAO.findUserById(transaction.getSenderId());
                    if (sender.getBalance() < transaction.getAmount()) {
                        System.err.println("Le solde de l'expéditeur pour la transaction ID " + transaction.getId() + " est insuffisant.");
                        return false;
                    }
                }

                // Mettre à jour le hash précédent pour le prochain bloc
                previousHash = block.getCurrentHash();
            }

            System.out.println("La blockchain est valide.");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur lors de la vérification de la blockchain : " + e.getMessage());
            return false;
        }
    }
    private void restoreTransactionData(Transaction transaction) {
        try {
            System.out.println("Restauration des données de la transaction ID " + transaction.getId());

            // Récupérer la transaction originale depuis la base de données (sauvegarde initiale)
            Transaction originalTransaction = transactionDAO.getTransactionById(transaction.getId());

            if (originalTransaction != null) {
                // Décoder la clé publique de l'expéditeur
                PublicKey senderPublicKey = SecurityUtils.decodePublicKey(
                        userDAO.getPublicKeyByUserId(originalTransaction.getSenderId())
                );

                // Vérifier la signature de la transaction
                boolean isSignatureValid = SecurityUtils.verifySignature(
                        originalTransaction.getDataToSign(),  // Les données à signer doivent être les mêmes
                        originalTransaction.getSignature(),
                        senderPublicKey
                );

                if (!isSignatureValid) {
                    System.err.println("La signature de la transaction ID " + transaction.getId() + " est invalide.");
                    return;  // Ne pas restaurer la transaction si la signature est invalide
                }

                // Restaurer les valeurs initiales de la transaction
                transaction.setSenderId(originalTransaction.getSenderId());
                transaction.setReceiverKey(originalTransaction.getReceiverKey());
                transaction.setAmount(originalTransaction.getAmount());
                transaction.setSignature(originalTransaction.getSignature());
                transaction.setCreatedAt(originalTransaction.getCreatedAt());

                // Mettre à jour la transaction restaurée dans la base de données
                transactionDAO.updateTransaction(transaction);

                System.out.println("Transaction restaurée avec succès ID " + transaction.getId());
            } else {
                System.err.println("Impossible de restaurer la transaction ID " + transaction.getId() + ": introuvable.");
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la restauration de la transaction ID " + transaction.getId() + ": " + e.getMessage());
        }
    }


    private void restoreBlockData(Block block) {
        System.out.println("Restoration du bloc ID " + block.getBlockId());
        try {
            // Récupérer les transactions liées au bloc depuis la base de données
            List<Transaction> transactions = transactionDAO.getTransactionsByBlockId(block.getBlockId());

            // Vérifier et restaurer chaque transaction si nécessaire
            List<Transaction> validTransactions = new ArrayList<>();
            for (Transaction transaction : transactions) {
                if (validateTransaction(transaction)) {
                    validTransactions.add(transaction);
                } else {
                    System.out.println("Restauration de la transaction ID " + transaction.getId());
                    restoreTransactionData(transaction);
                    Transaction restoredTransaction = transactionDAO.getTransactionById(transaction.getId());
                    if (restoredTransaction != null) {
                        validTransactions.add(restoredTransaction);
                    }
                }
            }

            // Recalculer le hash du bloc à partir des transactions valides
            String recalculatedHash = HashUtil.sha256(
                    block.getPreviousHash() +
                            validTransactions.stream()
                                    .map(Transaction::getId)
                                    .map(String::valueOf) // Assurez-vous que chaque ID est converti en String
                                    .collect(Collectors.joining(",")) + // Utilisation d'une virgule comme séparateur entre les IDs
                            block.getValidatorSignature()
            );


            // Mettre à jour le hash du bloc dans la base de données si nécessaire
            if (!recalculatedHash.equals(block.getCurrentHash())) {
                System.out.println("Mise à jour du hash du bloc ID " + block.getBlockId());
                block.setCurrentHash(recalculatedHash);
                blockDAO.updateBlock(block);
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la restauration du bloc ID " + block.getBlockId() + ": " + e.getMessage());
        }
    }
    public boolean validateTransaction(Transaction transaction) {
        try {
            // Vérifier la signature de la transaction
            PublicKey senderPublicKey = SecurityUtils.decodePublicKey(
                    userDAO.getPublicKeyByUserId(transaction.getSenderId())
            );
            boolean isSignatureValid = SecurityUtils.verifySignature(
                    transaction.getDataToSign(),
                    transaction.getSignature(),
                    senderPublicKey
            );

            if (!isSignatureValid) {
                System.err.println("La signature de la transaction ID " + transaction.getId() + " est invalide.");
                return false;
            }

            // Vérifier que le solde de l'expéditeur est suffisant
            User sender = userDAO.findUserById(transaction.getSenderId());
            if (sender.getBalance() < transaction.getAmount()) {
                System.err.println("Le solde de l'expéditeur pour la transaction ID " + transaction.getId() + " est insuffisant.");
                return false;
            }

            // Vérifier que la transaction n'est pas déjà validée ou annulée
            if (transaction.getStatus() != TransactionStatus.PENDING) {
                System.err.println("La transaction ID " + transaction.getId() + " n'est pas dans un état valide (elle est déjà " + transaction.getStatus() + ").");
                return false;
            }

            // Si tout est valide
            System.out.println("La transaction ID " + transaction.getId() + " est valide.");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur lors de la validation de la transaction ID " + transaction.getId() + ": " + e.getMessage());
            return false;
        }
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
        String updateSql = "UPDATE transactions SET sender_id= ?, receiver_key = ?, amount = ?, block_id = ?, status = ? , signature=? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
            // Récupérer l'id du bloc créé et le mettre à jour dans la transaction

            stmt.setInt(1, transaction.getSenderId());  // sender_id
            stmt.setString(2, transaction.getReceiverKey());  // receiver_key
            stmt.setDouble(3, transaction.getAmount());  // amount
            stmt.setInt(4, newBlock.getBlockId());  // block_id (nouvel ID de bloc)
            stmt.setString(5, TransactionStatus.VALIDATED.name());
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