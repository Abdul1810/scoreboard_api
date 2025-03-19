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

@WebServlet("/api/stats/change-batsman")
public class ChangeBatsman extends HttpServlet {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> jsonResponse = new HashMap<>();

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String matchId = request.getParameter("id");
        Map<String, String> requestData = objectMapper.readValue(request.getReader(), HashMap.class);
        String newPlayerIndex = requestData.getOrDefault("player", "");
        String passivePlayerIndex = requestData.getOrDefault("passive", "");

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = new Database().getConnection();
            stmt = conn.prepareStatement("SELECT * FROM matches WHERE id = ?");
            stmt.setString(1, matchId);
            rs = stmt.executeQuery();

            if (newPlayerIndex == "" && passivePlayerIndex != "") {
                if (rs.next()) {
                    int passiveBatsmanIndex = rs.getInt("passive_batsman_index");
                    if (passiveBatsmanIndex != -1) {
                        jsonResponse.put("error", "Passive batsman already set");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    } else {
                        int teamId = rs.getString("current_batting").equals("team1") ? rs.getInt("team1_id") : rs.getInt("team2_id");
                        String query = "SELECT * FROM player_stats WHERE team_id = ? AND match_id = ? ORDER BY id";
                        stmt = conn.prepareStatement(query);
                        stmt.setInt(1, teamId);
                        stmt.setString(2, matchId);
                        rs = stmt.executeQuery();

                        List<Boolean> playerStatus = new ArrayList<>();
                        while (rs.next()) {
                            playerStatus.add(rs.getInt("wicketer_id") > 0);
                        }

                        if (playerStatus.get((Integer.parseInt(passivePlayerIndex) - 1))) {
                            jsonResponse.put("error", "Passive player already out");
                            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            return;
                        }

                        stmt = conn.prepareStatement("UPDATE matches SET passive_batsman_index = ? WHERE id = ?");
                        stmt.setInt(1, Integer.parseInt(passivePlayerIndex));
                        stmt.setString(2, matchId);
                        stmt.executeUpdate();

                        stmt = conn.prepareStatement("SELECT * FROM team_order WHERE match_id = ? AND team_id = ?");
                        stmt.setString(1, matchId);
                        stmt.setInt(2, teamId);
                        rs = stmt.executeQuery();
                        if (rs.next()) {
                            String battingOrder = rs.getString("batting_order");
                            List<Integer> battingOrderList = objectMapper.readValue(battingOrder, List.class);
                            int index = battingOrderList.indexOf(-1);
                            if (index == -1) {
                                jsonResponse.put("error", "No empty slot in batting order");
                                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                                return;
                            }
                            battingOrderList.set(index, Integer.parseInt(passivePlayerIndex));

                            stmt = conn.prepareStatement("UPDATE team_order SET batting_order = ? WHERE match_id = ? AND team_id = ?");
                            stmt.setString(1, battingOrderList.toString());
                            stmt.setString(2, matchId);
                            stmt.setInt(3, teamId);
                            stmt.executeUpdate();
                        }

                        jsonResponse.put("message", "Batsman updated successfully");
                        response.setStatus(HttpServletResponse.SC_OK);
                        StatsListener.fireStatsUpdate(matchId);
                    }
                } else {
                    jsonResponse.put("error", "Match not found");
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                response
                        .getWriter()
                        .write(objectMapper.writeValueAsString(jsonResponse));
                return;
            }

            if (rs.next()) {
                int activeBatsmanIndex = rs.getInt("active_batsman_index");
                if (activeBatsmanIndex != -1) {
                    jsonResponse.put("error", "Active batsman already set");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                } else {
                    int teamId = rs.getString("current_batting").equals("team1") ? rs.getInt("team1_id") : rs.getInt("team2_id");
                    String query = "SELECT * FROM player_stats WHERE team_id = ? AND match_id = ? ORDER BY id";
                    stmt = conn.prepareStatement(query);
                    stmt.setInt(1, teamId);
                    stmt.setString(2, matchId);
                    rs = stmt.executeQuery();

                    List<Boolean> playerStatus = new ArrayList<>();
                    while (rs.next()) {
                        playerStatus.add(rs.getInt("wicketer_id") > 0);
                    }

                    if (playerStatus.get((Integer.parseInt(newPlayerIndex) - 1))) {
                        jsonResponse.put("error", "Player already out");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    }

                    if (passivePlayerIndex == null || passivePlayerIndex.equals("")) {
                        stmt = conn.prepareStatement("UPDATE matches SET active_batsman_index = ? WHERE id = ?");
                        stmt.setInt(1, Integer.parseInt(newPlayerIndex));
                        stmt.setString(2, matchId);
                        stmt.executeUpdate();
                    } else {
                        if (playerStatus.get((Integer.parseInt(passivePlayerIndex) - 1))) {
                            jsonResponse.put("error", "Passive player already out");
                            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            return;
                        }

                        stmt = conn.prepareStatement("UPDATE matches SET active_batsman_index = ?, passive_batsman_index = ? WHERE id = ?");
                        stmt.setInt(1, Integer.parseInt(newPlayerIndex));
                        stmt.setInt(2, Integer.parseInt(passivePlayerIndex));
                        stmt.setString(3, matchId);
                        stmt.executeUpdate();
                    }

                    stmt = conn.prepareStatement("SELECT * FROM team_order WHERE match_id = ? AND team_id = ?");
                    stmt.setString(1, matchId);
                    stmt.setInt(2, teamId);
                    rs = stmt.executeQuery();
                    if (rs.next()) {
                        String battingOrder = rs.getString("batting_order");
                        List<Integer> battingOrderList = objectMapper.readValue(battingOrder, List.class);
                        int index = battingOrderList.indexOf(-1);
                        if (index == -1) {
                            jsonResponse.put("error", "No empty slot in batting order");
                            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            return;
                        }
                        battingOrderList.set(index, Integer.parseInt(newPlayerIndex));

                        if (passivePlayerIndex != null && !passivePlayerIndex.equals("")) {
                            int passiveIndex = battingOrderList.indexOf(-1);
                            if (passiveIndex == -1) {
                                jsonResponse.put("error", "Passive player not found in batting order");
                                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                                return;
                            }
                            System.out.println(passiveIndex);
                            battingOrderList.set(passiveIndex, Integer.parseInt(passivePlayerIndex));
                        }

                        stmt = conn.prepareStatement("UPDATE team_order SET batting_order = ? WHERE match_id = ? AND team_id = ?");
                        stmt.setString(1, battingOrderList.toString());
                        stmt.setString(2, matchId);
                        stmt.setInt(3, teamId);
                        stmt.executeUpdate();
                    }

                    jsonResponse.put("message", "Batsman updated successfully");
                    response.setStatus(HttpServletResponse.SC_OK);
                    StatsListener.fireStatsUpdate(matchId);
                }
            } else {
                jsonResponse.put("error", "Match not found");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        } catch (SQLException e) {
            System.out.println("Error updating batsman: " + e.getMessage());
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
