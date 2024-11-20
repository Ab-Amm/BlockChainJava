package com.example.blockchainjava.Database;

import com.example.blockchainjava.Model.DAO.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DatabaseHelper {

    // Vérifier si l'ID public existe dans la base de données
    public static boolean isPublicKeyExists(String publicKey) {
        boolean exists = false;

        // Se connecter à la base de données (assurez-vous d'avoir votre gestion de connexion à la DB)
        try (Connection connection = DatabaseConnection.getConnection()) {
            String query = "SELECT COUNT(*) FROM users WHERE public_key = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, publicKey);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    exists = rs.getInt(1) > 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return exists;
    }
}
