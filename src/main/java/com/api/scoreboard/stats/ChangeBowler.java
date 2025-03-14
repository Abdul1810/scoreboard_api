package com.api.scoreboard.stats;

import com.api.scoreboard.commons.Match;
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

@WebServlet("/api/change-bowler")
public class ChangeBowler extends HttpServlet {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> jsonResponse = new HashMap<>();

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String matchId = request.getParameter("id");
        Map<String, String> requestData = objectMapper.readValue(request.getReader(), HashMap.class);
        String newPlayerIndex = requestData.getOrDefault("player", "");

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = Database.getConnection();
            stmt = conn.prepareStatement("SELECT * FROM matches WHERE id = ?");
            stmt.setString(1, matchId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                int activeBowlerIndex = rs.getInt("active_bowler_index");
                if (activeBowlerIndex != -1) {
                    jsonResponse.put("error", "Active bowler already set");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                } else {
                    int teamId = rs.getString("current_batting").equals("team1") ? rs.getInt("team2_id") : rs.getInt("team1_id");
                    stmt = conn.prepareStatement("UPDATE matches SET active_bowler_index = ? WHERE id = ?");
                    stmt.setInt(1, Integer.parseInt(newPlayerIndex));
                    stmt.setString(2, matchId);
                    stmt.executeUpdate();

                    stmt = conn.prepareStatement("SELECT * FROM team_order WHERE match_id = ? AND team_id = ?");
                    stmt.setString(1, matchId);
                    stmt.setInt(2, teamId);
                    rs = stmt.executeQuery();
                    if (rs.next()) {
                        String bowlingOrder = rs.getString("bowling_order");
                        List<Integer> bowlingOrderList = objectMapper.readValue(bowlingOrder, List.class);
                        int index = bowlingOrderList.indexOf(-1);
                        if (index == -1) {
                            jsonResponse.put("error", "No empty slot in bowling order");
                            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            return;
                        }
                        bowlingOrderList.set(index, Integer.parseInt(newPlayerIndex));

                        stmt = conn.prepareStatement("UPDATE team_order SET bowling_order = ? WHERE match_id = ? AND team_id = ?");
                        stmt.setString(1, bowlingOrderList.toString());
                        stmt.setString(2, matchId);
                        stmt.setInt(3, teamId);
                        stmt.executeUpdate();
                    }

                    jsonResponse.put("message", "Bowler updated successfully");
                    response.setStatus(HttpServletResponse.SC_OK);
                    StatsListener.fireStatsUpdate(matchId);
                }
            } else {
                jsonResponse.put("error", "Match not found");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        } catch (SQLException e) {
            System.out.println("Error updating bowler: " + e.getMessage());
            jsonResponse.put("error", "Error updating batsman");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }

        response.setContentType("application/json");
        response
                .getWriter()
                .write(objectMapper.writeValueAsString(jsonResponse));
    }
}
