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
        String sql = "INSERT INTO users (id, username, password, role, created_at) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getId());
            stmt.setString(2, user.getUsername());
            stmt.setString(3, user.getPassword());
            stmt.setString(5, user.getRole().toString());
            stmt.setTimestamp(6, Timestamp.valueOf(user.getCreatedAt()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user: " + user.getUsername(), e);
        }
    }

    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                UserRole role = UserRole.valueOf(rs.getString("role"));
                return createUserFromResultSet(rs, role);
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
        return switch (role) {
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
    }

    public List<Validator> getValidators() {
        List<Validator> validatorList = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role = 'VALIDATOR'";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Validator validator = new Validator(
                        rs.getString("username"),
                        rs.getString("password")
                );
                validatorList.add(validator);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load validators", e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return validatorList;
    }
}
