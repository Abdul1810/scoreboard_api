package com.api.scoreboard.tournament;

import com.api.util.Database;
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

@WebServlet("/api/tournaments/total-score")
public class TournamentPlayerRuns extends HttpServlet {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Object> jsonResponse = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        String id = request.getParameter("id");
        String playerId = request.getParameter("player_id");
        if (id == null || playerId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            jsonResponse.put("error", "Missing required parameter 'id' or 'player_id'");
            return;
        }
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = new Database().getConnection();
            String query = "SELECT" +
                    " ps.player_id," +
                    " p.name AS player_name," +
                    " t.id AS tournament_id," +
                    " t.name AS tournament_name," +
                    " SUM(ps.runs) AS total_runs," +
                    " SUM(ps.balls) AS total_balls," +
                    " SUM(CASE WHEN ps.balls > 0 THEN 1 ELSE 0 END) AS matches_played" +
                    " FROM player_stats ps" +
                    " JOIN matches m ON ps.match_id = m.id" +
                    " JOIN tournaments t ON m.tournament_id = t.id" +
                    " JOIN players p ON ps.player_id = p.id" +
                    " WHERE t.id = ? AND ps.player_id = ?" +
                    " GROUP BY ps.player_id, t.id;";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, id);
            stmt.setString(2, playerId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                jsonResponse.put("player_id", rs.getInt("player_id"));
                jsonResponse.put("player_name", rs.getString("player_name"));
                jsonResponse.put("tournament_id", rs.getInt("tournament_id"));
                jsonResponse.put("tournament_name", rs.getString("tournament_name"));
                jsonResponse.put("total_runs", rs.getInt("total_runs"));
                jsonResponse.put("total_balls", rs.getInt("total_balls"));
                jsonResponse.put("matches_played", rs.getInt("matches_played"));
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                jsonResponse.put("error", "No data found for the given tournament and player");
            }

            response.setContentType("application/json");
            response.getWriter().print(objectMapper.writeValueAsString(jsonResponse));
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
