package com.api.scoreboard.auth;

import com.api.util.Database;
import com.api.util.PBKDF2Encryption;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import java.util.*;

@WebServlet("/api/auth/register")
public class RegisterServlet extends HttpServlet {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, String> jsonResponse = new HashMap<>();
        Map<String, String> requestData = objectMapper.readValue(request.getReader(), HashMap.class);
        String username = requestData.getOrDefault("username", "");
        String password = requestData.getOrDefault("password", "");

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = new Database().getConnection();
            stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ?");
            stmt.setString(1, username);
            rs = stmt.executeQuery();
            if (rs.next()) {
                jsonResponse.put("error", "Username already taken");
                response.setStatus(HttpServletResponse.SC_CONFLICT);
            } else {
                if (password.length() < 8) {
                    jsonResponse.put("error", "Password must be at least 8 characters long");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                } else {
                    String salt = PBKDF2Encryption.generateSalt();
                    String hashedPassword = PBKDF2Encryption.encryptPassword(password, salt);
                    stmt = conn.prepareStatement("INSERT INTO users (username, password, salt) VALUES (?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
                    stmt.setString(1, username);
                    stmt.setString(2, hashedPassword);
                    stmt.setString(3, salt);
                    stmt.executeUpdate();
                    rs = stmt.getGeneratedKeys();
                    rs.next();
                    int userId = rs.getInt(1);
                    request.getSession(true);
                    request.getSession().setAttribute("authenticated", true);
                    request.getSession().setAttribute("uid", userId);
                    request.getSession().setAttribute("username", username);
                    jsonResponse.put("message", "User registered successfully");
                    String csrfToken = UUID.randomUUID().toString();
                    request.getSession().setAttribute("agent", request.getHeader("User-Agent"));
                    request.getSession().setAttribute("csrfToken", csrfToken);
                    jsonResponse.put("csrfToken", csrfToken);
                    jsonResponse.put("username", username);
                }
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | SQLException e) {
            System.out.println("Error registering user: " + e.getMessage());
            jsonResponse.put("error", "Internal server error");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception e) {
                System.out.println("Error Closing resource: " + e.getMessage());
            }
        }

        response
                .getWriter()
                .write(objectMapper.writeValueAsString(jsonResponse));
    }
}
