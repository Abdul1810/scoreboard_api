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

@WebServlet("/api/total-wickets")
public class PlayerWicketsServlet extends HttpServlet {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> jsonResponse = new HashMap<>();
        String player = request.getParameter("player");
        String teamId = request.getParameter("teamId");

        if (player == null || player.isEmpty() || teamId == null || teamId.isEmpty()) {
            jsonResponse.put("message", "Missing parameters");
            response.setStatus(404);
            try {
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            } catch (IOException e) {
                System.out.println("Error writing response: " + e.getMessage());
            }
            return;
        }

        Connection conn = null;

        try {
            conn = Database.getConnection();
            int playerIndex = getPlayerIndex(conn, teamId, player);

            if (playerIndex == -1) {
                jsonResponse.put("message", "Player not found in team");
                response.setStatus(404);
                try {
                    response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                } catch (IOException e) {
                    System.out.println("Error writing response: " + e.getMessage());
                }
                return;
            }

            fetchPlayerWicketsData(conn, jsonResponse, teamId, playerIndex, player);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } catch (Exception e) {
            jsonResponse.put("message", "Database error: " + e.getMessage());
            response.setStatus(500);
            try {
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            } catch (IOException ex) {
                System.out.println("Error writing response: " + ex.getMessage());
            }
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    System.out.println("Error closing connection: " + e.getMessage());
                }
            }
        }
    }

    private int getPlayerIndex(Connection conn, String teamId, String player) throws Exception {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String query = "SELECT * FROM teams WHERE id = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, teamId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                for (int i = 1; i <= 11; i++) {
                    if (player.equals(rs.getString("player" + i))) {
                        return i;
                    }
                }
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (stmt != null) {
                stmt.close();
            }
        }
        return -1;
    }

    private void fetchPlayerWicketsData(Connection conn, Map<String, Object> jsonResponse, String teamId, int playerIndex, String player) throws Exception {
        PreparedStatement stmt = null;
        ResultSet rs = null;

        String query = "SELECT ms.*, ts1.*, ts2.* " +
                "FROM match_stats ms " +
                "JOIN team_stats ts1 ON ms.team1_stats_id = ts1.id " +
                "JOIN team_stats ts2 ON ms.team2_stats_id = ts2.id " +
                "WHERE ts1.team_id = ? OR ts2.team_id = ?";

        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, teamId);
            stmt.setString(2, teamId);
            rs = stmt.executeQuery();

            int totalWickets = 0;
            int totalBallsBowled = 0;
            int matchesBowled = 0;

            while (rs.next()) {
                int team1StatsId = rs.getInt("ts1.id");
                int teamId1 = rs.getInt("ts1.team_id");
                int team2StatsId = rs.getInt("ts2.id");
                int teamId2 = rs.getInt("ts2.team_id");

                int matchWickets = 0;
                int opponentBalls = 0;

                if (teamId1 == Integer.parseInt(teamId)) {
                    matchWickets = rs.getInt("ts1.player" + playerIndex + "_wickets");
                    opponentBalls = rs.getInt("ts2.balls");
                } else {
                    matchWickets = rs.getInt("ts2.player" + playerIndex + "_wickets");
                    opponentBalls = rs.getInt("ts1.balls");
                }

                totalWickets += matchWickets;

                if (opponentBalls <= 66) {
                    if (playerIndex * 6 <= opponentBalls) {
                        totalBallsBowled += 6;
                    } else if ((playerIndex - 1) * 6 < opponentBalls) {
                        totalBallsBowled += opponentBalls - (playerIndex - 1) * 6;
                    }
                } else {
                    totalBallsBowled += 6;
                    int remainingBalls = opponentBalls - 66;
                    if (playerIndex <= 9) {
                        if (playerIndex * 6 <= remainingBalls) {
                            totalBallsBowled += 6;
                        } else if ((playerIndex - 1) * 6 < remainingBalls) {
                            totalBallsBowled += remainingBalls - (playerIndex - 1) * 6;
                        }
                    }
                }

                if (totalBallsBowled > 0) {
                    matchesBowled++;
                }
            }

            jsonResponse.put("player", player);
            jsonResponse.put("team_id", teamId);
            jsonResponse.put("total_wickets", totalWickets);
            jsonResponse.put("balls_bowled", totalBallsBowled);
            jsonResponse.put("matches_bowled", matchesBowled);
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (stmt != null) {
                stmt.close();
            }
        }
    }
}
