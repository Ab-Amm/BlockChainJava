package com.example.blockchainjava.Model.User;

import com.example.blockchainjava.Model.Block.BlockChain;
import com.example.blockchainjava.Model.DAO.UserDAO;
import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Util.Security.AuthenticationUtil;
import com.example.blockchainjava.Util.Security.EncryptionUtil;
import com.example.blockchainjava.Util.Security.HashUtil;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;

public class Validator extends User {
    //private String validatorAddress; // Unique address derived from the public key
    private String publicKey;
    private String privateKey;
    private boolean isActive; // Validator status (active/inactive)
    private String ipAddress; // IP Address of the validator
    private int port;         // Port number of the validator
    //private double balance;   // Validator's balance in the system
    private BlockChain blockchain;

    // Constructor with IP Address and Port
    public Validator(int id , String username , double balance) throws NoSuchAlgorithmException {
        super(id, username ,balance ,UserRole.VALIDATOR);  // Call the parent constructor (User)

        this.isActive = true; // Validator is active by default
        generateKeyPair(); // Generate key pair for the validator
        //this.validatorAddress = HashUtil.generateAddress(publicKey); // Generate address from public key
        loadValidatorData(id); // Load additional data (ipAddress, port, balance)
    }
    public Validator(int id , String username ,String Password , double balance) throws NoSuchAlgorithmException {
        super(id, username ,Password ,balance , UserRole.VALIDATOR);  // Call the parent constructor (User)

        this.isActive = true; // Validator is active by default
       // generateKeyPair(); // Generate key pair for the validator
        //this.validatorAddress = HashUtil.generateAddress(publicKey); // Generate address from public key
        loadValidatorData(id); // Load additional data (ipAddress, port, balance)
    }

    public void loadValidatorData(int id) {
        // Create a UserDAO object to access the database
        UserDAO userDAO = new UserDAO();
        Validator data = userDAO.getValidatorData(id);

        if (data != null) {
            this.ipAddress = data.getIpAddress();
            this.port = data.getPort();
            //this.balance = data.getBalance();
        } else {
            System.err.println("No data found for the validator with username: " + username);
        }
    }

    public Validator(String ipAddress, int port) throws NoSuchAlgorithmException {
        super(" ", " ", UserRole.VALIDATOR); // Appel au constructeur de la classe parente User
        this.ipAddress = ipAddress;
        this.port = port;
       // this.balance = 0.0; // Solde initial à 0
        this.isActive = true; // Le validateur est actif par défaut
        generateKeyPair(); // Générer une paire de clés pour le validateur
        //this.validatorAddress = HashUtil.generateAddress(publicKey); // Générer une adresse à partir de la clé publique
    }

    public Validator(String username, String password) throws NoSuchAlgorithmException {
        super(username, password, UserRole.VALIDATOR);
      //  this.balance = 0.0; // Default balance is 0
        this.isActive = true; // Validators are active by default
        generateKeyPair();
        //this.validatorAddress = HashUtil.generateAddress(publicKey);
    }
    public Validator(String username, String password , double balance) throws NoSuchAlgorithmException {
        super(username, password,balance, UserRole.VALIDATOR);
       // this.balance = balance; // Default balance is 0
        this.isActive = true; // Validators are active by default
        generateKeyPair();
        //this.validatorAddress = HashUtil.generateAddress(publicKey);
    }
    public Validator(int id,String username, String password , double balance , String publicKey , String privateKey , String ipAddress , int port) throws NoSuchAlgorithmException {
        super(id , username, password,balance, UserRole.VALIDATOR ,publicKey ,privateKey);
        // this.balance = balance; // Default balance is 0
        this.isActive = true; // Validators are active by default
        //this.validatorAddress = HashUtil.generateAddress(publicKey);
        this.isActive = true;
        this.ipAddress = ipAddress;
        this.port = port;
        this.publicKey=publicKey;
        this.privateKey=privateKey;
    }
    public Validator(String username, String password, String ipAddress, int port) throws NoSuchAlgorithmException {
        this(username, password);
        //this.validatorAddress = HashUtil.generateAddress(publicKey);// Appel au constructeur principal
        this.isActive = true;
        this.ipAddress = ipAddress;
        this.port = port;
    }
    public Validator(int id , String username, String ipAddress, int port , double balance ) throws NoSuchAlgorithmException {
        super(id ,username , balance , UserRole.VALIDATOR); // Appel au constructeur principal
        this.ipAddress = ipAddress;
        this.port = port;
    }


    public Validator(String username, String password, String ipAddress, int port , double balance) throws NoSuchAlgorithmException {
        this(username, password , balance ); // Appel au constructeur principal
        this.ipAddress = ipAddress;
        this.port = port;
        //this.balance=balance;
    }

    // Generate RSA Key Pair
    private void generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();
        this.privateKey = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
        this.publicKey = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
    }

    public String sign(Transaction transaction ,Validator validator) throws Exception {
        String dataToSign = Transaction.generateDataToSign(
                transaction.getSenderId(), transaction.getReceiverKey(), transaction.getAmount()
        );
        System.out.println("le validator va signer par ce private key");
        System.out.println(validator.getPrivateKey());
        return AuthenticationUtil.sign(dataToSign, EncryptionUtil.decrypt(privateKey));
    }
    // Getter for balance
//    public double getBalance() {
//        return balance;
//    }
//
//    // Setter for balance
//    public void setBalance(double balance) {
//        this.balance = balance;
//    }

    // Getter for validatorAddress
//    public String getValidatorAddress() {
//        return validatorAddress;
//    }

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

    public BlockChain getBlockchain() {
        return blockchain;
    }

    public void setBlockchain(BlockChain blockchain) {
        this.blockchain = blockchain;
    }

    @Override
    public String toString() {
        return "Validator{" +
                "id=" + getId() +
                ", username='" + getUsername() + '\'' +
//                ", validatorAddress='" + validatorAddress + '\'' +
                ", isActive=" + isActive +
                ", ipAddress='" + ipAddress + '\'' +
                ", port=" + port +
                ", balance=" + getBalance() +
                ", public key=" + getPublicKey() +
                ", private key=" + getPrivateKey() +
//                ", createdAt=" + getCreatedAt() +
                '}';
    }
}