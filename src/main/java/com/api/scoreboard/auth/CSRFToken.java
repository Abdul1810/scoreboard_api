package com.api.scoreboard.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/auth/verify")
public class CSRFToken extends HttpServlet {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, String> jsonResponse = new HashMap<>();
        System.out.println(request.getSession().getAttribute("authenticated"));
        HttpSession session = request.getSession(false);
        if (session.getAttribute("authenticated") != null && (boolean) session.getAttribute("authenticated")) {
            if (session.getAttribute("agent") != request.getHeader("User-Agent")) {
                jsonResponse.put("csrfToken", (String) session.getAttribute("csrfToken"));
            }
        } else {
            jsonResponse.put("error", "Unauthorized");
        }

        response.setContentType("application/json");
        response
                .getWriter()
                .write(objectMapper.writeValueAsString(jsonResponse));
    }
}
