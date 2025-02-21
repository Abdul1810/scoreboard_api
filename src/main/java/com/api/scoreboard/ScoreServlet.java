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
        } else {/*
        team1 - number - default 0
        team2 - number - default 0
        team1_wickets - number - default 0 - max 10
        team2_wickets - number - default 0 - max 10
        team1_balls - number - default 0 - max 120
        team2_balls - number - default 0 - max 120
        current_batting - team1/team2 - default team1 - protected
        is_completed - true/false - default false - protected
        winner - team1/team2/none/tie - default none - protected
         */
//            if (!validatePositiveIntegers(score.get("team1"), score.get("team2"), score.get("team1_wickets"), score.get("team2_wickets"), score.get("team1_balls"), score.get("team2_balls"))) {
//                response.setStatus(400);
//                response.getWriter().write("Invalid Data");
//                return;
//            }

            if (matchScores.get("current_batting").equals("team1")) {
                if (validatePositiveIntegers(score.get("team1"), score.get("team1_wickets"), score.get("team1_balls"))) {
                    if (Integer.parseInt(score.get("team1")) < Integer.parseInt(matchScores.get("team1")) || Integer.parseInt(score.get("team1_wickets")) < Integer.parseInt(matchScores.get("team1_wickets")) || Integer.parseInt(score.get("team1_balls")) < Integer.parseInt(matchScores.get("team1_balls"))) {
                        response.setStatus(400);
                        response.getWriter().write("Corrupted Data Team1");
                        return;
                    }
                } else {
                    response.setStatus(400);
                    response.getWriter().write("Invalid Data Team1");
                    return;
                }
                matchScores.put("team1", score.get("team1"));
                matchScores.put("team1_wickets", score.get("team1_wickets"));
                matchScores.put("team1_balls", score.get("team1_balls"));
            } else if (matchScores.get("current_batting").equals("team2")) {
                if (validatePositiveIntegers(score.get("team2"), score.get("team2_wickets"), score.get("team2_balls"))) {
                    if (Integer.parseInt(score.get("team2")) < Integer.parseInt(matchScores.get("team2")) || Integer.parseInt(score.get("team2_wickets")) < Integer.parseInt(matchScores.get("team2_wickets")) || Integer.parseInt(score.get("team2_balls")) < Integer.parseInt(matchScores.get("team2_balls"))) {
                        response.setStatus(400);
                        response.getWriter().write("Corrupted Data Team2");
                        return;
                    }
                } else {
                    response.setStatus(400);
                    response.getWriter().write("Invalid Data Team2");
                    return;
                }
                matchScores.put("team2", score.get("team2"));
                matchScores.put("team2_wickets", score.get("team2_wickets"));
                matchScores.put("team2_balls", score.get("team2_balls"));
                if (Integer.parseInt(matchScores.get("team1")) < Integer.parseInt(matchScores.get("team2"))) {
                    matchScores.put("winner", "team2");
                    matchScores.put("is_completed", "true");
                }
            }
            if (Integer.parseInt(matchScores.get("team1_balls")) == 120 || Integer.parseInt(matchScores.get("team1_wickets")) == 10) {
                matchScores.put("current_batting", "team2");
            } else if (Integer.parseInt(matchScores.get("team2_balls")) == 120 || Integer.parseInt(matchScores.get("team2_wickets")) == 10) {
                matchScores.put("is_completed", "true");
                if (Integer.parseInt(matchScores.get("team1")) > Integer.parseInt(matchScores.get("team2"))) {
                    matchScores.put("winner", "team1");
                } else if (Integer.parseInt(matchScores.get("team1")) < Integer.parseInt(matchScores.get("team2"))) {
                    matchScores.put("winner", "team2");
                } else {
                    matchScores.put("winner", "tie");
                }
            }
            context.setAttribute("match_" + matchId, matchScores);
            response.getWriter().write("success");
        }
    }
}
