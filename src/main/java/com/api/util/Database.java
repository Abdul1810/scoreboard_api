package com.api.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    private static Connection conn;
    private static final String DATABASE_URL = "jdbc:mysql://localhost:3306/scoreboard";
    private static final String DATABASE_USER = "root";
    private static final String DATABASE_PASSWORD = "";

    private static void initDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static Connection getConnection() {
        try {
            if (conn == null || conn.isClosed()) {
                initDatabase();
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return conn;
    }

    public static void closeConnection() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("MySQL connection closed.");
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
