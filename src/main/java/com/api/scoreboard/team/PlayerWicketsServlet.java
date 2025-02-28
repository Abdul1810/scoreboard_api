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
                response.setStatus(404);
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
            response.setStatus(500);
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

        String query = "SELECT ms.match_id, " + "SUM(CASE WHEN m.team1_id = ? THEN ms.team1_player" + playerIndex + "_wickets " + "ELSE ms.team2_player" + playerIndex + "_wickets END) AS total_wickets, " + "SUM(CASE WHEN m.team1_id = ? THEN ms.team2_balls ELSE ms.team1_balls END) AS team_balls " + "FROM matches m " + "JOIN match_stats ms ON m.id = ms.match_id " + "WHERE m.team1_id = ? OR m.team2_id = ? " + "GROUP BY ms.match_id";

        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, teamId);
            stmt.setString(2, teamId);
            stmt.setString(3, teamId);
            stmt.setString(4, teamId);
            rs = stmt.executeQuery();

            int totalWickets = 0;
            int totalBallsBowled = 0;
            int matchesBowled = 0;

            while (rs.next()) {
                int matchWickets = rs.getInt("total_wickets");
                int teamBalls = rs.getInt("team_balls");
                int ballsBowled = 0;

                if (teamBalls <= 66) {
                    if (playerIndex * 6 <= teamBalls) {
                        ballsBowled = 6;
                    } else if ((playerIndex - 1) * 6 < teamBalls) {
                        ballsBowled = teamBalls - (playerIndex - 1) * 6;
                    }
                } else {
                    ballsBowled = 6;
                    int remainingBalls = teamBalls - 66;
                    if (playerIndex <= 9) {
                        if (playerIndex * 6 <= remainingBalls) {
                            ballsBowled += 6;
                        } else if ((playerIndex - 1) * 6 < remainingBalls) {
                            ballsBowled += remainingBalls - (playerIndex - 1) * 6;
                        }
                    }
                }

                totalWickets += matchWickets;
                totalBallsBowled += ballsBowled;
                if (ballsBowled > 0) {
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

//    private void writeResponse(HttpServletResponse response, Map<String, Object> jsonResponse) {
//        response.setContentType("application/json");
//        try {
//            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
//        } catch (IOException e) {
//            System.out.println("Error writing response: " + e.getMessage());
//        }
//    }
}
