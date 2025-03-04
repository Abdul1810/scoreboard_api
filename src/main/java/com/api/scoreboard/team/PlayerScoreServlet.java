package com.api.scoreboard.team;

import com.api.util.Database;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/total-score")
public class PlayerScoreServlet extends HttpServlet {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Object> jsonResponse = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        String player = request.getParameter("player");
        String teamId = request.getParameter("teamId");

        if (player == null || player.isEmpty() || teamId == null || teamId.isEmpty()) {
            response.setStatus(404);
            jsonResponse.put("message", "Missing parameters");
            try {
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            } catch (IOException e) {
                System.out.println("Error writing response: " + e.getMessage());
            }
            return;
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int playerId = -1;

        try {
            conn = Database.getConnection();

            // Fetch player ID using player name
            stmt = conn.prepareStatement("SELECT id FROM players WHERE name = ? AND team_id = ?");
            stmt.setString(1, player);
            stmt.setString(2, teamId);
            rs = stmt.executeQuery();

            if (!rs.next()) {
                response.setStatus(404);
                jsonResponse.put("message", "Player not found");
                try {
                    response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                } catch (IOException e) {
                    System.out.println("Error writing response: " + e.getMessage());
                }
                return;
            }

            playerId = rs.getInt("id");

            String matchQuery =
                    "SELECT ps1.match_id, ps1.runs, ps1.balls, SUM(ps2.wickets) AS wickets " +
                            "FROM player_stats ps1 " +
                            "JOIN matches m ON ps1.match_id = m.id " +
                            "LEFT JOIN player_stats ps2 " +
                            "ON ps1.match_id = ps2.match_id " +
                            "AND ps1.player_id <> ps2.player_id " +
                            "AND ps2.team_id IN (m.team1_id, m.team2_id) " +
                            "WHERE ps1.player_id = ? " +
                            "AND ? IN (m.team1_id, m.team2_id) " +
                            "GROUP BY ps1.match_id, ps1.runs, ps1.wickets, ps1.balls";

            stmt = conn.prepareStatement(matchQuery);
            stmt.setInt(1, playerId);
            stmt.setString(2, teamId);
            rs = stmt.executeQuery();

            int totalRuns = 0;
            int matchesPlayed = 0;
            int totalBalls = 0;

            while (rs.next()) {
                int playerRuns = rs.getInt("runs");
                int ballsFaced = rs.getInt("balls");

                totalRuns += playerRuns;
                totalBalls += ballsFaced;

                if (ballsFaced > 0) {
                    matchesPlayed++;
                }
            }
            if (matchesPlayed == 0) {
                response.setStatus(404);
                jsonResponse.put("message", player + " has not batted in any matches");
            } else {
                jsonResponse.put("player", player);
                jsonResponse.put("team_id", teamId);
                jsonResponse.put("total_score", totalRuns);
                jsonResponse.put("matches_played", matchesPlayed);
                jsonResponse.put("total_balls", totalBalls);
            }

            try {
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("Database error: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                System.out.println("Error closing resources: " + e.getMessage());
            }
        }
    }
}
