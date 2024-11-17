package com.example.blockchainjava.Model.User;

import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Util.Security.AuthenticationUtil;
import com.example.blockchainjava.Util.Security.HashUtil;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Validator extends User {
    private String validatorAddress;
    private String publicKey;
    private String privateKey;
    private boolean isActive;

    public Validator(String username, String password, String email) throws NoSuchAlgorithmException {
        super(username, password, email, UserRole.VALIDATOR);
        generateKeyPair();
        this.validatorAddress = HashUtil.generateAddress(publicKey);
        this.isActive = true;
    }



    private void generateKeyPair() throws NoSuchAlgorithmException {
        // Generate RSA key pair for signing
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();
        this.privateKey = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
        this.publicKey = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
    }

    public String sign(Transaction transaction) {
        // Sign transaction with private key
        String transactionData = transaction.toString();
        return AuthenticationUtil.sign(transactionData, privateKey);
    }
}