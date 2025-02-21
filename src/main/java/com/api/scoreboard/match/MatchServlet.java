package com.api.scoreboard.match;

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

@WebServlet("/api/matches")
public class MatchServlet extends HttpServlet {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ServletContext context = getServletContext();
        String matchId = request.getParameter("id");

        List<Map<String, String>> matches = (List<Map<String, String>>) context.getAttribute("matches");
        if (matches == null) {
            matches = new ArrayList<>();
            context.setAttribute("matches", matches);
        }
        if (matchId != null) {
            Map<String, String> match = matches.stream().filter(m -> m.get("id").equals(matchId)).findFirst().orElse(null);
            response.setContentType("application/json");
            if (match == null) {
                response.setStatus(404);
                Map<String, String> error = new HashMap<>();
                error.put("error", "Match not found");
                response.getWriter().write(objectMapper.writeValueAsString(error));
                return;
            }
            response.getWriter().write(objectMapper.writeValueAsString(match));
        } else {
            response.setContentType("application/json");
            response.getWriter().write(objectMapper.writeValueAsString(matches));
        }
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

        Map<String,String> matchScores = new HashMap<>();
        matchScores.put("team1", "0");
        matchScores.put("team2", "0");
        context.setAttribute("match_" + newMatchId, matchScores);

        response.getWriter().write("success");
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String matchId = request.getParameter("id");
        ServletContext context = getServletContext();
        List<Map<String, String>> matches = (List<Map<String, String>>) context.getAttribute("matches");
        if (matches == null) {
            matches = new ArrayList<>();
            context.setAttribute("matches", matches);
        }

        matches.removeIf(match -> match.get("id").equals(matchId));
        context.setAttribute("matches", matches);
        context.removeAttribute("match_" + matchId);

        response.getWriter().write("success");
    }
}
