package com.example.blockchainjava.Util.Security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class EncryptionUtil {

    private static final String ALGORITHM = "AES";
    private static final String SECRET_KEY_ENV_VAR = "MY_SECRET_KEY"; // Environment variable for the secret key

    public static String encrypt(String data) throws Exception {
        SecretKey key = getKey();
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedData = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedData);
    }

    public static String decrypt(String encryptedData) throws Exception {
        SecretKey key = getKey();
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decodedData = Base64.getDecoder().decode(encryptedData);
        return new String(cipher.doFinal(decodedData));
    }

    private static SecretKey getKey() {
        // Retrieve the secret key from the environment variable
        String secretKeyString = System.getenv(SECRET_KEY_ENV_VAR);

        if (secretKeyString == null || secretKeyString.length() < 16) {
            throw new IllegalArgumentException("Invalid or missing secret key in environment variable.");
        }

        // Ensure the key is of appropriate length for AES (e.g., 128-bit, 16 bytes)
        return new SecretKeySpec(secretKeyString.getBytes(), ALGORITHM);
    }
}
