package com.api.scoreboard_old.match;

import com.api.scoreboard_old.StatsListener;
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

@WebServlet("/old/api/matches")
public class MatchServlet extends HttpServlet {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> jsonResponse = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String matchId = request.getParameter("id");
        Connection conn;
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
                    Map<String, String> match = new HashMap<>();
                    match.put("id", String.valueOf(rs.getInt("id")));
                    match.put("team1", rs.getString("team1"));
                    match.put("team2", rs.getString("team2"));

                    response.setContentType("application/json");
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
                    match.put("team1", rs.getString("team1"));
                    match.put("team2", rs.getString("team2"));
                    matches.add(match);
                }

                response.setContentType("application/json");
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

            String checkQuery = "SELECT id FROM matches WHERE team1 = ? AND team2 = ?";
            checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, team1);
            checkStmt.setString(2, team2);
            rs = checkStmt.executeQuery();

            if (rs.next()) {
                jsonResponse.put("message", "Match already exists");
                response.setStatus(400);
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                return;
            }

            String insertQuery = "INSERT INTO matches (team1, team2) VALUES (?, ?)";
            insertStmt = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
            insertStmt.setString(1, team1);
            insertStmt.setString(2, team2);
            insertStmt.executeUpdate();

            rs = insertStmt.getGeneratedKeys();
            if (rs.next()) {
                int matchId = rs.getInt(1);

                String insertStatsQuery = "INSERT INTO match_stats (match_id, team1, team2, team1_wickets, team2_wickets, team1_balls, team2_balls, current_batting, is_completed, winner) " +
                        "VALUES (?, 0, 0, 0, 0, 0, 0, 'team1', 'false', 'none')";
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
                if (checkStmt != null) checkStmt.close();
                if (insertStmt != null) insertStmt.close();
                if (insertStatsStmt != null) insertStatsStmt.close();
                if (conn != null) conn.close();
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
