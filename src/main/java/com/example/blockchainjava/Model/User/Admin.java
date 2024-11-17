package com.example.blockchainjava.Model.User;

import java.security.NoSuchAlgorithmException;

public class Admin extends User {
    public Admin(String username, String password, String email) {
        super(username, password, email, UserRole.ADMIN);
    }

    public Validator registerValidator(String username, String password, String email) throws NoSuchAlgorithmException {
        return new Validator(username, password, email);
    }
}