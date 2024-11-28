package com.example.blockchainjava.Model.DAO;

import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Model.Transaction.TransactionStatus;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionDAO {
    private final Connection connection;
    private Map<Integer, String> receiverUsernameMap = new HashMap<>();


    public TransactionDAO() {
        this.connection = DatabaseConnection.getConnection();
    }

    public String getReceiverUsername(int transactionId) {
        String receiverUsername = null;
        String sql = """
        SELECT u.username AS receiver_username
        FROM transactions t
        LEFT JOIN users u ON t.receiver_key = u.public_key
        WHERE t.id = ?
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, transactionId);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                receiverUsername = rs.getString("receiver_username");
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch receiver username for transaction ID: " + transactionId);
        }

        return receiverUsername;
    }

    public Transaction getTransactionById(int transactionId) {
        String sql = "SELECT * FROM transactions WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, transactionId);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // Créer une instance de Transaction avec les données principales
                Transaction transaction = new Transaction(
                        rs.getInt("id"),
                        rs.getInt("sender_id"),
                        rs.getString("receiver_key"),
                        rs.getDouble("amount"),
                        TransactionStatus.valueOf(rs.getString("status")),
                        rs.getObject("block_id", Integer.class), // Nullable block_id
                        rs.getTimestamp("created_at").toLocalDateTime()
                );



                return transaction;
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch transaction by ID: " + transactionId);
        }

        return null;
    }

    public List<Transaction> getTransactionsByClient(int clientId) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = """
        SELECT t.id, t.sender_id, t.receiver_key, t.amount, t.status, t.block_id, 
               t.created_at, u.username AS receiver_username
        FROM transactions t
        LEFT JOIN users u ON t.receiver_key = u.public_key
        WHERE t.sender_id = ? OR t.receiver_key = ?
        ORDER BY t.created_at DESC
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, clientId);
            stmt.setString(2, String.valueOf(clientId)); // Si nécessaire, ajustez le format pour receiver_key

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                // Créer une instance de Transaction avec les données principales
                Transaction transaction = new Transaction(
                        rs.getInt("id"),
                        rs.getInt("sender_id"),
                        rs.getString("receiver_key"),
                        rs.getDouble("amount"),
                        TransactionStatus.valueOf(rs.getString("status")),
                        rs.getObject("block_id", Integer.class), // Nullable block_id
                        rs.getTimestamp("created_at").toLocalDateTime()
                );

                // Stocker le `receiver_username` dans une Map côté contrôleur (sans l'ajouter à la classe Transaction)
                String receiverUsername = rs.getString("receiver_username");
                receiverUsernameMap.put(transaction.getId(), receiverUsername);

                transactions.add(transaction);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load transactions for client: " + clientId, e);
        }

        return transactions;
    }





    // Sauvegarder une transaction
    public void saveTransaction(Transaction transaction) {
        String sql = "INSERT INTO transactions (sender_id, receiver_key, amount, status, block_id, created_at, signature) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            // Remplir les valeurs pour l'insertion
            stmt.setInt(1, transaction.getSenderId());
            stmt.setString(2, transaction.getReceiverKey());
            stmt.setDouble(3, transaction.getAmount());
            stmt.setString(4, transaction.getStatus().toString());

            // Gérer le champ block_id (peut être null)
            if (transaction.getBlockId() != null) {
                stmt.setInt(5, transaction.getBlockId());
            } else {
                stmt.setNull(5, Types.INTEGER);
            }

            // Gérer la date de création (si null, utiliser la date actuelle)
            if (transaction.getCreatedAt() == null) {
                transaction.setCreatedAt(LocalDateTime.now());
            }
            stmt.setTimestamp(6, Timestamp.valueOf(transaction.getCreatedAt()));

            // Ajouter la signature
            stmt.setString(7, transaction.getSignature());

            // Exécuter la requête d'insertion
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new RuntimeException("Saving transaction failed, no rows affected.");
            }

            // Récupérer l'ID auto-généré
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    transaction.setId(generatedKeys.getInt(1)); // Assigner l'ID généré à l'objet Transaction
                } else {
                    throw new RuntimeException("Saving transaction failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save transaction: " + e.getMessage(), e);
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
    /*public void updateTransactionStatus(int transactionId, TransactionStatus status) {
        String sql = "UPDATE transactions SET status = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status.toString());
            stmt.setInt(2, transactionId);

            stmt.executeUpdate();
            System.out.println("l'id dde transaction : "+transaction.getId());
            return transaction.getId();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update transaction status", e);
        }
    }*/

    // Récupérer toutes les transactions
    public List<Map<String, Object>> getAllTransactions() {
        List<Map<String, Object>> transactions = new ArrayList<>();
        String query = """
    SELECT t.id, 
           t.amount, 
           t.status, 
           t.created_at,
           u1.username AS sender_name,
           u2.username AS receiver_name
    FROM transactions t
    JOIN users u1 ON t.sender_id = u1.id
    JOIN users u2 ON t.receiver_key = u2.public_key
    ORDER BY t.status, t.created_at DESC
    """;

        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> transactionData = new HashMap<>();
                transactionData.put("id", rs.getInt("id"));
                transactionData.put("amount", rs.getDouble("amount"));
                transactionData.put("status", rs.getString("status"));
                transactionData.put("createdAt", rs.getTimestamp("created_at").toLocalDateTime());
                transactionData.put("senderName", rs.getString("sender_name"));
                transactionData.put("receiverName", rs.getString("receiver_name"));

                transactions.add(transactionData);
            }
        } catch (Exception e) {
            e.printStackTrace();
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