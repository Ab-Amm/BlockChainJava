package com.example.blockchainjava.Model.DAO;

import com.example.blockchainjava.Model.User.*;
import com.example.blockchainjava.Util.Security.HashUtil;

import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private final Connection connection;

    public UserDAO() {
        this.connection = DatabaseConnection.getConnection();
    }

    public void saveUser(User user) {
        String sql = "INSERT INTO users (username, role, created_at, password, public_key, private_key) VALUES (?, ?, ?, ?, ?, ?)";

        // Hash the password before storing
        String hashedPassword = HashUtil.hashPassword(user.getPassword());

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getRole().toString());
            stmt.setTimestamp(3, Timestamp.valueOf(user.getCreatedAt()));
            stmt.setString(4, hashedPassword);
            stmt.setString(5, user.getPublicKey());
            stmt.setString(6, user.getPrivateKey());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user: " + user.getUsername(), e);
        }
    }
    public void saveUser(User user , double balance ) {
        String sql = "INSERT INTO users (username, role, created_at, password, balance ,public_key, private_key) VALUES (?, ?, ?, ?, ?, ?, ?)";

        // Hash the password before storing
        String hashedPassword = HashUtil.hashPassword(user.getPassword());

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getRole().toString());
            stmt.setTimestamp(3, Timestamp.valueOf(user.getCreatedAt()));
            stmt.setString(4, hashedPassword);
            stmt.setDouble(5,balance);
            stmt.setString(6, user.getPublicKey());
            stmt.setString(7, user.getPrivateKey());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user: " + user.getUsername(), e);
        }
    }
    public void saveValidator(Validator validator, String ipAddress, int port) {
        Connection conn = null;
        PreparedStatement validatorStmt = null;

        String insertValidatorSQL = "INSERT INTO validators (id, ip_address, port) VALUES (?, ?, ?)";

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Transaction management

            // Get the user ID from the existing user in the database
            String getUserIdSQL = "SELECT id FROM users WHERE username = ?";
            PreparedStatement userStmt = conn.prepareStatement(getUserIdSQL);
            userStmt.setString(1, validator.getUsername());

            ResultSet rs = userStmt.executeQuery();
            if (!rs.next()) {
                throw new SQLException("User not found: " + validator.getUsername());
            }
            int userId = rs.getInt("id");

            // Insert into validators table
            validatorStmt = conn.prepareStatement(insertValidatorSQL);
            validatorStmt.setInt(1, userId);
            validatorStmt.setString(2, ipAddress);
            validatorStmt.setInt(3, port);
            int validatorRows = validatorStmt.executeUpdate();

            if (validatorRows == 0) {
                throw new SQLException("No rows inserted for validator");
            }

            conn.commit(); // Commit transaction
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback(); // Rollback on failure
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            System.err.println("SQL Error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save validator: " + validator.getUsername(), e);
        } finally {
            try {
                if (validatorStmt != null) validatorStmt.close();
                if (conn != null) conn.setAutoCommit(true);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }



    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                UserRole role = UserRole.valueOf(rs.getString("role"));
                User user = createUserFromResultSet(rs, role);
                // Set public and private keys
                user.setPublicKey(rs.getString("public_key"));
                user.setPrivateKey(rs.getString("private_key"));
                return user;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load user with username: " + username, e);
        } catch (IllegalStateException e) {
            throw new RuntimeException("Unknown role for user: " + username, e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private User createUserFromResultSet(ResultSet rs, UserRole role) throws SQLException, NoSuchAlgorithmException {
        User user = switch (role) {
            case CLIENT -> new Client(
                    rs.getString("username"),
                    rs.getString("password")
            );
            case VALIDATOR -> new Validator(
                    rs.getString("username"),
                    rs.getString("password")
            );
            case ADMIN -> new Admin(
                    rs.getString("username"),
                    rs.getString("password")
            );
            default -> throw new IllegalStateException("Unknown user role: " + role);
        };

        // Set public and private keys
        user.setPublicKey(rs.getString("public_key"));
        user.setPrivateKey(rs.getString("private_key"));

        return user;
    }
    public void updateUser(User user) {
        String sql = "UPDATE users SET username = ?, role = ?, created_at = ?, password = ?, public_key = ?, private_key = ? WHERE username = ?";

        // Hash the password before updating
        String hashedPassword = HashUtil.hashPassword(user.getPassword());

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getRole().toString());
            stmt.setTimestamp(3, Timestamp.valueOf(user.getCreatedAt()));
            stmt.setString(4, hashedPassword);
            stmt.setString(5, user.getPublicKey());
            stmt.setString(6, user.getPrivateKey());
            stmt.setString(7, user.getUsername()); // Assuming you're updating by username

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("User " + user.getUsername() + " updated successfully.");
            } else {
                System.out.println("User " + user.getUsername() + " not found for update.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update user: " + user.getUsername(), e);
        }
    }


    public List<Validator> getValidators() {
        List<Validator> validatorList = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role = 'VALIDATOR'";  // Adjust query based on your database schema

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                // Create a Validator object from the database result
                Validator validator = new Validator(
                        rs.getString("username"),
                        rs.getString("password")
                );
                // Set other properties if needed
                validator.setPublicKey(rs.getString("public_key"));
                validator.setPrivateKey(rs.getString("private_key"));
                // Add the Validator to the list
                validatorList.add(validator);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load validators", e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error while processing algorithm", e);
        }

        return validatorList;
    }
    public void registerValidator(Validator validator) {
        // Code pour enregistrer le validateur dans la base de données.
        // Cela pourrait inclure l'ajout de l'utilisateur en tant que validateur, par exemple.
        try {
            // Supposons que vous avez une méthode pour enregistrer un utilisateur dans la base de données
            saveUser(validator);  // Utilisez votre méthode d'enregistrement utilisateur ici
        } catch (Exception e) {
            e.printStackTrace();
            // Gérer l'exception selon les besoins
        }
    }
    public void updateValidatorBalance(Validator validator, double newBalance) {
        String sql = "UPDATE users SET balance = ? WHERE username = ? AND role = 'VALIDATOR'";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDouble(1, newBalance);  // Définir le nouveau solde
            stmt.setString(2, validator.getUsername());  // Utiliser le nom d'utilisateur du validateur

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("Le solde du validateur " + validator.getUsername() + " a été mis à jour.");
            } else {
                System.out.println("Aucun validateur trouvé avec le nom d'utilisateur : " + validator.getUsername());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Échec de la mise à jour du solde pour le validateur : " + validator.getUsername(), e);
        }
    }


    public void deleteValidator(Validator validator) {
        String deleteValidatorSQL = "DELETE FROM validators WHERE id = (SELECT id FROM users WHERE username = ?)";

        try (PreparedStatement stmt = connection.prepareStatement(deleteValidatorSQL)) {
            stmt.setString(1, validator.getUsername());

            int rowsDeleted = stmt.executeUpdate();
            if (rowsDeleted > 0) {
                System.out.println("Validator " + validator.getUsername() + " deleted successfully.");
            } else {
                System.out.println("Validator " + validator.getUsername() + " not found for deletion.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete validator: " + validator.getUsername(), e);
        }
    }


}
