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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        int playerIndex = -1;

        try {
            conn = Database.getConnection();

            stmt = conn.prepareStatement("SELECT * FROM teams WHERE id = ? AND (" +
                    "player1 = ? OR player2 = ? OR player3 = ? OR player4 = ? OR player5 = ? OR " +
                    "player6 = ? OR player7 = ? OR player8 = ? OR player9 = ? OR player10 = ? OR player11 = ?)");

            stmt.setString(1, teamId);
            for (int i = 2; i <= 12; i++) {
                stmt.setString(i, player);
            }

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

            for (int i = 1; i <= 11; i++) {
                if (rs.getString("player" + i).equals(player)) {
                    playerIndex = i;
                    break;
                }
            }

            if (playerIndex == -1) {
                response.setStatus(404);
                jsonResponse.put("message", "Player not found in team");
                try {
                    response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                } catch (IOException e) {
                    System.out.println("Error: " + e.getMessage());
                }
                return;
            }

            String matchQuery = "SELECT ms.*, ts1.*, ts2.* " +
                "FROM match_stats ms " +
                "JOIN team_stats ts1 ON ms.team1_stats_id = ts1.id " +
                "JOIN team_stats ts2 ON ms.team2_stats_id = ts2.id " +
                "WHERE ts1.team_id = ? OR ts2.team_id = ?";

            stmt = conn.prepareStatement(matchQuery);
            stmt.setString(1, teamId);
            stmt.setString(2, teamId);
            rs = stmt.executeQuery();

            int totalScore = 0;
            int matchesPlayed = 0;

            while (rs.next()) {
                int team1StatsId = rs.getInt("ts1.id");
                int teamId1 = rs.getInt("ts1.team_id");
                int team2StatsId = rs.getInt("ts2.id");
                int teamId2 = rs.getInt("ts2.team_id");

                int playerRuns = 0;
                int opponentWickets = 0;

                if (teamId1 == Integer.parseInt(teamId)) {
                    playerRuns = rs.getInt("ts1.player" + playerIndex + "_runs");
                    opponentWickets = rs.getInt("ts2.total_wickets");
                } else {
                    playerRuns = rs.getInt("ts2.player" + playerIndex + "_runs");
                    opponentWickets = rs.getInt("ts1.total_wickets");
                }

                totalScore += playerRuns;

                if (playerIndex <= opponentWickets) {
                    matchesPlayed++;
                }
            }

            if (matchesPlayed == 0) {
                response.setStatus(404);
                jsonResponse.put("message", player + " has not batted in any matches");
            } else {
                jsonResponse.put("player", player);
                jsonResponse.put("team_id", teamId);
                jsonResponse.put("total_score", totalScore);
                jsonResponse.put("matches_played", matchesPlayed);
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
