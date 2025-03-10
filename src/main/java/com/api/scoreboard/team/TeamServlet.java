package com.api.scoreboard.team;

import com.api.scoreboard.stats.StatsListener;
import com.api.scoreboard.match.MatchListener;
import com.api.util.Database;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.*;
import java.util.*;

@WebServlet("/api/teams")
public class TeamServlet extends HttpServlet {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> jsonResponse = new HashMap<>();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> team = objectMapper.readValue(request.getReader(), HashMap.class);
        System.out.println(team);

        Optional<String> name = Optional.ofNullable((String) team.get("name"));
        if (!team.containsKey("players")) {
            response.setStatus(400);
            jsonResponse.put("message", "Invalid request body");
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            return;
        }

        List<String> players = (List<String>) team.get("players");

        if (name.get().isEmpty() || name.get().trim().isEmpty()) {
            response.setStatus(400);
            jsonResponse.put("message", "Invalid team name");
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            return;
        }

        if (players.isEmpty() || players.size() < 11) {
            response.setStatus(400);
            jsonResponse.put("message", "Invalid number of players");
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            return;
        }

        for (int i = 0; i < 11; i++) {
            if (players.get(i) == null || players.get(i).trim().isEmpty()) {
                response.setStatus(400);
                jsonResponse.put("message", "Invalid player name at index " + i);
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                return;
            }
        }

        Connection conn = null;
        PreparedStatement checkStmt = null;
        ResultSet rs = null;

        try {
            conn = Database.getConnection();

            String checkQuery = "SELECT id FROM teams WHERE name = ?";
            checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, name.get());
            rs = checkStmt.executeQuery();

            if (rs.next()) {
                jsonResponse.put("message", "Team already exists");
                response.setStatus(400);
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                return;
            }
        } catch (Exception e) {
            jsonResponse.put("message", "Database error: " + e.getMessage());
            response.setStatus(500);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            return;
        } finally {
            try {
                if (rs != null) rs.close();
                if (checkStmt != null) checkStmt.close();
            } catch (Exception e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }

        PreparedStatement insertPlayerStmt = null;
        PreparedStatement insertTeamStmt = null;
        PreparedStatement insertTeamPlayerStmt = null;
        ResultSet generatedKeys = null;

        try {
            String insertPlayerQuery = "INSERT INTO players (name) VALUES (?)";
            insertPlayerStmt = conn.prepareStatement(insertPlayerQuery, Statement.RETURN_GENERATED_KEYS);

            List<Integer> playerIds = new ArrayList<>();
            for (String playerName : players) {
                insertPlayerStmt.setString(1, playerName);
                insertPlayerStmt.addBatch();
            }
            insertPlayerStmt.executeBatch();

            generatedKeys = insertPlayerStmt.getGeneratedKeys();
            while (generatedKeys.next()) {
                playerIds.add(generatedKeys.getInt(1));
            }

            String insertTeamQuery = "INSERT INTO teams (name) VALUES (?)";
            insertTeamStmt = conn.prepareStatement(insertTeamQuery, Statement.RETURN_GENERATED_KEYS);
            insertTeamStmt.setString(1, name.get());
            insertTeamStmt.executeUpdate();

            int teamId = 0;
            generatedKeys = insertTeamStmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                teamId = generatedKeys.getInt(1);
            }

            String insertTeamPlayerQuery = "INSERT INTO team_players (team_id, player_id, player_position) VALUES (?, ?, ?)";
            insertTeamPlayerStmt = conn.prepareStatement(insertTeamPlayerQuery);

            for (int i = 0; i < 11; i++) {
                insertTeamPlayerStmt.setInt(1, teamId);
                insertTeamPlayerStmt.setInt(2, playerIds.get(i));
                insertTeamPlayerStmt.setInt(3, i + 1); // Assuming positions are 1-based
                insertTeamPlayerStmt.addBatch();
            }
            insertTeamPlayerStmt.executeBatch();

            jsonResponse.put("message", "Team created successfully");
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } catch (SQLException e) {
            jsonResponse.put("message", "Database error: " + e.getMessage());
            response.setStatus(500);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } finally {
            try {
                if (generatedKeys != null) generatedKeys.close();
                if (insertPlayerStmt != null) insertPlayerStmt.close();
                if (insertTeamStmt != null) insertTeamStmt.close();
                if (insertTeamPlayerStmt != null) insertTeamPlayerStmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String matchId = request.getParameter("id");
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = Database.getConnection();

            if (matchId == null) {
                // Fetch all teams
                String query = "SELECT t.id, t.name FROM teams t";
                stmt = conn.prepareStatement(query);
                rs = stmt.executeQuery();

                List<Map<String, Object>> teams = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> team = new HashMap<>();
                    int teamId = rs.getInt("id");
                    team.put("id", teamId);
                    team.put("name", rs.getString("name"));

                    // Fetch players for the team using team_players table
                    String playerQuery = "SELECT p.name FROM team_players tp JOIN players p ON tp.player_id = p.id WHERE tp.team_id = ?";
                    try (PreparedStatement playerStmt = conn.prepareStatement(playerQuery)) {
                        playerStmt.setInt(1, teamId);
                        ResultSet playerRs = playerStmt.executeQuery();
                        List<String> players = new ArrayList<>();
                        while (playerRs.next()) {
                            players.add(playerRs.getString("name"));
                        }
                        team.put("players", players);
                    }

                    teams.add(team);
                }

                response.getWriter().write(objectMapper.writeValueAsString(teams));
            } else {
                // Fetch a specific team
                String query = "SELECT t.id, t.name FROM teams t WHERE t.id = ?";
                stmt = conn.prepareStatement(query);
                stmt.setInt(1, Integer.parseInt(matchId));
                rs = stmt.executeQuery();

                if (rs.next()) {
                    Map<String, Object> team = new HashMap<>();
                    int teamId = rs.getInt("id");
                    team.put("id", teamId);
                    team.put("name", rs.getString("name"));

                    // Fetch players for the team using team_players table
                    String playerQuery = "SELECT p.name FROM team_players tp JOIN players p ON tp.player_id = p.id WHERE tp.team_id = ?";
                    try (PreparedStatement playerStmt = conn.prepareStatement(playerQuery)) {
                        playerStmt.setInt(1, teamId);
                        ResultSet playerRs = playerStmt.executeQuery();
                        List<String> players = new ArrayList<>();
                        while (playerRs.next()) {
                            players.add(playerRs.getString("name"));
                        }
                        team.put("players", players);
                    }

                    response.getWriter().write(objectMapper.writeValueAsString(team));
                } else {
                    jsonResponse.put("message", "Team not found");
                    response.setStatus(404);
                    response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                }
            }
        } catch (Exception e) {
            jsonResponse.put("message", "Database error: " + e.getMessage());
            response.setStatus(500);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String id = request.getParameter("id");
        if (id == null || id.trim().isEmpty()) {
            response.setStatus(400);
            jsonResponse.put("message", "Invalid team ID");
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            return;
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = Database.getConnection();

            // Delete team_players associations
            String deleteTeamPlayersQuery = "DELETE FROM team_players WHERE team_id = ?";
            stmt = conn.prepareStatement(deleteTeamPlayersQuery);
            stmt.setInt(1, Integer.parseInt(id));
            stmt.executeUpdate();

            // Delete the team
            String deleteTeamQuery = "DELETE FROM teams WHERE id = ?";
            stmt = conn.prepareStatement(deleteTeamQuery);
            stmt.setInt(1, Integer.parseInt(id));
            stmt.executeUpdate();

            // Fetch and delete related matches
            List<Integer> matchIds = new ArrayList<>();
            String fetchMatchesQuery = "SELECT id FROM matches WHERE team1_id = ? OR team2_id = ?";
            stmt = conn.prepareStatement(fetchMatchesQuery);
            stmt.setInt(1, Integer.parseInt(id));
            stmt.setInt(2, Integer.parseInt(id));
            rs = stmt.executeQuery();

            while (rs.next()) {
                matchIds.add(rs.getInt("id"));
            }

            String deleteMatchesQuery = "DELETE FROM matches WHERE team1_id = ? OR team2_id = ?";
            stmt = conn.prepareStatement(deleteMatchesQuery);
            stmt.setInt(1, Integer.parseInt(id));
            stmt.setInt(2, Integer.parseInt(id));
            stmt.executeUpdate();

            // Delete player stats associated with the matches
            String deletePlayerStatsQuery = "DELETE FROM player_stats WHERE match_id = ?";
            for (int matchId : matchIds) {
                stmt = conn.prepareStatement(deletePlayerStatsQuery);
                stmt.setInt(1, matchId);
                stmt.executeUpdate();
            }

            MatchListener.fireMatchesUpdate();
            for (int matchId : matchIds) {
                StatsListener.fireStatsRemove(String.valueOf(matchId));
            }
            jsonResponse.put("message", "Team deleted successfully");
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } catch (Exception e) {
            jsonResponse.put("message", "Database error: " + e.getMessage());
            response.setStatus(500);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }
}
