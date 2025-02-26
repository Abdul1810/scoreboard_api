package com.api.Listener;

import com.api.util.Database;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class AppListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("Application context initialized.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("Application context is being destroyed.");
        Database.closeConnection();
    }
}