package com.api.scoreboard;

import com.api.util.Database;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import static com.api.util.Utils.validatePositiveIntegers;

@WebServlet("/update-stats")
public class StatsServlet extends HttpServlet {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> jsonResponse = new HashMap<>();

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String matchId = request.getParameter("id");
        Map<String, String> stats = objectMapper.readValue(request.getReader(), HashMap.class);
        System.out.println(stats);
        System.out.println(matchId);
        if (matchId == null || matchId.trim().isEmpty()) {
            response.setStatus(400);
            jsonResponse.put("message", "Invalid match ID");
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            return;
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = Database.getConnection();

            String query = "SELECT * FROM match_stats WHERE match_id = ?";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, Integer.parseInt(matchId));
            rs = stmt.executeQuery();

            if (!rs.next()) {
                response.setStatus(404);
                jsonResponse.put("message", "Match not found");
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                return;
            }

            Map<String, String> matchStats = new HashMap<>();
            matchStats.put("team1_score", rs.getString("team1_score"));
            matchStats.put("team2_score", rs.getString("team2_score"));
            matchStats.put("team1_wickets", rs.getString("team1_wickets"));
            matchStats.put("team2_wickets", rs.getString("team2_wickets"));
            matchStats.put("team1_balls", rs.getString("team1_balls"));
            matchStats.put("team2_balls", rs.getString("team2_balls"));
            matchStats.put("current_batting", rs.getString("current_batting"));
            matchStats.put("is_completed", rs.getString("is_completed"));
            matchStats.put("winner", rs.getString("winner"));

            if (matchStats.get("is_completed").equals("true")) {
                response.setStatus(400);
                jsonResponse.put("message", "Match already completed");
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                return;
            }

            if (matchStats.get("current_batting").equals("team1")) {
                if (!validateStats(stats, "team1", matchStats, response)) return;
                matchStats.put("team1_score", stats.get("team1_score"));
                matchStats.put("team1_wickets", stats.get("team1_wickets"));
                matchStats.put("team1_balls", stats.get("team1_balls"));

                if (Integer.parseInt(matchStats.get("team1_balls")) == 120 || Integer.parseInt(matchStats.get("team1_wickets")) == 10) {
                    matchStats.put("current_batting", "team2");
                }
            } else if (matchStats.get("current_batting").equals("team2")) {
                if (!validateStats(stats, "team2", matchStats, response)) return;
                matchStats.put("team2_score", stats.get("team2_score"));
                matchStats.put("team2_wickets", stats.get("team2_wickets"));
                matchStats.put("team2_balls", stats.get("team2_balls"));
                findWinner(matchStats);
            }

            updateMatchStats(conn, matchStats, matchId);
            StatsListener.fireStatsUpdate(matchId);
            response.setStatus(200);
            jsonResponse.put("message", "success");
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } catch (SQLException e) {
            jsonResponse.put("message", "Database error: " + e.getMessage());
            response.setStatus(500);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }

    private boolean validateStats(Map<String, String> stats, String team, Map<String, String> matchStats, HttpServletResponse response) throws IOException {
        if (!validatePositiveIntegers(stats.get(team + "_score"), stats.get(team + "_wickets"), stats.get(team + "_balls"))) {
            response.setStatus(400);
            jsonResponse.put("message", "Invalid Data " + team);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            return false;
        }
        System.out.println(stats.get(team));
        System.out.println(matchStats.get(team));
        if (Integer.parseInt(stats.get(team + "_score")) < Integer.parseInt(matchStats.get(team + "_score")) || Integer.parseInt(stats.get(team + "_wickets")) < Integer.parseInt(matchStats.get(team + "_wickets")) || Integer.parseInt(stats.get(team + "_balls")) < Integer.parseInt(matchStats.get(team + "_balls"))) {
            response.setStatus(400);
            jsonResponse.put("message", "Corrupted Data " + team);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            return false;
        }

        if (Integer.parseInt(stats.get(team + "_balls")) > 120 || Integer.parseInt(stats.get(team + "_wickets")) > 10) {
            response.setStatus(400);
            jsonResponse.put("message", "Invalid Data " + team);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            return false;
        }

        return true;
    }

    private void findWinner(Map<String, String> matchStats) {
        if (Integer.parseInt(matchStats.get("team1_score")) < Integer.parseInt(matchStats.get("team2_score"))) {
            matchStats.put("winner", "team2");
            matchStats.put("is_completed", "true");
        }

        if (Integer.parseInt(matchStats.get("team2_balls")) == 120 || Integer.parseInt(matchStats.get("team2_wickets")) == 10) {
            matchStats.put("is_completed", "true");
            if (Integer.parseInt(matchStats.get("team1_score")) > Integer.parseInt(matchStats.get("team2_score"))) {
                matchStats.put("winner", "team1");
            } else if (Integer.parseInt(matchStats.get("team1_score")) < Integer.parseInt(matchStats.get("team2_score"))) {
                matchStats.put("winner", "team2");
            } else {
                matchStats.put("winner", "tie");
            }
        }
    }

    private void updateMatchStats(Connection conn, Map<String, String> matchStats, String matchId) throws SQLException {
        String updateQuery = "UPDATE match_stats SET team1_score = ?, team2_score = ?, team1_wickets = ?, team2_wickets = ?, team1_balls = ?, " + "team2_balls = ?, current_batting = ?, is_completed = ?, winner = ? WHERE match_id = ?";
        PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
        updateStmt.setString(1, matchStats.get("team1_score"));
        updateStmt.setString(2, matchStats.get("team2_score"));
        updateStmt.setString(3, matchStats.get("team1_wickets"));
        updateStmt.setString(4, matchStats.get("team2_wickets"));
        updateStmt.setString(5, matchStats.get("team1_balls"));
        updateStmt.setString(6, matchStats.get("team2_balls"));
        updateStmt.setString(7, matchStats.get("current_batting"));
        updateStmt.setString(8, matchStats.get("is_completed"));
        updateStmt.setString(9, matchStats.get("winner"));
        updateStmt.setInt(10, Integer.parseInt(matchId));
        updateStmt.executeUpdate();
    }
}
