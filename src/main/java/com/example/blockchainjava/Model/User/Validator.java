package com.example.blockchainjava.Model.User;

import com.example.blockchainjava.Model.DAO.UserDAO;
import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Util.Security.AuthenticationUtil;
import com.example.blockchainjava.Util.Security.HashUtil;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;

public class Validator extends User {
    private String validatorAddress; // Unique address derived from the public key
    private String publicKey;
    private String privateKey;
    private boolean isActive; // Validator status (active/inactive)
    private String ipAddress; // IP Address of the validator
    private int port;         // Port number of the validator
    private double balance;   // Validator's balance in the system

    // Constructor with IP Address and Port
    public Validator(String username) throws NoSuchAlgorithmException {
        super(username, "default_password", UserRole.VALIDATOR);  // Call the parent constructor (User)
        this.balance = 0.0; // Initial balance set to 0
        this.isActive = true; // Validator is active by default
        generateKeyPair(); // Generate key pair for the validator
        this.validatorAddress = HashUtil.generateAddress(publicKey); // Generate address from public key
        loadValidatorData(username); // Load additional data (ipAddress, port, balance)
    }

    private void loadValidatorData(String username) {
        // Create a UserDAO object to access the database
        UserDAO userDAO = new UserDAO();
        Validator data = userDAO.getValidatorData(username);

        if (data != null) {
            this.ipAddress = data.getIpAddress();
            this.port = data.getPort();
            this.balance = data.getBalance();
        } else {
            System.err.println("No data found for the validator with username: " + username);
        }
    }



    public Validator(String ipAddress, int port) throws NoSuchAlgorithmException {
        super(" ", " ", UserRole.VALIDATOR); // Appel au constructeur de la classe parente User
        this.ipAddress = ipAddress;
        this.port = port;
        this.balance = 0.0; // Solde initial à 0
        this.isActive = true; // Le validateur est actif par défaut
        generateKeyPair(); // Générer une paire de clés pour le validateur
        this.validatorAddress = HashUtil.generateAddress(publicKey); // Générer une adresse à partir de la clé publique
    }

    public Validator(String username, String password) throws NoSuchAlgorithmException {
        super(username, password, UserRole.VALIDATOR);
        this.balance = 0.0; // Default balance is 0
        this.isActive = true; // Validators are active by default
        generateKeyPair();
        this.validatorAddress = HashUtil.generateAddress(publicKey);
    }
    public Validator(String username, String password , double balance) throws NoSuchAlgorithmException {
        super(username, password, UserRole.VALIDATOR);
        this.balance = balance; // Default balance is 0
        this.isActive = true; // Validators are active by default
        generateKeyPair();
        this.validatorAddress = HashUtil.generateAddress(publicKey);
    }
    public Validator(String username, String password, String ipAddress, int port) throws NoSuchAlgorithmException {
        this(username, password); // Appel au constructeur principal
        this.ipAddress = ipAddress;
        this.port = port;
    }


    public Validator(String username, String password, String ipAddress, int port , double balance) throws NoSuchAlgorithmException {
        this(username, password); // Appel au constructeur principal
        this.ipAddress = ipAddress;
        this.port = port;
        this.balance=balance;
    }

    // Generate RSA Key Pair
    private void generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();
        this.privateKey = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
        this.publicKey = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
    }

    // Sign a transaction using the private key
    public String sign(Transaction transaction) {
        String transactionData = transaction.toString();
        return AuthenticationUtil.sign(transactionData, privateKey);
    }

    // Getter for balance
    public double getBalance() {
        return balance;
    }

    // Setter for balance
    public void setBalance(double balance) {
        this.balance = balance;
    }

    // Getter for validatorAddress
    public String getValidatorAddress() {
        return validatorAddress;
    }

    // Getter for publicKey
    public String getPublicKey() {
        return publicKey;
    }

    // Getter for privateKey
    public String getPrivateKey() {
        return privateKey;
    }

    // Getter for isActive
    public boolean isActive() {
        return isActive;
    }

    // Setter for isActive
    public void setActive(boolean active) {
        isActive = active;
    }

    // Getter for ipAddress
    public String getIpAddress() {
        return ipAddress;
    }

    // Setter for ipAddress
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    // Getter for port
    public int getPort() {
        return port;
    }

    // Setter for port
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "Validator{" +
                "id=" + getId() +
                "username='" + getUsername() + '\'' +
                ", validatorAddress='" + validatorAddress + '\'' +
                ", isActive=" + isActive +
                ", ipAddress='" + ipAddress + '\'' +
                ", port=" + port +
                ", balance=" + balance +
                ", createdAt=" + getCreatedAt() +
                '}';
    }



}
