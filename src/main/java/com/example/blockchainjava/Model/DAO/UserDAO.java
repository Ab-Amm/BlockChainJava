package com.example.blockchainjava.Model.DAO;

import com.example.blockchainjava.Model.User.*;

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
        String sql = "INSERT INTO users (id, username, password, email, role, created_at) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getId());
            stmt.setString(2, user.getUsername());
            stmt.setString(3, user.getPassword());
            stmt.setString(4, user.getEmail());
            stmt.setString(5, user.getRole().toString());
            stmt.setTimestamp(6, Timestamp.valueOf(user.getCreatedAt()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user", e);
        }
    }

    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                UserRole role = UserRole.valueOf(rs.getString("role"));
                switch (role) {
                    case CLIENT:
                        return new Client(
                                rs.getString("username"),
                                rs.getString("password"),
                                rs.getString("email")
                        );
                    case VALIDATOR:
                        return new Validator(
                                rs.getString("username"),
                                rs.getString("password"),
                                rs.getString("email")
                        );
                    case ADMIN:
                        return new Admin(
                                rs.getString("username"),
                                rs.getString("password"),
                                rs.getString("email")
                        );
                    default:
                        throw new IllegalStateException("Unknown user role: " + role);
                }
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to load user", e);
        }
        return null;
    }

    public Validator getValidators() {
        List<Validator> validatorList = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role = 'VALIDATOR'";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Validator validator = new Validator(
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("email")
                );
                validatorList.add(validator);
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to load validators", e);
        }
        return (Validator) validatorList;
    }
}
