package com.example.blockchainjava.Util.Security;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class SecurityUtils {
    public static PrivateKey decodePrivateKey(String base64PrivateKey) throws Exception {
        byte[] decodedKey = Base64.getDecoder().decode(base64PrivateKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decodedKey));
    }

    // Convertir la clé publique en objet PublicKey depuis une chaîne Base64
    public static PublicKey decodePublicKey(String base64PublicKey) throws Exception {
        byte[] decodedKey = Base64.getDecoder().decode(base64PublicKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));
    }
    // Méthode pour signer des données avec une clé privée
    public static String signData(String dataToSign, PrivateKey privateKey) {
        try {
            // Créer une instance de l'algorithme de signature (ici, RSA avec SHA-256)
            Signature signature = Signature.getInstance("SHA256withRSA");

            // Initialiser la signature avec la clé privée
            signature.initSign(privateKey);

            // Signer les données
            signature.update(dataToSign.getBytes());

            // Obtenir la signature
            byte[] signedData = signature.sign();

            // Retourner la signature encodée en Base64 (pratique pour le stockage ou l'envoi)
            return Base64.getEncoder().encodeToString(signedData);

        } catch (Exception e) {
            throw new RuntimeException("Error signing data", e);
        }
    }

    // Méthode pour vérifier la signature avec la clé publique
    public static boolean verifySignature(String data, String signatureStr, PublicKey publicKey) {
        try {
            // Créer une instance de l'algorithme de signature (ici, RSA avec SHA-256)
            Signature signature = Signature.getInstance("SHA256withRSA");

            // Initialiser la signature avec la clé publique
            signature.initVerify(publicKey);

            // Mettre à jour les données à vérifier
            signature.update(data.getBytes());

            // Décoder la signature de Base64
            byte[] decodedSignature = Base64.getDecoder().decode(signatureStr);

            // Vérifier la signature
            return signature.verify(decodedSignature);

        } catch (Exception e) {
            throw new RuntimeException("Error verifying signature", e);
        }
    }

}

