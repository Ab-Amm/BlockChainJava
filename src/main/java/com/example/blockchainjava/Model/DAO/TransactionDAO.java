package com.example.blockchainjava.Model.DAO;

import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Model.Transaction.TransactionStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {
    private final Connection connection;

    public TransactionDAO() {
        this.connection = DatabaseConnection.getConnection();
    }

    public void saveTransaction(Transaction transaction) {
        String sql = "INSERT INTO transactions (transaction_id, sender, receiver, amount, status, block_id, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, transaction.getTransactionId());
            stmt.setString(2, transaction.getSender());
            stmt.setString(3, transaction.getReceiver());
            stmt.setDouble(4, transaction.getAmount());
            stmt.setString(5, transaction.getStatus().toString());
            stmt.setLong(6, transaction.getBlockId());
            stmt.setTimestamp(7, Timestamp.valueOf(transaction.getTimestamp()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save transaction", e);
        }
    }

    public List<Transaction> getTransactionsByUser(String userId) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE sender = ? OR receiver = ? ORDER BY timestamp DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setString(2, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Transaction transaction = new Transaction(
                        rs.getString("sender"),
                        rs.getString("receiver"),
                        rs.getDouble("amount"),
                        TransactionStatus.valueOf(rs.getString("status"))
                );
                // Set other properties
                transactions.add(transaction);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load transactions", e);
        }
        return transactions;
    }
}
