package com.api.scoreboard.match;

import com.api.scoreboard.stats.StatsListener;
import com.api.scoreboard.commons.Match;
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
        ResultSet rs1 = null;

        try {
            conn = Database.getConnection();
            if (matchId != null) {
                String query = "SELECT * FROM matches WHERE id = ?";
                stmt = conn.prepareStatement(query);
                stmt.setInt(1, Integer.parseInt(matchId));
                rs = stmt.executeQuery();

                if (rs.next()) {
                    query = "    SELECT m.id, m.team1_id, m.team2_id, m.is_completed" +
                            "    FROM matches m" +
                            "    WHERE m.tournament_id = ?" +
                            "    AND m.is_completed = 'false'" +
                            "    ORDER BY m.id";
                    stmt = conn.prepareStatement(query);
                    stmt.setInt(1, rs.getInt("tournament_id"));
                    rs1 = stmt.executeQuery();
                    List<Integer> tournamentMatches = new ArrayList<>();

                    while (rs1.next()) {
                        tournamentMatches.add(rs1.getInt("id"));
                    }

                    if (!tournamentMatches.isEmpty() && tournamentMatches.contains(rs.getInt("id"))) {
                        System.out.println(tournamentMatches.size());
                        tournamentMatches.forEach(System.out::println);
                        if (tournamentMatches.get(0) != rs.getInt("id")) {
                            response.setStatus(403);
                            jsonResponse.put("message", "Match cannot be accessed");
                            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                            return;
                        }
                    }

                    Map<String, Integer> matchIds = new HashMap<>();
                    matchIds.put("id", rs.getInt("id"));
                    matchIds.put("team1_id", rs.getInt("team1_id"));
                    matchIds.put("team2_id", rs.getInt("team2_id"));

                    Map<String, Object> match = new HashMap<>();
                    match.put("id", String.valueOf(matchIds.get("id")));
                    match.put("is_completed", rs.getString("is_completed"));
                    match.put("highlights_path", rs.getString("highlights_path"));
                    match.put("winner", rs.getString("winner"));
                    match.put("current_batting", rs.getString("current_batting"));
                    match.put("active_batsman_index", rs.getInt("active_batsman_index"));
                    match.put("passive_batsman_index", rs.getInt("passive_batsman_index"));

                    query = "SELECT t.name, p.name as player_name FROM teams t JOIN team_players tp ON t.id = tp.team_id JOIN players p ON tp.player_id = p.id WHERE t.id = ?";
                    stmt = conn.prepareStatement(query);

                    stmt.setInt(1, matchIds.get("team1_id"));
                    rs = stmt.executeQuery();

                    if (rs.next()) {
                        match.put("team1", rs.getString("name"));
                        List<String> team1Players = new ArrayList<>();
                        do {
                            team1Players.add(rs.getString("player_name"));
                        } while (rs.next());
                        match.put("team1_players", team1Players);
                    }

                    stmt.setInt(1, matchIds.get("team2_id"));
                    rs = stmt.executeQuery();

                    if (rs.next()) {
                        match.put("team2", rs.getString("name"));
                        List<String> team2Players = new ArrayList<>();
                        do {
                            team2Players.add(rs.getString("player_name"));
                        } while (rs.next());
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
        } finally {
            try {
                if (rs != null) rs.close();
                if (rs1 != null) rs1.close();
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
            conn = Database.getConnection();
            String team1Id = match.get("team1");
            String team2Id = match.get("team2");

            if (team1Id == null || team2Id == null || team1Id.trim().isEmpty() || team2Id.trim().isEmpty()) {
                jsonResponse.put("message", "Invalid team names");
                response.setStatus(400);
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                return;
            }

            Match.create(conn, Integer.parseInt(team1Id), Integer.parseInt(team2Id));
            MatchListener.fireMatchesUpdate();
            jsonResponse.put("message", "success");
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

            // Delete from player_stats table
            String deletePlayerStatsQuery = "DELETE FROM player_stats WHERE match_id = ?";
            deletePlayerStatsStmt = conn.prepareStatement(deletePlayerStatsQuery);
            deletePlayerStatsStmt.setInt(1, matchIdInt);
            deletePlayerStatsStmt.executeUpdate();

            // Delete from matches table
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
                if (deletePlayerStatsStmt != null) deletePlayerStatsStmt.close();
                if (deleteMatchStmt != null) deleteMatchStmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }
}
