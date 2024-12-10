package com.example.blockchainjava.Model.DAO;

import com.example.blockchainjava.Model.User.*;
import com.example.blockchainjava.Util.Security.EncryptionUtil;
import com.example.blockchainjava.Util.Security.HashUtil;
import com.example.blockchainjava.Util.RedisUtil;
import com.example.blockchainjava.Util.RedisUtil;

import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import redis.clients.jedis.Jedis;

public class UserDAO {
    private final Connection connection;

    public UserDAO() {
        this.connection = DatabaseConnection.getConnection();
    }

    public Client getClientByPublicKey(String publicKey) {
//        // Check Redis cache first
//        Double cachedBalance = RedisUtil.getBalanceByPublicKey(publicKey);
//        if (cachedBalance != null) {
//            System.out.println("Retrieved balance from Redis for public key: " + publicKey);
//        }

        // SQL query to get client info
        String sql = """
        SELECT id, username, password, balance, public_key, private_key
        FROM users 
        WHERE public_key = ? AND role = 'CLIENT'
    """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, publicKey);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                int id = resultSet.getInt("id");
                String username = resultSet.getString("username");
                String password = resultSet.getString("password");
                double balance = resultSet.getDouble("balance");
                String retrievedPublicKey = resultSet.getString("public_key");
                String privateKey = resultSet.getString("private_key");


                Double cachedBalance = RedisUtil.getUserBalance(id);

                if (cachedBalance == null) {
                    RedisUtil.setUserBalance(id, balance);
                    cachedBalance = RedisUtil.getUserBalance(id);
                    System.out.println("Updated balance in Redis for public key: " + retrievedPublicKey);
                }

                // Create and return the Client object
                return new Client(id, username, password, cachedBalance, retrievedPublicKey, privateKey);
            } else {
                System.out.println("No client found with public key: " + publicKey);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve client data for public key: " + publicKey, e);
        }

        return null; // Return null if no client was found
    }


    public Validator getValidatorData(int id) {
        // Check Redis cache first
        Double cachedBalance = RedisUtil.getUserBalance(id);
        if (cachedBalance != null) {
            System.out.println("Retrieved balance from Redis for validator ID: " + id);
        }

        String sql = """
        SELECT u.id , u.username, u.password, v.ip_address, v.port
        FROM users u
        JOIN validators v ON u.id = v.id
        WHERE u.id = ? AND u.role = 'VALIDATOR'
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id); // Set the username parameter
            ResultSet rs = stmt.executeQuery(); // Execute the query

            if (rs.next()) {
                // Retrieve data from the ResultSet
                String username = rs.getString("username");
                String password = rs.getString("password");
                String ipAddress = rs.getString("ip_address");
                int port = rs.getInt("port");
                double balance = cachedBalance != null ? cachedBalance : rs.getDouble("balance");

                // Update Redis if the balance was retrieved from the database
                if (cachedBalance == null) {
                    RedisUtil.setUserBalance(id, balance);
                    System.out.println("Updated balance in Redis for validator ID: " + id);
                }

                // Create the Validator object using the new constructor
                Validator validator = new Validator(id, username, ipAddress, port, balance);

                return validator; // Return the Validator object
            } else {
                System.out.println("No validator found with id: " + id);
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to load validator with id: " + id, e);
        }

        return null; // Return null if no validator was found
    }

    public Client getClientData(int id) {
        String sql = """
        SELECT u.id, u.username, u.password, u.balance
        FROM users u
        WHERE u.id = ? AND u.role = 'CLIENT'
    """;

        // Check if the balance is cached in Redis
        Double cachedBalance = RedisUtil.getUserBalance(id);
        if (cachedBalance != null) {
            // Cache hit: Fetch username and password from the database
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String username = rs.getString("username");
                    String password = rs.getString("password");

                    // Return the client object with the cached balance
                    return new Client(id, username, password, cachedBalance);
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to load client with id: " + id, e);
            }
        } else {
            // Cache miss: Query the database and store the balance in Redis
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String username = rs.getString("username");
                    String password = rs.getString("password");
                    double balance = rs.getDouble("balance");

                    // Cache the balance in Redis
                    RedisUtil.setUserBalance(id, balance);

                    // Return the client object
                    return new Client(id, username, password, balance);
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to load client with id: " + id, e);
            }
        }

        // Return null if no client was found
        return null;
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
            stmt.setString(2, user.getRole() != null ? user.getRole().toString() : null); // Optionnel si le rôle est nul
            stmt.setTimestamp(3, Timestamp.valueOf(user.getCreatedAt()));
            stmt.setString(4, hashedPassword);
            stmt.setString(5, user.getPublicKey());
            System.out.println("voici private key :" + user.getPrivateKey());
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

        // Handle balance with Redis
        int userId = rs.getInt("id");
        Double cachedBalance = RedisUtil.getUserBalance(userId);
        if (cachedBalance != null) {
            System.out.println("Retrieved balance from Redis for user ID: " + userId);
            user.setBalance(cachedBalance);
        } else {
            double dbBalance = rs.getDouble("balance");
            user.setBalance(dbBalance);
            RedisUtil.setUserBalance(userId, dbBalance);
            System.out.println("Updated Redis with balance for user ID: " + userId);
        }

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
        String sql = "SELECT * FROM users JOIN validators ON users.id=validators.id WHERE role = 'VALIDATOR'";  // Adjust query based on your database schema

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
                        rs.getString("private_key"),
                        rs.getString("ip_address"),
                        rs.getInt("port")
                );
                String publicKey = rs.getString("public_key");
                System.out.println("Raw public_key from DB for user ID " + rs.getInt("id") + ": " + publicKey);

                if (publicKey == null || publicKey.isEmpty()) {
                    System.out.println("public_key is null or empty for user ID: " + rs.getInt("id"));
                }
                System.out.println("validator dans bd");
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
        String sql = "SELECT * FROM users WHERE role = 'CLIENT'";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int userId = rs.getInt("id");
                String username = rs.getString("username");
                String role = rs.getString("role");
                String password = rs.getString("password");
                String publicKey = rs.getString("public_key");
                String privateKey = rs.getString("private_key");

                // Fetch balance from Redis if available, otherwise fallback to DB
                Double balance = RedisUtil.getUserBalance(userId);
                if (balance == null) {
                    balance = rs.getDouble("balance");
                    RedisUtil.setUserBalance(userId, balance); // Cache the balance for future queries
                }

                // Create and add the Client object
                Client client = new Client(userId, username, password, balance, publicKey, privateKey);
                clientList.add(client);
            }

        } catch (SQLException e) {
            System.err.println("SQLException: " + e.getMessage());
            throw new RuntimeException("Failed to load all clients", e);
        }

        return clientList;
    }


    public boolean updateUserBalance(User user, double newBalance) {
        String sql = "UPDATE users SET balance = ? WHERE username = ? AND role = 'CLIENT'";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDouble(1, newBalance);
            stmt.setString(2, user.getUsername());

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                // Update Redis cache
                RedisUtil.setUserBalance(user.getId(), newBalance);
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

    public void updateAdminBalance(Admin admin, double newBalance) {
        String sql = "UPDATE users SET balance = ? WHERE id = ? AND role = 'ADMIN'";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDouble(1, newBalance);
            stmt.setInt(2, admin.getId());

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                // Update Redis cache
                RedisUtil.setUserBalance(admin.getId(), newBalance);
                System.out.println("Le solde du admin " + admin.getUsername() + " a été mis à jour.");
            } else {
                System.out.println("Aucun admin trouvé avec l id : " + admin.getId());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Échec de la mise à jour du solde pour admin : " + admin.getUsername(), e);
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
    public Admin getAdminFromDatabase(int adminId) {

        // Requête SQL pour récupérer les informations de l'administrateur
        String sql = "SELECT id, username, role, created_at, password, balance, public_key, private_key, is_connected, last_connection " +
                "FROM users WHERE id = ? AND role = 'ADMIN'";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            // Remplacer ? par l'ID de l'administrateur
            statement.setInt(1, adminId);

            // Exécuter la requête et récupérer les résultats
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                // Récupérer les données de l'administrateur depuis le ResultSet
                int id = resultSet.getInt("id");
                String username = resultSet.getString("username");
                String password = resultSet.getString("password");
                double balance = resultSet.getDouble("balance");
                String publicKey = resultSet.getString("public_key");
                String privateKey = resultSet.getString("private_key");

                // Créer et retourner un objet Admin
                Admin admin = new Admin(id, username, password, balance, publicKey, privateKey);
                return admin;
            } else {
                // Si l'administrateur n'est pas trouvé
                System.out.println("Aucun administrateur trouvé avec l'ID " + adminId);
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    public Validator getValidatorFromDatabase(int validatorId) {

        // Requête SQL pour récupérer les informations de l'administrateur
        String sql = "SELECT u.id, u.username, u.role, u.created_at, u.password, u.balance, u.public_key, " +
                "u.private_key, u.is_connected, u.last_connection, v.ip_address, v.port " +
                "FROM users u JOIN validators v ON u.id = v.id WHERE u.id = ? AND u.role = 'VALIDATOR'";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            // Remplacer ? par l'ID de l'administrateur
            statement.setInt(1, validatorId);

            // Exécuter la requête et récupérer les résultats
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                // Récupérer les données de l'administrateur depuis le ResultSet
                int id = resultSet.getInt("id");
                String username = resultSet.getString("username");
                String password = resultSet.getString("password");
                double balance = resultSet.getDouble("balance");
                String publicKey = resultSet.getString("public_key");
                String privateKey = resultSet.getString("private_key");
                String ipaddress = resultSet.getString("ip_address");
                int port =resultSet.getInt("port");
                // Créer et retourner un objet Admin
                Validator validator = new Validator(id, username, password, balance, publicKey, privateKey , ipaddress , port);
                return validator;
            } else {
                // Si l'administrateur n'est pas trouvé
                System.out.println("Aucun administrateur trouvé avec l'ID " + validatorId);
                return null;
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
    public Client getClientFromDatabase(int clientId) {
        // Check Redis cache first
        Double cachedBalance = RedisUtil.getUserBalance(clientId);
        if (cachedBalance != null) {
            System.out.println("Retrieved balance from Redis for client ID: " + clientId);
        }

        // SQL query to get client info
        String sql = """
        SELECT id, username, password, balance, public_key, private_key
        FROM users 
        WHERE id = ? AND role = 'CLIENT'
    """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, clientId);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                int id = resultSet.getInt("id");
                String username = resultSet.getString("username");
                String password = resultSet.getString("password");
                double balance = cachedBalance != null ? cachedBalance : resultSet.getDouble("balance");
                String publicKey = resultSet.getString("public_key");
                String privateKey = resultSet.getString("private_key");

                // Update Redis if the balance was retrieved from the database
                if (cachedBalance == null) {
                    RedisUtil.setUserBalance(id, balance);
                    System.out.println("Updated balance in Redis for public key: " + publicKey);
                }

                // Create and return the Client object
                return new Client(id, username, password, balance, publicKey, privateKey);
            } else {
                System.out.println("No client found with ID: " + clientId);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve client data for ID: " + clientId, e);
        }

        return null; // Return null if no client was found
    }

    public String getPublicKeyByUserId(int userId) throws SQLException {
        String publicKey = null;
        String sql = "SELECT public_key FROM users WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    publicKey = rs.getString("public_key");
                }
            }
        }
        if (publicKey == null) {
            throw new IllegalArgumentException("No public key found for user with ID: " + userId);
        }
        return publicKey;
    }

    public void markValidatorNeedsUpdate(int validatorId, long requiredVersion) {
        String sql = "UPDATE validators SET pending_update_version = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, requiredVersion);
            stmt.setInt(2, validatorId);
            stmt.executeUpdate();
            System.out.println("[Database] ✅ Marked validator " + validatorId + 
                             " for update to version " + requiredVersion);
        } catch (SQLException e) {
            System.err.println("[Database] ❌ Error marking validator for update: " + e.getMessage());
            throw new RuntimeException("Failed to mark validator for update", e);
        }
    }

    public Long getPendingUpdateVersion(int validatorId) {
        String sql = "SELECT pending_update_version FROM validators WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, validatorId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Long version = rs.getLong("pending_update_version");
                if (rs.wasNull()) {
                    return null;
                }
                return version;
            }
            return null;
        } catch (SQLException e) {
            System.err.println("[Database] ❌ Error getting pending update version: " + e.getMessage());
            throw new RuntimeException("Failed to get pending update version", e);
        }
    }

    public void clearPendingUpdate(int validatorId) {
        String sql = "UPDATE validators SET pending_update_version = NULL WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, validatorId);
            stmt.executeUpdate();
            System.out.println("[Database] ✅ Cleared pending update for validator " + validatorId);
        } catch (SQLException e) {
            System.err.println("[Database] ❌ Error clearing pending update: " + e.getMessage());
            throw new RuntimeException("Failed to clear pending update", e);
        }
    }

    public List<Validator> getAllValidators() {
        List<Validator> validators = new ArrayList<>();
        String sql = "SELECT * FROM validators";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Validator validator = new Validator(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getDouble("balance")
                );
                validator.setActive(rs.getBoolean("is_active"));
                validators.add(validator);
            }
            return validators;
        } catch (SQLException | NoSuchAlgorithmException e) {
            System.err.println("[Database] ❌ Error getting all validators: " + e.getMessage());
            throw new RuntimeException("Failed to get all validators", e);
        }
    }
}