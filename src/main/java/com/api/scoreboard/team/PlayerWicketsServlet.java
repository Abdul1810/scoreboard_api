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
            int playerId = getPlayerId(conn, teamId, player);

            if (playerId == -1) {
                jsonResponse.put("message", "Player not found in team");
                response.setStatus(404);
                try {
                    response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                } catch (IOException e) {
                    System.out.println("Error writing response: " + e.getMessage());
                }
                return;
            }

            int playerIndex = getPlayerIndex(conn, teamId, playerId);

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

            fetchPlayerWicketsData(conn, jsonResponse, teamId, playerId, player, playerIndex);
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

    private int getPlayerId(Connection conn, String teamId, String player) throws Exception {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String query = "SELECT id FROM players WHERE name = ? AND team_id = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, player);
            stmt.setString(2, teamId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
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

    private void fetchPlayerWicketsData(Connection conn, Map<String, Object> jsonResponse, String teamId, int playerId, String player, int playerIndex) throws Exception {
        PreparedStatement stmt = null;
        ResultSet rs = null;

        // Query to get the player's wickets and the total balls faced by the opposing team
        String query = "SELECT ps.wickets, " +
                "SUM(CASE WHEN ps_opp.team_id != ? THEN ps_opp.balls ELSE 0 END) AS opponent_balls " +
                "FROM player_stats ps " +
                "JOIN matches m ON ps.match_id = m.id " +
                "JOIN player_stats ps_opp ON ps.match_id = ps_opp.match_id " +
                "WHERE ps.player_id = ? AND (m.team1_id = ? OR m.team2_id = ?) " +
                "GROUP BY ps.match_id, ps.wickets";

        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, teamId);
            stmt.setInt(2, playerId);
            stmt.setString(3, teamId);
            stmt.setString(4, teamId);
            rs = stmt.executeQuery();

            int totalWickets = 0;
            int totalBallsBowled = 0;
            int matchesBowled = 0;

            while (rs.next()) {
                int matchWickets = rs.getInt("wickets");
                int matchBalls = rs.getInt("opponent_balls");
                int thisMatchBalls = 0;

                totalWickets += matchWickets;
                if (matchBalls <= 66) {
                    if (playerIndex * 6 <= matchBalls) {
                        thisMatchBalls += 6;
                    } else if ((playerIndex - 1) * 6 < matchBalls) {
                        thisMatchBalls += matchBalls - (playerIndex - 1) * 6;
                    }
                } else {
                    thisMatchBalls += 6;
                    int remainingBalls = matchBalls - 66;
                    if (playerIndex <= 9) {
                        if (playerIndex * 6 <= remainingBalls) {
                            thisMatchBalls += 6;
                        } else if ((playerIndex - 1) * 6 < remainingBalls) {
                            thisMatchBalls += remainingBalls - (playerIndex - 1) * 6;
                        }
                    }
                }

                if (thisMatchBalls > 0) {
                    matchesBowled++;
                    totalBallsBowled += thisMatchBalls;
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

    private int getPlayerIndex(Connection conn, String teamId, int playerId) throws Exception {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String query = "SELECT id FROM players WHERE team_id = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, teamId);
            rs = stmt.executeQuery();
            int index = 1;
            while (rs.next()) {
                if (rs.getInt("id") == playerId) {
                    return index;
                }
                index++;
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
}
