package com.api.scoreboard.tournament;

import com.api.util.Database;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.xml.transform.Result;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@WebServlet("/api/tournaments/teams")
public class TournamentTeamsServlet extends HttpServlet {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Object> jsonResponse = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        String id = request.getParameter("id");
        if (id == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            jsonResponse.put("error", "Missing required parameter 'id'");
            return;
        }
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = new Database().getConnection();
            String query = "SELECT" +
                    " t.id AS team_id," +
                    " t.name AS team_name," +
                    " GROUP_CONCAT(DISTINCT p.id ORDER BY p.id) AS player_ids," +
                    " GROUP_CONCAT(DISTINCT p.name ORDER BY p.id) AS player_names" +
                    " FROM matches m" +
                    " JOIN teams t ON t.id = m.team1_id OR t.id = m.team2_id" +
                    " JOIN team_players tp ON tp.team_id = t.id" +
                    " JOIN players p ON p.id = tp.player_id" +
                    " WHERE m.tournament_id = ?" +
                    " GROUP BY t.id, t.name;";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, id);
            rs = stmt.executeQuery();

            List<Map<String, Object>> teams = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> team = new HashMap<>();
                team.put("id", rs.getInt("team_id"));
                team.put("name", rs.getString("team_name"));
                String[] playerIds = rs.getString("player_ids").split(",");
                String[] playerNames = rs.getString("player_names").split(",");
                Map<String, Object>[] players = new Map[playerIds.length];
                for (int i = 0; i < playerIds.length; i++) {
                    Map<String, Object> player = new HashMap<>();
                    player.put("id", Integer.parseInt(playerIds[i]));
                    player.put("name", playerNames[i]);
                    players[i] = player;
                }
                team.put("players", players);
                teams.add(team);
            }

            if (teams.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                jsonResponse.put("error", "No teams found for the given tournament id");
                response.setContentType("application/json");
                response.getWriter().print(objectMapper.writeValueAsString(jsonResponse));
                return;
            }
            response.setContentType("application/json");
            response.getWriter().print(objectMapper.writeValueAsString(Collections.singletonMap("teams", teams)));
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonResponse.put("error", "Database error occurred" + e.getMessage());
        }  catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonResponse.put("error", "An error occurred" + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.out.println("Error while closing Database" + e.getMessage());
            }
        }
    }
}
