package com.example.blockchainjava.Model.User;

import com.example.blockchainjava.Util.Security.HashUtil;

import java.time.LocalDateTime;
import java.util.UUID;

public class User {
    private String id;
    private String username;
    private String password; // Stored hashed
    private UserRole role;
    private LocalDateTime createdAt;

    public User(String username, String password, UserRole role) {
        this.id = UUID.randomUUID().toString();
        this.username = username;
        this.password = password;
        this.role = role;
        this.createdAt = LocalDateTime.now();
    }



    public String getId() {
        return id;
    }

    public void setId(String id) {
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
}