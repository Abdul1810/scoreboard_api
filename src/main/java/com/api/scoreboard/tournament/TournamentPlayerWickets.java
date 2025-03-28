package com.api.scoreboard.tournament;

import com.api.util.Database;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@WebServlet("/api/tournaments/total-wickets")
public class TournamentPlayerWickets extends HttpServlet {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> jsonResponse = new HashMap<>();
        String id = request.getParameter("id");
        String playerId = request.getParameter("player_id");

        try {
            if (id == null || playerId == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                jsonResponse.put("error", "Missing required parameter 'id' or 'player_id'");
                response.getWriter().print(objectMapper.writeValueAsString(jsonResponse));
                return;
            }
            AtomicInteger playerIndex = new AtomicInteger(-1);
            AtomicInteger teamId = new AtomicInteger(-1);
            AtomicReference<String> playerName = new AtomicReference<>("");
            getPlayerIndexAndTeamId(id, playerId, (index, id1, name) -> {
                playerIndex.set(index);
                teamId.set(id1);
                playerName.set(name);
            });
            if (playerIndex.get() == -1) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                jsonResponse.put("error", "Player not found in tournament");
                response.getWriter().print(objectMapper.writeValueAsString(jsonResponse));
                return;
            }

            fetchPlayerWicketsData(jsonResponse, teamId.get(), Integer.parseInt(playerId), Integer.parseInt(id), playerName.get(), playerIndex.get());
            response.getWriter().print(objectMapper.writeValueAsString(jsonResponse));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonResponse.put("error", "Database error: " + e.getMessage());
        }
    }

    private void fetchPlayerWicketsData(Map<String, Object> jsonResponse, int teamId, int playerId, int tournamentId, String player, int playerIndex) throws SQLException, JsonProcessingException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        String query = "SELECT ps.wickets, ps.no_balls, ps.wide_balls, " +
                "SUM(CASE WHEN ps_opp.team_id != ? THEN ps_opp.balls ELSE 0 END) AS opponent_balls, " +
                "to1.bowling_order, t.name AS tournament_name " +
                "FROM player_stats ps " +
                "JOIN matches m ON ps.match_id = m.id " +
                "JOIN player_stats ps_opp ON ps.match_id = ps_opp.match_id " +
                "JOIN team_order to1 ON to1.team_id = ? AND to1.match_id = m.id " +
                "JOIN tournaments t ON m.tournament_id = t.id " +
                "WHERE ps.player_id = ? AND (m.team1_id = ? OR m.team2_id = ?) " +
                "AND m.tournament_id = ? " +
                "GROUP BY ps.match_id, ps.wickets";

        try {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, teamId);
            stmt.setInt(2, teamId);
            stmt.setInt(3, playerId);
            stmt.setInt(4, teamId);
            stmt.setInt(5, teamId);
            stmt.setInt(6, tournamentId);
            rs = stmt.executeQuery();

            int totalWickets = 0;
            int totalBallsBowled = 0;
            int matchesBowled = 0;
            String tournamentName = "";

            while (rs.next()) {
                int matchWickets = rs.getInt("wickets");
                int matchNoBalls = rs.getInt("no_balls");
                int matchWideBalls = rs.getInt("wide_balls");
                int matchBalls = rs.getInt("opponent_balls");
                tournamentName = rs.getString("tournament_name");

                List<Integer> bowlingOrder = objectMapper.readValue(rs.getString("bowling_order"), List.class);
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
            jsonResponse.put("tournament_name", tournamentName);
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    private void getPlayerIndexAndTeamId(String tournamentId, String playerId, TriConsumer<Integer, Integer, String> callback) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String query = "SELECT tp.player_position, tp.team_id, p.name FROM team_players tp " +
                "JOIN matches m ON tp.team_id = m.team1_id OR tp.team_id = m.team2_id " +
                "JOIN players p ON tp.player_id = p.id " +
                "WHERE m.tournament_id = ? AND tp.player_id = ? LIMIT 1";
        try {
            conn = new Database().getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, tournamentId);
            stmt.setString(2, playerId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                callback.accept(rs.getInt("player_position"), rs.getInt("team_id"), rs.getString("name"));
            }
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    @FunctionalInterface
    public interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }
}
