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
            conn = new Database().getConnection();
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

            fetchPlayerWicketsData(conn, jsonResponse, Integer.parseInt(teamId), playerId, player, playerIndex);
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
        String query = "SELECT tp.player_id FROM team_players tp JOIN players p ON tp.player_id = p.id WHERE p.name = ? AND tp.team_id = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, player);
            stmt.setString(2, teamId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("player_id");
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

    private void fetchPlayerWicketsData(Connection conn, Map<String, Object> jsonResponse, int teamId, int playerId, String player, int playerIndex) throws Exception {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String query = "SELECT ps.wickets, ps.no_balls, ps.wide_balls, " +
                "SUM(CASE WHEN ps_opp.team_id != ? THEN ps_opp.balls ELSE 0 END) AS opponent_balls, " +
                "to1.bowling_order " +
                "FROM player_stats ps " +
                "JOIN matches m ON ps.match_id = m.id " +
                "JOIN player_stats ps_opp ON ps.match_id = ps_opp.match_id " +
                "JOIN team_order to1 ON to1.team_id = ? AND to1.match_id = m.id " +
                "WHERE ps.player_id = ? AND (m.team1_id = ? OR m.team2_id = ?) " +
                "GROUP BY ps.match_id, ps.wickets";

        try {
            stmt = conn.prepareStatement(query);
            System.out.println("teamId: " + teamId);
            System.out.println("playerId: " + playerId);
            stmt.setInt(1, teamId);
            stmt.setInt(2, teamId);
            stmt.setInt(3, playerId);
            stmt.setInt(4, teamId);
            stmt.setInt(5, teamId);
            rs = stmt.executeQuery();

            int totalWickets = 0;
            int totalBallsBowled = 0;
            int matchesBowled = 0;

            while (rs.next()) {
                int matchWickets = rs.getInt("wickets");
                int matchNoBalls = rs.getInt("no_balls");
                int matchWideBalls = rs.getInt("wide_balls");
                int matchBalls = rs.getInt("opponent_balls");
//                [1, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1]
                List<Integer> bowlingOrder = objectMapper.readValue(rs.getString("bowling_order"), List.class);
                System.out.println(bowlingOrder);
                System.out.println(playerIndex);
                System.out.println(matchWickets);
                System.out.println(matchBalls);
                int thisMatchBalls = 0;

                totalWickets += matchWickets;

                if (matchBalls > 0) {
                    List<Integer> ballsArray = new ArrayList<>();
                    int balls = matchBalls;
                    int i = 0;

                    while (balls > 0) {
                        if (balls >= 6) {
                            ballsArray.add(6);
                            balls -= 6;
                        } else {
                            ballsArray.add(balls);
                            balls = 0;
                        }
                        i++;
                    }

                    while (i < 11) {
                        ballsArray.add(0);
                        i++;
                    }

                    for (int j = 0; j < bowlingOrder.size(); j++) {
                        if (bowlingOrder.get(j) == playerIndex) {
                            thisMatchBalls += ballsArray.get(j);
                        }
                    }
                }
                thisMatchBalls += matchNoBalls + matchWideBalls;

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
        String query = "SELECT player_position FROM team_players WHERE team_id = ? AND player_id = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, teamId);
            stmt.setInt(2, playerId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("player_position");
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
