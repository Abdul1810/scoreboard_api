package com.api.listener;

import com.api.util.Database;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;

@WebListener
public class AppContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            Database.initDatabase();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            Database.closeConnection();
            System.out.println("Database connection closed.");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
