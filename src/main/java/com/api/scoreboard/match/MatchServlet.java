package com.api.scoreboard.match;

import com.api.scoreboard.StatsListener;
import com.api.util.Database;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/matches")
public class MatchServlet extends HttpServlet {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> jsonResponse = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String matchId = request.getParameter("id");
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = Database.getConnection();
            if (matchId != null) {
                String query = "SELECT * FROM matches WHERE id = ?";
                stmt = conn.prepareStatement(query);
                stmt.setInt(1, Integer.parseInt(matchId));
                rs = stmt.executeQuery();

                if (rs.next()) {
                    Map<String, Integer> matchIds = new HashMap<>();
                    matchIds.put("id", rs.getInt("id"));
                    matchIds.put("team1_id", rs.getInt("team1_id"));
                    matchIds.put("team2_id", rs.getInt("team2_id"));

                    Map<String, Object> match = new HashMap<>();
                    match.put("id", String.valueOf(matchIds.get("id")));

                    conn = Database.getConnection();
                    query = "SELECT * FROM teams WHERE id = ?";
                    stmt = conn.prepareStatement(query);

                    stmt.setInt(1, matchIds.get("team1_id"));
                    rs = stmt.executeQuery();
                    /*
                    {
                        "id": "1",
                        "team1": "India",
                        "team2": "Australia"
                        "team1_players": String[],
                        "team2_players": String[]
                     */

                    if (rs.next()) {
                        match.put("team1", rs.getString("name"));
                        List<String> team1Players = new ArrayList<>();
                        for (int i = 1; i <= 11; i++) {
                            team1Players.add(rs.getString("player" + i));
                        }
                        match.put("team1_players", team1Players);
                    }

                    stmt.setInt(1, matchIds.get("team2_id"));
                    rs = stmt.executeQuery();

                    if (rs.next()) {
                        match.put("team2", rs.getString("name"));
                        List<String> team2Players = new ArrayList<>();
                        for (int i = 1; i <= 11; i++) {
                            team2Players.add(rs.getString("player" + i));
                        }
                        match.put("team2_players", team2Players);
                    }



                    response.getWriter().write(objectMapper.writeValueAsString(match));
                } else {
                    response.setStatus(404);
                    jsonResponse.put("message", "Match not found");
                    response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                }
            } else {
                String query = "SELECT * FROM matches";
                stmt = conn.prepareStatement(query);
                rs = stmt.executeQuery();

                List<Map<String, String>> matches = new ArrayList<>();
                while (rs.next()) {
                    Map<String, String> match = new HashMap<>();
                    match.put("id", String.valueOf(rs.getInt("id")));

                    conn = Database.getConnection();
                    query = "SELECT * FROM teams WHERE id = ?";
                    try (PreparedStatement stmt1 = conn.prepareStatement(query)) {
                        stmt1.setInt(1, rs.getInt("team1_id"));
                        rs = stmt1.executeQuery();

                        if (rs.next()) {
                            match.put("team1", rs.getString("name"));
                        }

                        stmt1.setInt(1, rs.getInt("team2_id"));
                        rs = stmt1.executeQuery();

                        if (rs.next()) {
                            match.put("team2", rs.getString("name"));
                        }

                        matches.add(match);
                    }
                }

                response.getWriter().write(objectMapper.writeValueAsString(matches));
            }
        } catch (SQLException e) {
            jsonResponse.put("message", "Database error: " + e.getMessage());
            response.setStatus(500);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } catch (NumberFormatException e) {
            jsonResponse.put("message", "Invalid match ID format");
            response.setStatus(400);
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


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, String> match = objectMapper.readValue(request.getReader(), HashMap.class);
        Connection conn = null;
        PreparedStatement checkStmt = null;
        PreparedStatement insertStmt = null;
        PreparedStatement insertStatsStmt = null;
        ResultSet rs = null;

        try {
            conn = Database.getConnection();
            String team1 = match.get("team1");
            String team2 = match.get("team2");

            if (team1 == null || team2 == null || team1.trim().isEmpty() || team2.trim().isEmpty()) {
                jsonResponse.put("message", "Invalid team names");
                response.setStatus(400);
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                return;
            }

            String insertQuery = "INSERT INTO matches (team1_id, team2_id) VALUES (?, ?)";
            insertStmt = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
            insertStmt.setString(1, team1);
            insertStmt.setString(2, team2);
            insertStmt.executeUpdate();

            rs = insertStmt.getGeneratedKeys();
            if (rs.next()) {
                int matchId = rs.getInt(1);
                String insertStatsQuery = "INSERT INTO match_stats (" +
                        "match_id, team1_player1_runs, team1_player2_runs, team1_player3_runs, team1_player4_runs, team1_player5_runs, " +
                        "team1_player6_runs, team1_player7_runs, team1_player8_runs, team1_player9_runs, team1_player10_runs, team1_player11_runs, " +
                        "team2_player1_runs, team2_player2_runs, team2_player3_runs, team2_player4_runs, team2_player5_runs, " +
                        "team2_player6_runs, team2_player7_runs, team2_player8_runs, team2_player9_runs, team2_player10_runs, team2_player11_runs, " +
                        "team1_player1_wickets, team1_player2_wickets, team1_player3_wickets, team1_player4_wickets, team1_player5_wickets, " +
                        "team1_player6_wickets, team1_player7_wickets, team1_player8_wickets, team1_player9_wickets, team1_player10_wickets, team1_player11_wickets, " +
                        "team2_player1_wickets, team2_player2_wickets, team2_player3_wickets, team2_player4_wickets, team2_player5_wickets, " +
                        "team2_player6_wickets, team2_player7_wickets, team2_player8_wickets, team2_player9_wickets, team2_player10_wickets, team2_player11_wickets, " +
                        "team1_balls, team2_balls, current_batting, is_completed, winner) " +
                        "VALUES (?, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, " +
                        "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, " +
                        "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, " +
                        "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, " +
                        "0, 0, 'team1', 'false', 'none');";

                insertStatsStmt = conn.prepareStatement(insertStatsQuery);
                insertStatsStmt.setInt(1, matchId);
                insertStatsStmt.executeUpdate();

            }

            MatchListener.fireMatchesUpdate();
            jsonResponse.put("message", "success");
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } catch (SQLException e) {
            jsonResponse.put("message", "Database error: " + e.getMessage());
            response.setStatus(500);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } finally {
            try {
                if (rs != null) rs.close();
//                if (checkStmt != null) checkStmt.close();
                if (insertStmt != null) insertStmt.close();
                if (insertStatsStmt != null) insertStatsStmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String matchId = request.getParameter("id");

        if (matchId == null || matchId.trim().isEmpty()) {
            jsonResponse.put("message", "Match ID is required");
            response.setStatus(400);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            return;
        }

        Connection conn = null;
        PreparedStatement deleteMatchStmt = null;
        PreparedStatement deleteStmt = null;

        try {
            conn = Database.getConnection();

            if (conn == null) {
                jsonResponse.put("message", "Database connection error");
                response.setStatus(500);
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                return;
            }

            int matchIdInt;
            try {
                matchIdInt = Integer.parseInt(matchId);
            } catch (NumberFormatException e) {
                jsonResponse.put("message", "Invalid match ID format");
                response.setStatus(400);
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                return;
            }

            String deleteMatchQuery = "DELETE FROM match_stats WHERE match_id = ?";
            deleteMatchStmt = conn.prepareStatement(deleteMatchQuery);
            deleteMatchStmt.setInt(1, matchIdInt);
            deleteMatchStmt.executeUpdate();

            String deleteQuery = "DELETE FROM matches WHERE id = ?";
            deleteStmt = conn.prepareStatement(deleteQuery);
            deleteStmt.setInt(1, matchIdInt);
            int affectedRows = deleteStmt.executeUpdate();

            if (affectedRows == 0) {
                jsonResponse.put("message", "Match not found");
                response.setStatus(404);
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                return;
            }

            StatsListener.fireStatsRemove(matchId);
            MatchListener.fireMatchesUpdate();
            jsonResponse.put("message", "success");
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } catch (SQLException e) {
            jsonResponse.put("message", "Database error: " + e.getMessage());
            response.setStatus(500);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } finally {
            try {
                if (deleteMatchStmt != null) deleteMatchStmt.close();
                if (deleteStmt != null) deleteStmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }
}
