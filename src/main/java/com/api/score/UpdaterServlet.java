package com.api.score;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/update-score")
public class UpdaterServlet extends HttpServlet {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ServletContext context = getServletContext();
        Map<String, Integer> scores = (Map<String, Integer>) context.getAttribute("scores");
        if (scores == null) {
            scores = new HashMap<>();
            scores.put("team1", 0);
            scores.put("team2", 0);
            context.setAttribute("scores", scores);
        }

        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(scores));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Integer> scores = objectMapper.readValue(request.getReader(), HashMap.class);
        ServletContext context = getServletContext();
        context.setAttribute("scores", scores);
        response.getWriter().write("success");
    }
}
