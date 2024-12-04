package com.example.blockchainjava.Model.User;

import com.example.blockchainjava.Model.DAO.DatabaseConnection;
import com.example.blockchainjava.Model.DAO.UserDAO;
import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Util.Security.AuthenticationUtil;
import com.example.blockchainjava.Util.Security.EncryptionUtil;
import com.example.blockchainjava.Util.Security.HashUtil;

import java.security.NoSuchAlgorithmException;
import java.sql.*;

public class Admin extends User {
    int Id ;
    private String publicKey;
    private String privateKey;
    public Admin(String username, String password) {
        super(username, password, UserRole.ADMIN);
    }
    public Admin(int Id, String username, String password , Double balance) {
        super(Id , username, password,balance , UserRole.ADMIN);
        this.Id=Id;
    }
    public Admin(int Id, String username, String password , Double balance , String pubkickey , String privatekey) {
        super(Id , username, password,balance , UserRole.ADMIN , pubkickey , privatekey);
        this.Id=Id;
        this.privateKey=privatekey;
        this.publicKey=pubkickey;
    }
    public String sign(Transaction transaction ,Admin admin) throws Exception {
        String transactionData = transaction.toString();
        System.out.println("l'admin va signer par ce private key");
        System.out.println(admin.getPrivateKey());
        return AuthenticationUtil.sign(transactionData, EncryptionUtil.decrypt(privateKey));
    }
    public String getPublicKey() {
        return publicKey;
    }

    // Getter for privateKey
    public String getPrivateKey() {
        return privateKey;
    }
    public int registerValidator(Validator validator, String ipAddress, int port) {
            if (validator == null || ipAddress == null || ipAddress.isEmpty() || port <= 0) {
                throw new IllegalArgumentException("Invalid arguments provided for registering validator.");
            }

            Connection conn = null;
            PreparedStatement userStmt = null;
            PreparedStatement validatorStmt = null;

            String insertUserSQL = "INSERT INTO users (username, role, created_at, password, balance, public_key, private_key) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
            String insertValidatorSQL = "INSERT INTO validators (id_user, ip_address, port) VALUES (?, ?, ?)";

            try {
                conn = DatabaseConnection.getConnection();
                conn.setAutoCommit(false); // Transaction management

                // Insert into users table
                userStmt = conn.prepareStatement(insertUserSQL, Statement.RETURN_GENERATED_KEYS);
                userStmt.setString(1, validator.getUsername());
                userStmt.setString(2, UserRole.VALIDATOR.toString());
                userStmt.setTimestamp(3, Timestamp.valueOf(validator.getCreatedAt()));
                userStmt.setString(4, HashUtil.hashPassword(validator.getPassword())); // Hash password securely
                userStmt.setDouble(5, validator.getBalance());
                userStmt.setString(6, validator.getPublicKey());
                userStmt.setString(7, validator.getPrivateKey());
                userStmt.executeUpdate();

                // Get generated user ID
                ResultSet rs = userStmt.getGeneratedKeys();
                if (!rs.next()) {
                    throw new SQLException("Failed to retrieve user ID for validator.");
                }
                int userId = rs.getInt(1);

                // Insert into validators table
                validatorStmt = conn.prepareStatement(insertValidatorSQL);
                validatorStmt.setInt(1, userId);
                validatorStmt.setString(2, ipAddress);
                validatorStmt.setInt(3, port);
                validatorStmt.executeUpdate();

                conn.commit(); // Commit transaction
                return userId; // Return the generated ID for the validator

            } catch (SQLException e) {
                try {
                    if (conn != null) conn.rollback(); // Rollback on failure
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
                throw new RuntimeException("Failed to register validator: " + validator.getUsername(), e);
            } finally {
                try {
                    if (userStmt != null) userStmt.close();
                    if (validatorStmt != null) validatorStmt.close();
                    if (conn != null) conn.setAutoCommit(true);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    @Override
    public String toString() {
        return "Admin{" +
                "id=" + getId() +
                ", username='" + getUsername() + '\'' +
                ", balance=" + getBalance() +
//                ", createdAt=" + getCreatedAt() +
                '}';
    }

}

