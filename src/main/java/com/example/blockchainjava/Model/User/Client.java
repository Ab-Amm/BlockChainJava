package com.example.blockchainjava.Model.User;

import com.example.blockchainjava.Model.Transaction.Transaction;

import java.util.ArrayList;
import java.util.List;

public class Client extends User {
    private double balance;
    private List<Transaction> transactions;

    public Client(String username, String password, String email) {
        super(username, password, email, UserRole.CLIENT);
        this.balance = 0.0;
        this.transactions = new ArrayList<>();
    }

    public void updateBalance(double amount) {
        this.balance += amount;
    }

    public double getBalance() {
        return balance;
    }


    public void setBalance(double balance) {
        this.balance = balance;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }
}