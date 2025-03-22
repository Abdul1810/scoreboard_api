package com.api.scoreboard.match;

import com.api.scoreboard.match.embed.EmbedListener;
import com.api.scoreboard.stats.StatsListener;
import com.api.scoreboard.commons.Match;
import com.api.util.Database;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
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
        int userId = (int) request.getSession().getAttribute("uid");
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = new Database().getConnection();
            if (matchId != null) {
                Map<String, Object> matchData = Match.get(conn, Integer.parseInt(matchId), userId);
                response.getWriter().write(objectMapper.writeValueAsString(matchData));
            } else {
                String query = "SELECT * FROM matches WHERE user_id = ?";
                stmt = conn.prepareStatement(query);
                rs = stmt.executeQuery();

                List<Map<String, String>> matches = new ArrayList<>();
                while (rs.next()) {
                    Map<String, String> match = new HashMap<>();
                    match.put("id", String.valueOf(rs.getInt("id")));

                    query = "SELECT name FROM teams WHERE id = ?";
                    try (PreparedStatement stmt1 = conn.prepareStatement(query)) {
                        stmt1.setInt(1, rs.getInt("team1_id"));
                        ResultSet rsTeam = stmt1.executeQuery();
                        if (rsTeam.next()) {
                            match.put("team1", rsTeam.getString("name"));
                        }

                        stmt1.setInt(1, rs.getInt("team2_id"));
                        rsTeam = stmt1.executeQuery();

                        if (rsTeam.next()) {
                            match.put("team2", rsTeam.getString("name"));
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
        } catch (Exception e) {
            jsonResponse.put("message", "Error fetching match: " + e.getMessage());
            response.setStatus(500);
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
        PreparedStatement insertMatchStmt = null;
        ResultSet rs = null;

        try {
            conn = new Database().getConnection();
            String team1Id = match.get("team1");
            String team2Id = match.get("team2");

            if (team1Id == null || team2Id == null || team1Id.trim().isEmpty() || team2Id.trim().isEmpty()) {
                jsonResponse.put("message", "Invalid team names");
                response.setStatus(400);
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                return;
            }

            int userId = (int) request.getSession().getAttribute("uid");
            String query = "SELECT * FROM teams WHERE user_id = ? AND id IN (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, Integer.parseInt(team1Id));
                stmt.setInt(3, Integer.parseInt(team2Id));
                rs = stmt.executeQuery();
                if (!rs.next()) {
                    jsonResponse.put("message", "Invalid team names");
                    response.setStatus(400);
                    response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                    return;
                }
            }
            int matchId = Match.create(conn, userId, Integer.parseInt(team1Id), Integer.parseInt(team2Id));
            MatchListener.fireMatchesUpdate(userId);
            jsonResponse.put("message", "success");
            jsonResponse.put("id", String.valueOf(matchId));
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } catch (SQLException e) {
            jsonResponse.put("message", "Database error: " + e.getMessage());
            response.setStatus(500);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } catch (Exception e) {
            jsonResponse.put("message", "Error creating match: " + e.getMessage());
            response.setStatus(500);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } finally {
            try {
                if (rs != null) rs.close();
                if (insertMatchStmt != null) insertMatchStmt.close();
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
        PreparedStatement deletePlayerStatsStmt = null;
        PreparedStatement deleteMatchStmt = null;

        try {
            conn = new Database().getConnection();

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

            String query = "SELECT * FROM matches WHERE id = ?";
            deleteMatchStmt = conn.prepareStatement(query);
            deleteMatchStmt.setInt(1, matchIdInt);
            ResultSet rs = deleteMatchStmt.executeQuery();

            if (!rs.next()) {
                jsonResponse.put("message", "Match not found");
                response.setStatus(404);
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                return;
            }

            String highlightsPath = rs.getString("highlights_path");
            String bannerPath = rs.getString("banner_path");

            String deletePlayerStatsQuery = "DELETE FROM player_stats WHERE match_id = ?";
            deletePlayerStatsStmt = conn.prepareStatement(deletePlayerStatsQuery);
            deletePlayerStatsStmt.setInt(1, matchIdInt);
            deletePlayerStatsStmt.executeUpdate();

            String deleteMatchQuery = "DELETE FROM matches WHERE id = ?";
            deleteMatchStmt = conn.prepareStatement(deleteMatchQuery);
            deleteMatchStmt.setInt(1, matchIdInt);
            int affectedRows = deleteMatchStmt.executeUpdate();

            if (affectedRows == 0) {
                jsonResponse.put("message", "Match not found");
                response.setStatus(404);
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                return;
            }

            if (highlightsPath != null) {
                String highlightsFilePath = "\\uploads\\highlights\\" + highlightsPath;
                if (!new File(highlightsFilePath).delete()) {
                    System.err.println("Error deleting highlights file");
                }
            }

            if (bannerPath != null) {
                String bannerFilePath = "\\uploads\\banners\\" + bannerPath;
                if (!new File(bannerFilePath).delete()) {
                    System.err.println("Error deleting banner file");
                }
            }

            int userId = (int) request.getSession().getAttribute("uid");
            StatsListener.fireStatsRemove(matchId);
            EmbedListener.fireMatchRemove(matchId);
            MatchListener.fireMatchesUpdate(userId);
            jsonResponse.put("message", "success");
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } catch (SQLException e) {
            jsonResponse.put("message", "Database error: " + e.getMessage());
            response.setStatus(500);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } finally {
            try {
                if (deletePlayerStatsStmt != null) deletePlayerStatsStmt.close();
                if (deleteMatchStmt != null) deleteMatchStmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }
}
