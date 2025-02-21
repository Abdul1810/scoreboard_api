package com.api.scoreboard.match;

import com.api.scoreboard.StatsListener;
import com.api.util.Database;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
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

import static com.api.scoreboard.StatsListener.fireStatsRemove;
import static com.api.scoreboard.match.MatchListener.fireMatchesUpdate;

@WebServlet("/api/matches")
public class MatchServlet extends HttpServlet {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static Map<String, String> error = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String matchId = request.getParameter("id");
        Map<String, String> error = new HashMap<>();
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
                    Map<String, String> match = new HashMap<>();
                    match.put("id", String.valueOf(rs.getInt("id")));
                    match.put("team1", rs.getString("team1"));
                    match.put("team2", rs.getString("team2"));

                    response.setContentType("application/json");
                    response.getWriter().write(objectMapper.writeValueAsString(match));
                } else {
                    response.setStatus(404);
                    error.put("error", "Match not found");
                    response.getWriter().write(objectMapper.writeValueAsString(error));
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
            error.put("error", "Database error: " + e.getMessage());
            response.setStatus(500);
            response.getWriter().write(objectMapper.writeValueAsString(error));
        } catch (NumberFormatException e) {
            error.put("error", "Invalid match ID format");
            response.setStatus(400);
            response.getWriter().write(objectMapper.writeValueAsString(error));
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
        Map<String, String> error = new HashMap<>();
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
                error.put("error", "Invalid team names");
                response.setStatus(400);
                response.setContentType("application/json");
                response.getWriter().write(objectMapper.writeValueAsString(error));
                return;
            }

            String checkQuery = "SELECT id FROM matches WHERE team1 = ? AND team2 = ?";
            checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, team1);
            checkStmt.setString(2, team2);
            rs = checkStmt.executeQuery();

            if (rs.next()) {
                error.put("error", "Match already exists");
                response.setStatus(400);
                response.setContentType("application/json");
                response.getWriter().write(objectMapper.writeValueAsString(error));
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

            fireMatchesUpdate();
            response.setContentType("application/json");
            response.getWriter().write("success");
        } catch (SQLException e) {
            error.put("error", "Database error: " + e.getMessage());
            response.setStatus(500);
            response.setContentType("application/json");
            response.getWriter().write(objectMapper.writeValueAsString(error));
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
        Map<String, String> error = new HashMap<>();
        String matchId = request.getParameter("id");

        if (matchId == null || matchId.trim().isEmpty()) {
            error.put("error", "Match ID is required");
            response.setStatus(400);
            response.setContentType("application/json");
            response.getWriter().write(objectMapper.writeValueAsString(error));
            return;
        }

        Connection conn = null;
        PreparedStatement deleteStatsStmt = null;
        PreparedStatement deleteStmt = null;

        try {
            conn = Database.getConnection();

            if (conn == null) {
                error.put("error", "Database connection error");
                response.setStatus(500);
                response.setContentType("application/json");
                response.getWriter().write(objectMapper.writeValueAsString(error));
                return;
            }

            int matchIdInt;
            try {
                matchIdInt = Integer.parseInt(matchId);
            } catch (NumberFormatException e) {
                error.put("error", "Invalid match ID format");
                response.setStatus(400);
                response.setContentType("application/json");
                response.getWriter().write(objectMapper.writeValueAsString(error));
                return;
            }

            String deleteStatsQuery = "DELETE FROM match_stats WHERE match_id = ?";
            deleteStatsStmt = conn.prepareStatement(deleteStatsQuery);
            deleteStatsStmt.setInt(1, matchIdInt);
            deleteStatsStmt.executeUpdate();

            String deleteQuery = "DELETE FROM matches WHERE id = ?";
            deleteStmt = conn.prepareStatement(deleteQuery);
            deleteStmt.setInt(1, matchIdInt);
            int affectedRows = deleteStmt.executeUpdate();

            if (affectedRows == 0) {
                error.put("error", "Match not found");
                response.setStatus(404);
                response.setContentType("application/json");
                response.getWriter().write(objectMapper.writeValueAsString(error));
                return;
            }

            fireStatsRemove(matchId);
            response.setContentType("application/json");
            response.getWriter().write("success");
        } catch (SQLException e) {
            error.put("error", "Database error: " + e.getMessage());
            response.setStatus(500);
            response.setContentType("application/json");
            response.getWriter().write(objectMapper.writeValueAsString(error));
        } finally {
            try {
                if (deleteStatsStmt != null) deleteStatsStmt.close();
                if (deleteStmt != null) deleteStmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }
}
