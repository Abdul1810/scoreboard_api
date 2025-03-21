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

@WebServlet("/api/auth/login")
public class LoginServlet extends HttpServlet {
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
                String salt = rs.getString("salt");
                String hashedPassword = rs.getString("password");
                if (PBKDF2Encryption.verifyPassword(password, salt, hashedPassword)) {
                    request.getSession(true);
                    request.getSession().setAttribute("uid", rs.getInt("id"));
                    request.getSession().setAttribute("username", rs.getString("username"));
                    String csrfToken = UUID.randomUUID().toString();
                    request.getSession().setAttribute("agent", request.getHeader("User-Agent"));
                    request.getSession().setAttribute("csrfToken", csrfToken);
                    jsonResponse.put("csrfToken", csrfToken);
                    jsonResponse.put("username", rs.getString("username"));
                } else {
                    jsonResponse.put("error", "Invalid username or password");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
            } else {
                jsonResponse.put("error", "user not found");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | SQLException e) {
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

        response.setContentType("application/json");
        response
            .getWriter()
            .write(objectMapper.writeValueAsString(jsonResponse));
    }
}
