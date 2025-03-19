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
import java.util.*;

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
            conn = new Database().getConnection();
            String playerQuery = "SELECT tp.player_id FROM team_players tp JOIN players p ON tp.player_id = p.id WHERE p.name = ? AND tp.team_id = ?";
            stmt = conn.prepareStatement(playerQuery);
            stmt.setString(1, player);
            stmt.setString(2, teamId);
            rs = stmt.executeQuery();

            if (!rs.next()) {
                response.setStatus(404);
                jsonResponse.put("message", "Player not found");
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                return;
            }

            playerId = rs.getInt("player_id");
            String matchQuery =
                "SELECT t.name AS tournament_name, " +
                "SUM(ps1.runs) AS tournament_total_runs, " +
                "SUM(ps1.balls) AS tournament_total_balls, " +
                "SUM(ps1.wickets) AS tournament_total_wickets " +
                "FROM player_stats ps1 " +
                "JOIN matches m ON ps1.match_id = m.id " +
                "LEFT JOIN tournaments t ON m.tournament_id = t.id " +
                "WHERE ps1.player_id = ? " +
                "AND ? IN (m.team1_id, m.team2_id) " +
                "GROUP BY m.id";

            stmt = conn.prepareStatement(matchQuery);
            stmt.setInt(1, playerId);
            stmt.setString(2, teamId);
            rs = stmt.executeQuery();

            int totalRuns = 0;
            int matchesPlayed = 0;
            int totalBalls = 0;
            List<Map<String, Object>> tournamentStats = new ArrayList<>();

            while (rs.next()) {
                String tournamentName = rs.getString("tournament_name");
                int tournamentRuns = rs.getInt("tournament_total_runs");
                int tournamentBalls = rs.getInt("tournament_total_balls");
                int tournamentWickets = rs.getInt("tournament_total_wickets");

                totalRuns += tournamentRuns;
                totalBalls += tournamentBalls;

                if (tournamentBalls > 0) {
                    matchesPlayed++;
                }

                if (tournamentName != null) {
                    boolean found = false;
                    for (Map<String, Object> tournamentData : tournamentStats) {
                        if (tournamentData.get("name").equals(tournamentName)) {
                            found = true;
                            tournamentData.put("total_runs", (int) tournamentData.get("total_runs") + tournamentRuns);
                            tournamentData.put("total_balls", (int) tournamentData.get("total_balls") + tournamentBalls);
                            tournamentData.put("total_wickets", (int) tournamentData.get("total_wickets") + tournamentWickets);
                            tournamentData.put("total_matches", (int) tournamentData.get("total_matches") + 1);
                            break;
                        }
                    }

                    if (!found) {
                        Map<String, Object> tournamentData = new HashMap<>();
                        tournamentData.put("name", tournamentName);
                        tournamentData.put("total_runs", tournamentRuns);
                        tournamentData.put("total_balls", tournamentBalls);
                        tournamentData.put("total_wickets", tournamentWickets);
                        tournamentData.put("total_matches", 1);
                        tournamentStats.add(tournamentData);
                    }
                }
            }

            jsonResponse.put("player", player);
            jsonResponse.put("team_id", teamId);
            jsonResponse.put("total_score", totalRuns);
            jsonResponse.put("matches_played", matchesPlayed);
            jsonResponse.put("total_balls", totalBalls);
            jsonResponse.put("tournaments", tournamentStats);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
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
