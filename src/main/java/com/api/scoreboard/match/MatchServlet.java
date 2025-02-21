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
        for (Map<String, String> m : matches) {
            if (m.get("team1").equals(match.get("team1")) && m.get("team2").equals(match.get("team2"))) {
                response.setStatus(400);
                response.getWriter().write("Match already exists");
                return;
            }
        }
        matches.add(match);
        context.setAttribute("matches", matches);

        Map<String,Object> matchStats = new HashMap<>();
        /*
        team1 - number - default 0
        team2 - number - default 0
        team1_wickets - number - default 0 - max 10
        team2_wickets - number - default 0 - max 10
        team1_balls - number - default 0 - max 120
        team2_balls - number - default 0 - max 120
        current_batting - team1/team2 - default team1
        is_completed - true/false - default false
        winner - team1/team2/none/tie - default none
         */
        matchStats.put("team1", "0");
        matchStats.put("team2", "0");
        matchStats.put("team1_wickets", "0");
        matchStats.put("team2_wickets", "0");
        matchStats.put("team1_balls", "0");
        matchStats.put("team2_balls", "0");
        matchStats.put("current_batting", "team1");
        matchStats.put("is_completed", "false");
        matchStats.put("winner", "none");

        context.setAttribute("match_" + newMatchId, matchStats);
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
