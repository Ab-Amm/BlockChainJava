package com.example.blockchainjava.Model.User;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

import static java.lang.Math.incrementExact;
import static java.lang.Math.random;

public class User {
    private int id;
    protected String username;
    private String password; // Stored hashed
    private UserRole role;
    private LocalDateTime createdAt;
    private String privateKey;
    private String publicKey;
    private double balance ;


    public User(int id , String username , double balance ,UserRole role){
        this.id=id;
        this.username=username;
        this.balance=balance;
        this.role = role;
    }
    public User(int id , String username , String password, double balance ,UserRole role){
        this.id=id;
        this.username=username;
        this.password=password;
        this.balance=balance;
        this.role = role;
    }
    public User(String username, String password, UserRole role) {
        this.id=incrementExact(getId());
        this.username = username;
        this.password = password;
        this.role = role;
        this.createdAt = LocalDateTime.now();
        generateKeys();
    }


    public void setBalance(double balance) {
        this.balance = balance;
    }

    public Integer getId() {
        return id;
    }

    public double getBalance() {
        return balance;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }



    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +

                ", role=" + role +
                ", createdAt=" + createdAt +
                '}';
    }

    private void generateKeys() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            this.privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
            this.publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }
}