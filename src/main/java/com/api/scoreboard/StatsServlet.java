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

import static com.api.util.Utils.validatePositiveIntegers;

@WebServlet("/update-stats")
public class StatsServlet extends HttpServlet {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String matchId = request.getParameter("id");
        Map<String, String> stats = objectMapper.readValue(request.getReader(), HashMap.class);
        ServletContext context = getServletContext();

        Map<String, String> matchStats = (Map<String, String>) context.getAttribute("match_" + matchId);
        if (matchStats == null) {
            response.setStatus(404);
            response.getWriter().write("Match not found");
        } else {
            if (matchStats.get("is_completed").equals("true")) {
                response.setStatus(400);
                response.getWriter().write("Match already completed");
            } else {
                if (matchStats.get("current_batting").equals("team1")) {
                    if (validatePositiveIntegers(stats.get("team1"), stats.get("team1_wickets"), stats.get("team1_balls"))) {
                        if (Integer.parseInt(stats.get("team1")) < Integer.parseInt(matchStats.get("team1")) ||
                                Integer.parseInt(stats.get("team1_wickets")) < Integer.parseInt(matchStats.get("team1_wickets")) ||
                                Integer.parseInt(stats.get("team1_balls")) < Integer.parseInt(matchStats.get("team1_balls"))) {
                            response.setStatus(400);
                            response.getWriter().write("Corrupted Data Team1");
                            return;
                        }
                        if (Integer.parseInt(stats.get("team1_balls")) > 120 || Integer.parseInt(stats.get("team1_wickets")) > 10) {
                            response.setStatus(400);
                            response.getWriter().write("Invalid Data Team1");
                            return;
                        }
                    } else {
                        response.setStatus(400);
                        response.getWriter().write("Invalid Data Team1");
                        return;
                    }
                    matchStats.put("team1", stats.get("team1"));
                    matchStats.put("team1_wickets", stats.get("team1_wickets"));
                    matchStats.put("team1_balls", stats.get("team1_balls"));
                    if (Integer.parseInt(matchStats.get("team1_balls")) == 120 || Integer.parseInt(matchStats.get("team1_wickets")) == 10) {
                        matchStats.put("current_batting", "team2");
                    }
                } else if (matchStats.get("current_batting").equals("team2")) {
                    if (validatePositiveIntegers(stats.get("team2"), stats.get("team2_wickets"), stats.get("team2_balls"))) {
                        if (Integer.parseInt(stats.get("team2")) < Integer.parseInt(matchStats.get("team2")) || Integer.parseInt(stats.get("team2_wickets")) < Integer.parseInt(matchStats.get("team2_wickets")) || Integer.parseInt(stats.get("team2_balls")) < Integer.parseInt(matchStats.get("team2_balls"))) {
                            response.setStatus(400);
                            response.getWriter().write("Corrupted Data Team2");
                            return;
                        }
                        if (Integer.parseInt(stats.get("team2_balls")) > 120 || Integer.parseInt(stats.get("team2_wickets")) > 10) {
                            response.setStatus(400);
                            response.getWriter().write("Invalid Data Team2");
                            return;
                        }
                    } else {
                        response.setStatus(400);
                        response.getWriter().write("Invalid Data Team2");
                        return;
                    }
                    matchStats.put("team2", stats.get("team2"));
                    matchStats.put("team2_wickets", stats.get("team2_wickets"));
                    matchStats.put("team2_balls", stats.get("team2_balls"));
                    if (Integer.parseInt(matchStats.get("team1")) < Integer.parseInt(matchStats.get("team2"))) {
                        matchStats.put("winner", "team2");
                        matchStats.put("is_completed", "true");
                    }
                    if (Integer.parseInt(matchStats.get("team2_balls")) == 120 || Integer.parseInt(matchStats.get("team2_wickets")) == 10) {
                        matchStats.put("is_completed", "true");
                        if (Integer.parseInt(matchStats.get("team1")) > Integer.parseInt(matchStats.get("team2"))) {
                            matchStats.put("winner", "team1");
                        } else if (Integer.parseInt(matchStats.get("team1")) < Integer.parseInt(matchStats.get("team2"))) {
                            matchStats.put("winner", "team2");
                        } else {
                            matchStats.put("winner", "tie");
                        }
                    }
                }
                context.setAttribute("match_" + matchId, matchStats);
                response.getWriter().write("success");
            }
        }
    }
}
