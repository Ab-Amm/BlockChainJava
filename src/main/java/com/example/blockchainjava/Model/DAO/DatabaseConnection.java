package com.example.blockchainjava.Model.DAO;

import com.example.blockchainjava.Util.Security.HashUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://25.51.250.189:3306/blockchain";
    private static final String USER = "root";
    private static final String PASSWORD = "2004";

    private static Connection connection;

    public static Connection getConnection() {
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("connected successfully to database blockchain");
            } catch (SQLException e) {
                throw new RuntimeException("Failed to connect to database", e);
            }
        }
        return connection;
    }
}
