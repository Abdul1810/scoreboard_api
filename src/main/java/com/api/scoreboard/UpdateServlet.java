package com.api.scoreboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/update-score")
public class UpdateServlet extends HttpServlet {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String matchId = request.getParameter("id");
        Map<String, String> score = objectMapper.readValue(request.getReader(), HashMap.class);
        ServletContext context = getServletContext();
        Map<String, Map<String, String>> scores = (Map<String, Map<String, String>>) context.getAttribute("scores");
        if (scores == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"error\": \"No matches found\"}");
            return;
        }

        if (!scores.containsKey(matchId)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"error\": \"Match not found\"}");
            return;
        }

        scores.put(matchId, score);
        context.setAttribute("scores", scores);
        response.getWriter().write("success");
    }


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, String> match = objectMapper.readValue(request.getReader(), HashMap.class);
        ServletContext context = getServletContext();

        int lastMatchId = context.getAttribute("lastMatchId") == null ? 0 : (int) context.getAttribute("lastMatchId");
        int newMatchId = lastMatchId + 1;
        match.put("id", String.valueOf(newMatchId));
        context.setAttribute("lastMatchId", newMatchId);

        List<Map<String, String>> matches = (List<Map<String, String>>) context.getAttribute("matches");
        if (matches == null) {
            matches = new ArrayList<>();
            context.setAttribute("matches", matches);
        }
        matches.add(match);
        context.setAttribute("matches", matches);

        Map<String, Map<String, String>> scores = (Map<String, Map<String, String>>) context.getAttribute("scores");
        if (scores == null) {
            scores = new HashMap<>();
        }
        Map<String, String> matchScores = new HashMap<>();
        matchScores.put("team1", "0");
        matchScores.put("team2", "0");
        scores.put(String.valueOf(newMatchId), matchScores);
        context.setAttribute("scores", scores);

        response.getWriter().write("success");
    }
}
