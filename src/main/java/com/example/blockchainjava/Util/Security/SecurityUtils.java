package com.example.blockchainjava.Util.Security;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SecurityUtils {

    // Méthode pour décoder une clé privée à partir d'une chaîne Base64
    public static PrivateKey decodePrivateKey(String base64PrivateKey) throws Exception {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(base64PrivateKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decodedKey));
        } catch (Exception e) {
            System.err.println("Error decoding private key: " + e.getMessage());
            throw new RuntimeException("Error decoding private key", e);
        }
    }

    // Méthode pour décoder une clé publique à partir d'une chaîne Base64
    public static PublicKey decodePublicKey(String base64PublicKey) throws Exception {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(base64PublicKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));
        } catch (Exception e) {
            System.err.println("Error decoding public key: " + e.getMessage());
            throw new RuntimeException("Error decoding public key", e);
        }
    }

    // Méthode pour signer des données avec une clé privée
    public static String signData(String dataToSign, PrivateKey privateKey) {
        try {
            // Créer une instance de l'algorithme de signature (RSA avec SHA-256)
            Signature signature = Signature.getInstance("SHA256withRSA");

            // Initialiser la signature avec la clé privée
            signature.initSign(privateKey);

            // Signer les données (en utilisant UTF-8 pour l'encodage des données)
            signature.update(dataToSign.getBytes(StandardCharsets.UTF_8));

            // Obtenir la signature
            byte[] signedData = signature.sign();

            // Retourner la signature encodée en Base64 (pratique pour le stockage ou l'envoi)
            return Base64.getEncoder().encodeToString(signedData);

        } catch (Exception e) {
            System.err.println("Error signing data: " + e.getMessage());
            throw new RuntimeException("Error signing data", e);
        }
    }

    // Méthode pour vérifier la signature avec la clé publique
    public static boolean verifySignature(String data, String signatureStr, PublicKey publicKey) {
        try {
            // Créer une instance de l'algorithme de signature (RSA avec SHA-256)
            Signature signature = Signature.getInstance("SHA256withRSA");

            // Initialiser la signature avec la clé publique
            signature.initVerify(publicKey);

            // Mettre à jour les données à vérifier (en utilisant UTF-8 pour l'encodage des données)
            signature.update(data.getBytes(StandardCharsets.UTF_8));

            // Décoder la signature de Base64
            byte[] decodedSignature = Base64.getDecoder().decode(signatureStr);

            // Vérifier la signature
            return signature.verify(decodedSignature);

        } catch (Exception e) {
            System.err.println("Error verifying signature: " + e.getMessage());
            throw new RuntimeException("Error verifying signature", e);
        }
    }
}
