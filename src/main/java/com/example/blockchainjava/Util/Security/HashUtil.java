package com.example.blockchainjava.Util.Security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.mindrot.jbcrypt.BCrypt;

public class HashUtil {

    /**
     * Generate a SHA-256 hash from the given input string.
     *
     * @param input The input string to hash.
     * @return The SHA-256 hash as a hexadecimal string.
     */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    // Hachage du mot de passe avec BCrypt
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    // Vérification du mot de passe par rapport au hachage stocké
    public static boolean verifyPassword(String password, String hashedPassword) {
        if (password == null || password.isEmpty()) {
            System.out.println("Password entered is null or empty.");
            return false;
        }

        if (hashedPassword == null || hashedPassword.isEmpty()) {
            System.out.println("Hashed password from database is null or empty.");
            return false;
        }

        // Log les valeurs des entrées pour débogage
        System.out.println("Password entered: " + password);
        System.out.println("Stored hashed password: " + hashedPassword);

        // Vérification du mot de passe
        try {
            boolean isVerified = BCrypt.checkpw(password, hashedPassword);
            if (isVerified) {
                System.out.println("Password verified successfully.");
            } else {
                System.out.println("Password verification failed.");
            }
            return isVerified;
        } catch (IllegalArgumentException e) {
            System.err.println("Error during password verification: " + e.getMessage());
            return false;
        }
    }


    // Génération d'une adresse à partir de la clé publique
    public static String generateAddress(String publicKey) {
        return sha256(publicKey).substring(0, 40);
    }

}
