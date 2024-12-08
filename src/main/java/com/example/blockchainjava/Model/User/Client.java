package com.example.blockchainjava.Model.User;

import com.example.blockchainjava.Model.DAO.UserDAO;
import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Util.Security.AuthenticationUtil;
import com.example.blockchainjava.Util.Security.EncryptionUtil;
import com.example.blockchainjava.Util.Security.SecurityUtils;
import javafx.beans.property.*;

import java.util.ArrayList;
import java.util.List;

public class Client extends User {
    private double balance;
    private List<Transaction> transactions;
    private String publicKey;
    private String privateKey ;
    private int id;
    public Client(int Id, String username, String password , Double balance , String pubkickey , String privatekey) {
        super(Id , username, password, balance , UserRole.CLIENT , pubkickey , privatekey);
        this.id=Id;
        this.balance=balance;
        this.privateKey=privatekey;
        this.publicKey=pubkickey;
    }
    public Client(String username, String password) {
        super(username, password, UserRole.CLIENT);
        this.balance = 0.0;
        this.transactions = new ArrayList<>();
        this.privateKey=super.getPrivateKey();
        this.publicKey=super.getPublicKey();
    }
    public String sign(Transaction transaction ,Client client) throws Exception {
        String dataToSign = Transaction.generateDataToSign(
                transaction.getSenderId(), transaction.getReceiverKey(), transaction.getAmount()
        );
        System.out.println("le client  va signer par ce private key");
        System.out.println(client.getPrivateKey());
        // Signer les données avec la clé privée
        return AuthenticationUtil.sign(dataToSign, EncryptionUtil.decrypt(privateKey));
    }

    @Override
    public String getPrivateKey() {
        return privateKey;
    }

    @Override
    public String getPublicKey() {
        return publicKey;
    }

    public Client(int id , String username, String password , Double balance) {
        super(username, password, UserRole.CLIENT);
        this.id=id;
        this.balance = balance;
        this.transactions = new ArrayList<>();
    }
    public Client(int id ,String username,Double balance) {
        super(id , username, balance, UserRole.CLIENT);
        this.id=id;
        this.balance = balance;
        this.transactions = new ArrayList<>();
    }

    public boolean hasSufficientBalance(Transaction transaction) {
        return this.getBalance() >= transaction.getAmount();
    }

    public IntegerProperty getIdProperty() {
        return new SimpleIntegerProperty(id);  // Retourne une IntegerProperty
    }
    // Renvoi la propriété StringProperty de 'username' (hérité de User)
    public StringProperty getNameProperty() {
        return new SimpleStringProperty(username);  // Utilisation de 'username' hérité
    }

    // Renvoi la propriété DoubleProperty de 'balance'
    public DoubleProperty getBalanceProperty() {
        return new SimpleDoubleProperty(balance);
    }

    // Mise à jour du solde
    public void updateBalance(double amount) {
        this.balance += amount;
    }

    // Getter pour 'balance'
    public double getBalance() {
        return balance;
    }

    // Setter pour 'balance'
    public void setBalance(double balance) {
        this.balance = balance;
    }

    // Getter pour les transactions
    public List<Transaction> getTransactions() {
        return transactions;
    }

    // Setter pour les transactions
    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public void loadClientData(int id) {
        // Create a UserDAO object to access the database
        UserDAO userDAO = new UserDAO();
        Client data = userDAO.getClientData(id);

        if (data != null) {
            this.id=data.getId();
            this.balance = data.getBalance();
        } else {
            System.err.println("No data found for the validator with username: " + username);
        }
    }
}
