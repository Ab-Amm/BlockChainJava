package com.example.blockchainjava.Model.User;


public class Session {
    private static User currentUser;

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }
    public static void clearSession() {
        currentUser = null;
    }
}
