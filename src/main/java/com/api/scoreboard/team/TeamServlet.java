package com.api.scoreboard.team;

import com.api.scoreboard.StatsListener;
import com.api.scoreboard.match.MatchListener;
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
import java.util.*;

@WebServlet("/api/teams")
public class TeamServlet extends HttpServlet {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> jsonResponse = new HashMap<>();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        /*
        db structure
        name: string
        player1: string
        player2: string
        player3: string
        player4: string
        player5: string
        player6: string
        player7: string
        player8: string
        player9: string
        player10: string
        player11: string
         */
        /*
        request body
        name: string
        players: string[]
         */
        // validate input
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
                jsonResponse.put("message", "Invalid player name in " + i);
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

        PreparedStatement insertStmt = null;
        try {
            String insertQuery = "INSERT INTO teams (name, player1, player2, player3, player4, player5, player6, player7, player8, player9, player10, player11) VALUES " + "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            insertStmt = conn.prepareStatement(insertQuery);
            insertStmt.setString(1, name.get());
            for (int i = 0; i < 11; i++) {
                insertStmt.setString(i + 2, players.get(i));
            }
            insertStmt.executeUpdate();

            TeamListener.fireTeamsUpdate();
            jsonResponse.put("message", "Team created successfully");
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } catch (Exception e) {
            jsonResponse.put("message", "Database error: " + e.getMessage());
            response.setStatus(500);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } finally {
            try {
                if (insertStmt != null) insertStmt.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = Database.getConnection();

            String query = "SELECT * FROM teams";
            stmt = conn.prepareStatement(query);
            rs = stmt.executeQuery();

            List<Map<String, Object>> teams = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> team = new HashMap<>();
                team.put("id", rs.getInt("id"));
                team.put("name", rs.getString("name"));
                team.put("players", new ArrayList<>());
                for (int i = 1; i <= 11; i++) {
                    ((List<String>) team.get("players")).add(rs.getString("player" + i));
                }
                teams.add(team);
            }

            response.getWriter().write(objectMapper.writeValueAsString(teams));
        } catch (Exception e) {
            jsonResponse.put("message", "Database error: " + e.getMessage());
            response.setStatus(500);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
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

        try {
            conn = Database.getConnection();

            String query = "DELETE FROM teams WHERE id = ?";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, Integer.parseInt(id));
            stmt.executeUpdate();

            List<Integer> matchIds = new ArrayList<>();
            query = "SELECT id FROM matches WHERE team1_id = ? OR team2_id = ?";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, Integer.parseInt(id));
            stmt.setInt(2, Integer.parseInt(id));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                matchIds.add(rs.getInt("id"));
            }

            query = "DELETE FROM matches WHERE team1_id = ? OR team2_id = ?";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, Integer.parseInt(id));
            stmt.setInt(2, Integer.parseInt(id));
            stmt.executeUpdate();

            query = "DELETE FROM match_stats WHERE match_id = ?";
            for (int matchId : matchIds) {
                stmt = conn.prepareStatement(query);
                stmt.setInt(1, matchId);
                stmt.executeUpdate();
            }

            MatchListener.fireMatchesUpdate();
            TeamListener.fireTeamsUpdate();
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
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }
}
