package com.api.scoreboard;

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
public class ScoreServlet extends HttpServlet {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String matchId = request.getParameter("id");
        Map<String, String> score = objectMapper.readValue(request.getReader(), HashMap.class);
        ServletContext context = getServletContext();

        Map<String, String> matchScores = (Map<String, String>) context.getAttribute("match_" + matchId);
        if (matchScores == null) {
            response.setStatus(404);
            response.getWriter().write("Match not found");
        } else {
            try {
                Integer.parseInt(score.get("team1"));
                Integer.parseInt(score.get("team2"));
            } catch (NumberFormatException e) {
                response.setStatus(400);
                response.getWriter().write("Invalid score");
                return;
            }
            if (Integer.parseInt(score.get("team1")) < 0 || Integer.parseInt(score.get("team2")) < 0) {
                response.setStatus(400);
                response.getWriter().write("Invalid score");
                return;
            }
            matchScores.put("team1", score.get("team1"));
            matchScores.put("team2", score.get("team2"));
            context.setAttribute("match_" + matchId, matchScores);
            response.getWriter().write("success");
        }
    }
}
