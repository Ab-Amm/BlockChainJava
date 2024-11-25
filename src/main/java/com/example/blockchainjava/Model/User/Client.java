package com.example.blockchainjava.Model.User;

import com.example.blockchainjava.Model.Transaction.Transaction;
import javafx.beans.property.*;

import java.util.ArrayList;
import java.util.List;

public class Client extends User {
    private double balance;
    private List<Transaction> transactions;
    private int id;

    public Client(String username, String password) {
        super(username, password, UserRole.CLIENT);
        this.balance = 0.0;
        this.transactions = new ArrayList<>();
    }
    public Client(int id ,String username, String password , Double balance) {
        super(username, password, UserRole.CLIENT);
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
}
