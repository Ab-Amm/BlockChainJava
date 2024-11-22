package com.example.blockchainjava.Model.DAO;

import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Model.Transaction.TransactionStatus;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {
    private final Connection connection;

    public TransactionDAO() {
        this.connection = DatabaseConnection.getConnection();
    }

    // Sauvegarder une transaction
    public void saveTransaction(Transaction transaction) {
        String sql = "INSERT INTO transactions (sender_id, receiver_key, amount, status, block_id, created_at, signature) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, transaction.getSenderId());
            stmt.setString(2, transaction.getReceiverKey());
            stmt.setDouble(3, transaction.getAmount());
            stmt.setString(4, transaction.getStatus().toString());
            stmt.setObject(5, transaction.getBlockId() != null ? transaction.getBlockId() : null, Types.INTEGER);

            // Vérifie si la date de création est null et assigne la date actuelle
            if (transaction.getCreatedAt() == null) {
                transaction.setCreatedAt(LocalDateTime.now());
            }
            stmt.setTimestamp(6, Timestamp.valueOf(transaction.getCreatedAt())); // Conversion LocalDateTime à Timestamp

            // Enregistrer la signature
            stmt.setString(7, transaction.getSignature());  // Ajouter la signature à l'insertion

            // Exécution de l'insertion
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save transaction", e);
        }
    }

    // Récupérer les transactions d'un utilisateur par ID ou clé publique
    public List<Transaction> getTransactionsByUser(int senderId, String receiverKey) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE sender_id = ? OR receiver_key = ? ORDER BY created_at DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, senderId);
            stmt.setString(2, receiverKey);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Transaction transaction = new Transaction(
                        rs.getInt("id"),
                        rs.getInt("sender_id"),
                        rs.getString("receiver_key"),
                        rs.getDouble("amount"),
                        TransactionStatus.valueOf(rs.getString("status")),
                        rs.getInt("block_id"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                );
                transactions.add(transaction);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load transactions", e);
        }
        return transactions;
    }

    // Mettre à jour le statut d'une transaction
    public void updateTransactionStatus(int transactionId, TransactionStatus status) {
        String sql = "UPDATE transactions SET status = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status.toString());
            stmt.setInt(2, transactionId);

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update transaction status", e);
        }
    }

    // Récupérer toutes les transactions
    public List<Transaction> getAllTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions ORDER BY created_at DESC";

        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                Transaction transaction = new Transaction(
                        rs.getInt("id"),
                        rs.getInt("sender_id"),
                        rs.getString("receiver_key"),
                        rs.getDouble("amount"),
                        TransactionStatus.valueOf(rs.getString("status")),
                        rs.getInt("block_id"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                );
                transactions.add(transaction);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load all transactions", e);
        }
        return transactions;
    }
    public Transaction getLatestTransaction(int senderId, String receiverPublicKey, double amount, TransactionStatus status) {
        try {
            String sql = "SELECT * FROM transactions WHERE sender_id = ? AND receiver_key = ? AND amount = ? AND status = ? ORDER BY id DESC LIMIT 1";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, senderId);
            stmt.setString(2, receiverPublicKey);
            stmt.setDouble(3, amount);
            stmt.setString(4, status.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Transaction(
                        rs.getInt("id"),
                        rs.getInt("sender_id"),
                        rs.getString("receiver_key"),
                        rs.getDouble("amount"),
                        TransactionStatus.valueOf(rs.getString("status")),
                        rs.getString("signature")
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    // Récupérer les transactions par statut
    public List<Transaction> getTransactionsByStatus(TransactionStatus status) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE status = ? ORDER BY created_at DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status.toString());

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Transaction transaction = new Transaction(
                        rs.getInt("id"),
                        rs.getInt("sender_id"),
                        rs.getString("receiver_key"),
                        rs.getDouble("amount"),
                        TransactionStatus.valueOf(rs.getString("status")),
                        rs.getInt("block_id"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                );
                transactions.add(transaction);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load transactions by status", e);
        }
        return transactions;
    }

    // Mettre à jour une transaction existante
    public void updateTransaction(Transaction transaction) {
        String sql = "UPDATE transactions SET sender_id = ?, receiver_key = ?, amount = ?, status = ?, block_id = ?, created_at = ?, signature = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, transaction.getSenderId());
            stmt.setString(2, transaction.getReceiverKey());
            stmt.setDouble(3, transaction.getAmount());
            stmt.setString(4, transaction.getStatus().toString());
            stmt.setObject(5, transaction.getBlockId(), Types.INTEGER);

            // Vérifie si la date de création est null et assigne la date actuelle
            if (transaction.getCreatedAt() == null) {
                transaction.setCreatedAt(LocalDateTime.now());
            }
            stmt.setTimestamp(6, Timestamp.valueOf(transaction.getCreatedAt())); // Conversion LocalDateTime à Timestamp

            // Mettre à jour la signature
            stmt.setString(7, transaction.getSignature());
            stmt.setInt(8, transaction.getId());

            // Exécution de la mise à jour
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update transaction", e);
        }
    }


}
