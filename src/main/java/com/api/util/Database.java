package com.api.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private static Connection conn = null;
    private static final String DATABASE_URL = "jdbc:sqlite:C:\\Users\\ACER\\Downloads\\db.sqlite";

    public static void initDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(DATABASE_URL);
            System.out.println("SQLite connection established.");
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
                System.out.println("SQLite connection closed.");
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void runQuery(String query) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(query);
            System.out.println("Query executed successfully.");
        } catch (SQLException e) {
            System.out.println("Error executing query: " + e.getMessage());
        }

    }
}
