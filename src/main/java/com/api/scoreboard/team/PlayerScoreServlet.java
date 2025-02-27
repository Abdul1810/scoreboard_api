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
        int playerIndex = -1;

        try {
            conn = Database.getConnection();

            stmt = conn.prepareStatement("SELECT * FROM teams WHERE id = ? AND (" + "player1 = ? OR player2 = ? OR player3 = ? OR player4 = ? OR player5 = ? OR " + "player6 = ? OR player7 = ? OR player8 = ? OR player9 = ? OR player10 = ? OR player11 = ?)");

            stmt.setString(1, teamId);
            for (int i = 2; i <= 12; i++) {
                stmt.setString(i, player);
            }

            rs = stmt.executeQuery();

            if (!rs.next()) {
                response.setStatus(404);
                jsonResponse.put("message", "player not found");
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
                jsonResponse.put("message", "player not found in team");
                try {
                    response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                } catch (IOException e) {
                    System.out.println("Error: " + e.getMessage());
                }
                return;
            }

            String query =
                "SELECT SUM(CASE WHEN m.team1_id = ? THEN ms.team1_player" + playerIndex + "_runs ELSE ms.team2_player" + playerIndex + "_runs END) AS total_score, " +
                "COUNT(CASE WHEN (m.team1_id = ? AND ms.team1_wickets >= " + (playerIndex-1) + ") OR " +
                "(m.team2_id = ? AND ms.team2_wickets >= " + (playerIndex-1) + ") THEN 1 END) AS matches_played " +
                "FROM matches m " +
                "JOIN match_stats ms ON m.id = ms.match_id " +
                "WHERE m.team1_id = ? OR m.team2_id = ?";

            stmt = conn.prepareStatement(query);
            stmt.setString(1, teamId);
            stmt.setString(2, teamId);
            stmt.setString(3, teamId);
            stmt.setString(4, teamId);
            stmt.setString(5, teamId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                int totalScore = rs.getInt("total_score");
                int matchesPlayed = rs.getInt("matches_played");

                if (matchesPlayed == 0) {
                    response.setStatus(404);
                    jsonResponse.put("message", player + " has not batted in any matches");
                } else {
                    jsonResponse.put("player", player);
                    jsonResponse.put("team_id", teamId);
                    jsonResponse.put("total_score", totalScore);
                    jsonResponse.put("matches_played", matchesPlayed);
                }
            } else {
                response.setStatus(404);
                jsonResponse.put("message", player + " has not batted in any matches");
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
