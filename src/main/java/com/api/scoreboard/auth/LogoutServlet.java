package com.api.scoreboard.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.*;

@WebServlet("/api/auth/logout")
public class LogoutServlet extends HttpServlet {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, String> jsonResponse = new HashMap<>();

        HttpSession session = request.getSession(false);
        if (session == null) {
            jsonResponse.put("error", "Not logged in");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response
                    .getWriter()
                    .write(objectMapper.writeValueAsString(jsonResponse));
            return;
        }

        session.invalidate();
        jsonResponse.put("message", "Logged out successfully");
        response.setContentType("application/json");
        response
                .getWriter()
                .write(objectMapper.writeValueAsString(jsonResponse));
    }
}
