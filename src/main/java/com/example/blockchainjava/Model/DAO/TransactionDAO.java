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
        String sql = "INSERT INTO transactions (sender_id, receiver_key, amount, status, block_id, created_at) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, transaction.getSenderId());
            stmt.setString(2, transaction.getReceiverKey());
            stmt.setDouble(3, transaction.getAmount());
            stmt.setString(4, transaction.getStatus().toString());
            stmt.setObject(5, transaction.getBlockId() != null ? transaction.getBlockId() : null, Types.INTEGER);
            if (transaction.getCreatedAt() == null) {
                transaction.setCreatedAt(LocalDateTime.now());
            }

            stmt.setTimestamp(6, Timestamp.valueOf(transaction.getCreatedAt()));
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
}
