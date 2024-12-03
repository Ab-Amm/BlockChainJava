package com.example.blockchainjava.Model.DAO;

import com.example.blockchainjava.Model.User.*;
import com.example.blockchainjava.Util.Security.EncryptionUtil;
import com.example.blockchainjava.Util.Security.HashUtil;

import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserDAO {
    private final Connection connection;

    public UserDAO() {
        this.connection = DatabaseConnection.getConnection();
    }

    public Validator getValidatorData(int id) {
        String sql = """
        SELECT u.id , u.username, u.password, u.balance, v.ip_address, v.port
        FROM users u
        JOIN validators v ON u.id = v.id
        WHERE u.id = ? AND u.role = 'VALIDATOR'
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id); // Set the username parameter
            ResultSet rs = stmt.executeQuery(); // Execute the query

            if (rs.next()) {
                // Retrieve data from the ResultSet
                String username=rs.getString("username");
                String password = rs.getString("password");
                String ipAddress = rs.getString("ip_address");
                int port = rs.getInt("port");
                double balance = rs.getDouble("balance");

                // Create the Validator object using the new constructor
                Validator validator = new Validator(id ,username, ipAddress, port, balance);

                return validator; // Return the Validator object
            } else {
                System.out.println("No validator found with id: " + id);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load validator with id: " + id, e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error while creating validator object: " + id, e);
        }

        return null; // Return null if no validator was found
    }
    public Client getClientData(int id) {
        String sql = """
        SELECT u.id , u.username, u.password, u.balance
        FROM users u
        WHERE u.id = ? AND u.role = 'CLIENT'
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id); // Set the username parameter
            ResultSet rs = stmt.executeQuery(); // Execute the query

            if (rs.next()) {
                // Retrieve data from the ResultSet
                String username=rs.getString("username");
                String password = rs.getString("password");
                double balance = rs.getDouble("balance");

                // Create the Validator object using the new constructor
                Client client = new Client(id ,username ,password,balance);

                return client;
            } else {
                System.out.println("No validator found with id: " + id);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load validator with id: " + id, e);
        }

        return null; // Return null if no validator was found
    }
    public Admin getAdminData(int id) {
        String sql = """
        SELECT u.id , u.username, u.password, u.balance
        FROM users u
        WHERE u.id = ? AND u.role = 'ADMIN'
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id); // Set the username parameter
            ResultSet rs = stmt.executeQuery(); // Execute the query

            if (rs.next()) {
                // Retrieve data from the ResultSet
                String username=rs.getString("username");
                String password = rs.getString("password");
                double balance = rs.getDouble("balance");

                // Create the Validator object using the new constructor
                Admin admin = new Admin(id ,username ,password,balance);

                return admin;
            } else {
                System.out.println("No ADMIN found with id: " + id);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load ADMIN with id: " + id, e);
        }

        return null; // Return null if no validator was found
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
            stmt.setString(6, EncryptionUtil.encrypt(user.getPrivateKey()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user: " + user.getUsername(), e);
        } catch (Exception e) {
            throw new RuntimeException(e);
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
            stmt.setString(7, EncryptionUtil.encrypt(user.getPrivateKey()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user: " + user.getUsername(), e);
        } catch (Exception e) {
            throw new RuntimeException(e);
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

    // Track user connections
    private static final Map<Integer, LocalDateTime> activeUsers = new ConcurrentHashMap<>();

    public void updateUserConnection(int userId, boolean isConnected) {
        if (isConnected) {
            activeUsers.put(userId, LocalDateTime.now());
        } else {
            activeUsers.remove(userId);
        }
        updateUserConnectionStatus(userId, isConnected);
    }

    private void updateUserConnectionStatus(int userId, boolean isConnected) {
        String sql = "UPDATE users SET is_connected = ?, last_connection = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBoolean(1, isConnected);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(3, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating user connection status: " + e.getMessage());
        }
    }

    public int getConnectedValidatorsCount() {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'VALIDATOR' AND is_connected = true";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting connected validators count: " + e.getMessage());
        }
        return 0;
    }

    public int getConnectedClientsCount() {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'CLIENT' AND is_connected = true";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting connected clients count: " + e.getMessage());
        }
        return 0;
    }

    public void cleanupInactiveConnections(Duration timeout) {
        LocalDateTime cutoff = LocalDateTime.now().minus(timeout);
        activeUsers.entrySet().removeIf(entry -> {
            if (entry.getValue().isBefore(cutoff)) {
                updateUserConnection(entry.getKey(), false);
                return true;
            }
            return false;
        });
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
                user.setId(rs.getInt("id"));
                user.setBalance(rs.getDouble("balance"));
                user.setPublicKey(rs.getString("public_key"));
                user.setPrivateKey(EncryptionUtil.decrypt(rs.getString("private_key")));
                return user;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load user with username: " + username, e);
        } catch (IllegalStateException e) {
            throw new RuntimeException("Unknown role for user: " + username, e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }
    public User getUserById(Integer id) {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id); // Set the ID parameter
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // Retrieve the role to determine the user type
                UserRole role = UserRole.valueOf(rs.getString("role"));
                // Create the user object based on the role
                User user = createUserFromResultSet(rs, role);
                user.setId(id); // Set the user ID
                user.setBalance(rs.getDouble("balance")); // Set the user's balance
                user.setPublicKey(rs.getString("public_key"));
                user.setPrivateKey(EncryptionUtil.decrypt(rs.getString("private_key")));
                return user; // Return the constructed User object
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load user with ID: " + id, e);
        } catch (IllegalStateException e) {
            throw new RuntimeException("Unknown role for user with ID: " + id, e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null; // Return null if no user was found
    }


    private User createUserFromResultSet(ResultSet rs, UserRole role) throws Exception {
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
        user.setPrivateKey(EncryptionUtil.decrypt(rs.getString("private_key")));
        user.setBalance(rs.getDouble("balance"));
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
            stmt.setString(6, EncryptionUtil.encrypt(user.getPrivateKey()));
            stmt.setString(7, user.getUsername()); // Assuming you're updating by username

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("User " + user.getUsername() + " updated successfully.");
            } else {
                System.out.println("User " + user.getUsername() + " not found for update.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update user: " + user.getUsername(), e);
        } catch (Exception e) {
            throw new RuntimeException(e);
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
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getDouble("balance") ,
                        rs.getString("public_key"),
                        rs.getString("private_key")
                );
                String publicKey = rs.getString("public_key");
                System.out.println("Raw public_key from DB for user ID " + rs.getInt("id") + ": " + publicKey);

                if (publicKey == null || publicKey.isEmpty()) {
                    System.out.println("public_key is null or empty for user ID: " + rs.getInt("id"));
                }
                System.out.println("chno jbna mn bd");
                System.out.println(validator);
                validatorList.add(validator);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load validators", e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error while processing algorithm", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return validatorList;
    }
    public List<Client> getAllClients() {
        List<Client> clientList = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role = 'CLIENT'";  // Assurez-vous que la table 'users' existe

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();  // Exécuter la requête et obtenir le ResultSet

            while (rs.next()) {
                // Créer un objet Client à partir des données du ResultSet
                int userId = rs.getInt("id");
                String username = rs.getString("username");
                String role = rs.getString("role");
                double balance = rs.getDouble("balance");

                // Affichage des résultats extraits de la base de données
                System.out.println("Fetched client data - User ID: " + userId + ", Username: " + username + ", Role: " + role + ", Balance: " + balance);

                // Créer l'objet Client et définir ses propriétés
                UserRole userRole = UserRole.valueOf(role);
                Client client = new Client(userId ,username, rs.getString("password") , balance);

                // Ajouter l'objet Client à la liste
                clientList.add(client);
            }

        } catch (SQLException e) {
            System.err.println("SQLException: " + e.getMessage());
            throw new RuntimeException("Failed to load all clients", e);
        }

        // Affichage pour vérifier le contenu de la liste clientList
        System.out.println("Total number of clients fetched: " + clientList.size());
        for (Client client : clientList) {
            System.out.println("Client: " + client.getUsername() + ", Balance: " + client.getBalance());
        }

        // Retourner la liste des clients
        return clientList;
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
    public void updateAdminBalance(Admin admin, double newBalance) {
        String sql = "UPDATE users SET balance = ? WHERE id = ? AND role = 'ADMIN'";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDouble(1, newBalance);  // Définir le nouveau solde
            stmt.setInt(2, admin.getId());  // Utiliser le nom d'utilisateur du validateur

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("Le solde du admin " + admin.getUsername() + " a été mis à jour.");
            } else {
                System.out.println("Aucun admin trouvé avec l id : " + admin.getId());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Échec de la mise à jour du solde pour admin : " + admin.getUsername(), e);
        }
    }
    public boolean updateUserBalance(User user, double newBalance) {
        String sql = "UPDATE users SET balance = ? WHERE username = ? AND role = 'CLIENT'";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDouble(1, newBalance);  // Définir le nouveau solde
            stmt.setString(2, user.getUsername());  // Utiliser le nom d'utilisateur du validateur

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("Le solde du validateur " + user.getUsername() + " a été mis à jour.");
                return true;
            } else {
                System.out.println("Aucun validateur trouvé avec le nom d'utilisateur : " + user.getUsername());
                return false;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Échec de la mise à jour du solde pour le validateur : " + user.getUsername(), e);
        }
    }

    public void deleteValidator(Validator validator) {
        String deleteUserSQL = "DELETE FROM users WHERE username = ?";

        try (PreparedStatement stmtUser = connection.prepareStatement(deleteUserSQL)) {
            stmtUser.setString(1, validator.getUsername());
            int rowsDeletedUser = stmtUser.executeUpdate();

            if (rowsDeletedUser > 0) {
                System.out.println("Validator " + validator.getUsername() + " and related data deleted successfully.");
            } else {
                System.out.println("Validator " + validator.getUsername() + " not found for deletion.");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete validator: " + validator.getUsername(), e);
        }
    }
    public User findUserById(int clientId) {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, clientId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // Obtient le rôle de l'utilisateur depuis la base de données
                UserRole role = UserRole.valueOf(rs.getString("role"));
                // Crée un objet utilisateur à partir des résultats
                User user = createUserFromResultSet(rs, role);
                // Définit la clé publique et la clé privée
                user.setPublicKey(rs.getString("public_key"));
                user.setPrivateKey(EncryptionUtil.decrypt(rs.getString("private_key")));
                return user;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load user with ID: " + clientId, e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error processing algorithm", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null; // Retourne null si aucun utilisateur n'est trouvé
    }

    // Méthode utilitaire pour obtenir le nombre de lignes dans le ResultSet
    private int getRowCount(ResultSet rs) throws SQLException {
        rs.last(); // Aller à la dernière ligne
        int rowCount = rs.getRow(); // Obtenir le numéro de ligne
        rs.beforeFirst(); // Revenir au début du ResultSet
        return rowCount;
    }

}