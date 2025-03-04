package com.api.scoreboard;

import com.api.util.Database;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.*;
import java.util.*;

@WebServlet("/update-stats")
public class StatsServlet extends HttpServlet {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> jsonResponse = new HashMap<>();

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String matchId = request.getParameter("id");
//        Map<String, String> stats = objectMapper.readValue(request.getReader(), HashMap.class);
        Map<String, String> requestData = objectMapper.readValue(request.getReader(), HashMap.class);
        String updateRequest = requestData.getOrDefault("update", "");
        if (matchId == null || matchId.trim().isEmpty()) {
            response.setStatus(400);
            jsonResponse.put("message", "Invalid match ID");
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            return;
        }

        if (updateRequest.isEmpty()) {
            response.setStatus(400);
            jsonResponse.put("message", "Invalid update request");
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            return;
        }

        if (!Arrays.asList("1", "2", "4", "6", "out").contains(updateRequest)) {
            response.setStatus(400);
            jsonResponse.put("message", "Invalid update request");
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            return;
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = Database.getConnection();
            String query = "SELECT * FROM matches WHERE id = ?";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, Integer.parseInt(matchId));
            rs = stmt.executeQuery();

            if (!rs.next()) {
                response.setStatus(404);
                jsonResponse.put("message", "Match not found");
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                return;
            }

            int team1Id = rs.getInt("team1_id");
            int team2Id = rs.getInt("team2_id");
            String currentBattingTeam = rs.getString("current_batting");
            String isCompleted = rs.getString("is_completed");
            String winner = rs.getString("winner");
            int activeBatsmanIndex = rs.getInt("active_batsman_index");
            int passiveBatsmanIndex = rs.getInt("passive_batsman_index");

            if ("true".equals(isCompleted)) {
                response.setStatus(400);
                jsonResponse.put("message", "Match already completed");
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                return;
            }

            Map<String, String> matchStats = new HashMap<>();
            matchStats.put("id", matchId);
            matchStats.put("current_batting", currentBattingTeam);
            matchStats.put("active_batsman_index", String.valueOf(rs.getInt("active_batsman_index")));
            matchStats.put("passive_batsman_index", String.valueOf(rs.getInt("passive_batsman_index")));
            matchStats.put("is_completed", isCompleted);
            matchStats.put("winner", winner);

            query = "SELECT ps.player_id, ps.runs, ps.wickets, ps.team_id, ps.wicketer_id, ps.balls, " +
                    "w.name AS wicketer_name," +
                    "p.name AS player_name " +
                    "FROM player_stats ps " +
                    "JOIN players p ON ps.player_id = p.id " +
                    "LEFT JOIN players w ON ps.wicketer_id = w.id " +
                    "WHERE ps.match_id = ? AND ps.team_id IN (?, ?)";

            stmt = conn.prepareStatement(query);
            stmt.setString(1, matchId);
            stmt.setInt(2, team1Id);
            stmt.setInt(3, team2Id);
            rs = stmt.executeQuery();

            Map<Integer, Integer> team1Scores = new HashMap<>();
            Map<Integer, Integer> team2Scores = new HashMap<>();
            Map<Integer, Integer> team1Wickets = new HashMap<>();
            Map<Integer, Integer> team2Wickets = new HashMap<>();
            Map<Integer, String> team1Players = new HashMap<>();
            Map<Integer, String> team2Players = new HashMap<>();

            List<String> team1WicketsMap = new ArrayList<>();
            List<String> team2WicketsMap = new ArrayList<>();
            List<Integer> team1BallsMap = new ArrayList<>();
            List<Integer> team2BallsMap = new ArrayList<>();
            int team1_balls = 0;
            int team2_balls = 0;
            int wicketer_id = -1;

            while (rs.next()) {
                int playerId = rs.getInt("player_id");
                int runs = rs.getInt("runs");
                int balls = rs.getInt("balls");
                int wickets = rs.getInt("wickets");
                int teamId = rs.getInt("team_id");
                String wicketerName = rs.getString("wicketer_name");

                if (teamId == team1Id) {
                    team1Scores.put(playerId, runs);
                    team1Wickets.put(playerId, wickets);
                    team1_balls += balls;
                    team1WicketsMap.add(wicketerName);
                    team1Players.put(playerId, rs.getString("player_name"));
                    team1BallsMap.add(balls);
                } else if (teamId == team2Id) {
                    team2Scores.put(playerId, runs);
                    team2Wickets.put(playerId, wickets);
                    team2_balls += balls;
                    team2WicketsMap.add(wicketerName);
                    team2Players.put(playerId, rs.getString("player_name"));
                    team2BallsMap.add(balls);
                }
            }

            team1Scores = sortHashMapWithVals(team1Scores);
            team2Scores = sortHashMapWithVals(team2Scores);
            team1Wickets = sortHashMapWithVals(team1Wickets);
            team2Wickets = sortHashMapWithVals(team2Wickets);

            int currentPlayerId = activeBatsmanIndex;
            if (currentPlayerId == -1) {
                response.setStatus(400);
                jsonResponse.put("message", "Invalid player ID");
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                return;
            }

            int currentPlayerIndex = currentPlayerId;
            currentPlayerId = "team1".equals(currentBattingTeam) ? (int) team1Scores.keySet().toArray()[currentPlayerId - 1] : (int) team2Scores.keySet().toArray()[currentPlayerId - 1];
            int currentPlayerOldScore = "team1".equals(currentBattingTeam) ? team1Scores.get(currentPlayerId) : team2Scores.get(currentPlayerId);

            if (updateRequest.equals("out")) {
                if ("team1".equals(currentBattingTeam)) {
                    int bowlerIndex = updateWicketTaker(team1_balls);
                    if (bowlerIndex != -1) {
                        int bowlerId = (int) team2Wickets.keySet().toArray()[bowlerIndex - 1];
                        wicketer_id = bowlerId;
                        team2Wickets.put(bowlerId, team2Wickets.getOrDefault(bowlerIndex, 0) + 1);
                        team1WicketsMap.set(activeBatsmanIndex - 1, team2Players.get(bowlerId));
                        for (int i = 0; i < team1WicketsMap.size(); i++) {
                            if (team1WicketsMap.get(i) == null && i != passiveBatsmanIndex - 1) {
                                activeBatsmanIndex = i+1;
                                break;
                            }
                        }
                    }
                } else {
                    int bowlerIndex = updateWicketTaker(team2_balls);
                    if (bowlerIndex != -1) {
                        int bowlerId = (int) team1Wickets.keySet().toArray()[bowlerIndex - 1];
                        wicketer_id = bowlerId;
                        team1Wickets.put(bowlerId, team1Wickets.getOrDefault(bowlerIndex, 0) + 1);
                        team2WicketsMap.set(activeBatsmanIndex - 1, team1Players.get(bowlerId));
                        for (int i = 0; i < team2WicketsMap.size(); i++) {
                            if (team2WicketsMap.get(i) == null && i != passiveBatsmanIndex - 1) {
                                activeBatsmanIndex = i+1;
                                break;
                            }
                        }
                    }
                }
            } else {
                if ("team1".equals(currentBattingTeam)) {
                    team1Scores.put(currentPlayerId, Integer.parseInt(updateRequest) + currentPlayerOldScore);
                    if ("1".equals(updateRequest)) {
                        int temp = activeBatsmanIndex;
                        activeBatsmanIndex = passiveBatsmanIndex;
                        passiveBatsmanIndex = temp;
                    }
                } else {
                    team2Scores.put(currentPlayerId, Integer.parseInt(updateRequest) + currentPlayerOldScore);
                    if ("1".equals(updateRequest)) {
                        int temp = activeBatsmanIndex;
                        activeBatsmanIndex = passiveBatsmanIndex;
                        passiveBatsmanIndex = temp;
                    }
                }
            }

            matchStats.put("team1_wickets", String.valueOf(team2Wickets.values().stream().mapToInt(Integer::intValue).sum()));
            matchStats.put("team2_wickets", String.valueOf(team1Wickets.values().stream().mapToInt(Integer::intValue).sum()));

            if ("team1".equals(currentBattingTeam)) {
                team1_balls++;
                team1BallsMap.set(activeBatsmanIndex - 1, team1BallsMap.get(activeBatsmanIndex - 1) + 1);
                if (team1_balls % 6 == 0) {
                    int temp = activeBatsmanIndex;
                    activeBatsmanIndex = passiveBatsmanIndex;
                    passiveBatsmanIndex = temp;
                }
            } else {
                team2_balls++;
                team2BallsMap.set(activeBatsmanIndex - 1, team2BallsMap.get(activeBatsmanIndex - 1) + 1);
                if (team2_balls % 6 == 0) {
                    int temp = activeBatsmanIndex;
                    activeBatsmanIndex = passiveBatsmanIndex;
                    passiveBatsmanIndex = temp;
                }
            }

            if ("team1".equals(currentBattingTeam)) {
                if (team2Wickets.values().stream().mapToInt(Integer::intValue).sum() == 10 || team1_balls == 120) {
                    matchStats.put("current_batting", "team2");
                    activeBatsmanIndex = 1;
                    passiveBatsmanIndex = 2;
                }
            } else if ("team2".equals(currentBattingTeam)) {
                int team1Score = team1Scores.values().stream().mapToInt(Integer::intValue).sum();
                int team2Score = team2Scores.values().stream().mapToInt(Integer::intValue).sum();
                if (team2Score > team1Score) {
                    matchStats.put("winner", "team2");
                    matchStats.put("is_completed", "true");
                }
                if (team2_balls == 120 || team1Wickets.values().stream().mapToInt(Integer::intValue).sum() == 10) {
                    if (team1Score > team2Score) {
                        matchStats.put("winner", "team1");
                        matchStats.put("is_completed", "true");
                    } else if (team1Score < team2Score) {
                        matchStats.put("winner", "team2");
                        matchStats.put("is_completed", "true");
                    } else {
                        matchStats.put("winner", "tie");
                        matchStats.put("is_completed", "true");
                    }
                }
            }

            updateMatchStats(matchStats, team1Scores, team2Scores, team1Wickets, team2Wickets, activeBatsmanIndex, passiveBatsmanIndex, wicketer_id, team1BallsMap, team2BallsMap, currentPlayerId);
            Map<String, Object> matchData = new HashMap<>();

            matchData.put("id", matchStats.get("id"));
            matchData.put("current_batting", matchStats.get("current_batting"));
            matchData.put("is_completed", matchStats.get("is_completed"));
            matchData.put("winner", matchStats.get("winner"));
            matchData.put("active_batsman_index", activeBatsmanIndex);
            matchData.put("passive_batsman_index", passiveBatsmanIndex);
            matchData.put("team1_score", team1Scores.values().stream().mapToInt(Integer::intValue).sum());
            matchData.put("team2_score", team2Scores.values().stream().mapToInt(Integer::intValue).sum());
            matchData.put("team1_wickets", team1Wickets.values().stream().mapToInt(Integer::intValue).sum());
            matchData.put("team2_wickets", team2Wickets.values().stream().mapToInt(Integer::intValue).sum());
            matchData.put("team1_runs", team1Scores);
            matchData.put("team2_runs", team2Scores);
            matchData.put("team1_outs", team1Wickets);
            matchData.put("team2_outs", team2Wickets);
            matchData.put("team1_balls", team1_balls);
            matchData.put("team2_balls", team2_balls);
            matchData.put("team1_wickets_map", team1WicketsMap);
            matchData.put("team2_wickets_map", team2WicketsMap);
            matchData.put("team1_balls_map", team1BallsMap);
            matchData.put("team2_balls_map", team2BallsMap);

            StatsListener.fireStatsUpdate(matchId, objectMapper.writeValueAsString(matchData));
            response.setStatus(200);
            jsonResponse.put("message", "success");
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } catch (SQLException e) {
            jsonResponse.put("message", "Database error: " + e.getMessage());
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

    private void updateMatchStats(Map<String, String> matchStats, Map<Integer, Integer> team1Scores, Map<Integer, Integer> team2Scores, Map<Integer, Integer> team1Wickets, Map<Integer, Integer> team2Wickets, int activeBatsmanIndex, int passiveBatsmanIndex, int wicketer_id, List<Integer> team1BallsMap, List<Integer> team2BallsMap, int currentPlayerId) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = Database.getConnection();
            String query = "UPDATE matches SET current_batting = ?, is_completed = ?, winner = ?, active_batsman_index = ?, passive_batsman_index = ? WHERE id = ?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, matchStats.get("current_batting"));
            stmt.setString(2, matchStats.get("is_completed"));
            stmt.setString(3, matchStats.get("winner"));
            stmt.setInt(4, activeBatsmanIndex);
            stmt.setInt(5, passiveBatsmanIndex);
            stmt.setInt(6, Integer.parseInt(matchStats.get("id")));
            stmt.executeUpdate();

            query = "UPDATE player_stats SET runs = ?, balls = ?, wickets = ? WHERE match_id = ? AND player_id = ?";

            stmt = conn.prepareStatement(query);
            int i = 0;
            for (Map.Entry<Integer, Integer> entry : team1Scores.entrySet()) {
                stmt.setInt(1, entry.getValue());
                stmt.setInt(2, team1BallsMap.get(i));
                stmt.setInt(3, team1Wickets.get(entry.getKey()));
                stmt.setInt(4, Integer.parseInt(matchStats.get("id")));
                stmt.setInt(5, entry.getKey());
                stmt.addBatch();
                i++;
            }

            i = 0;
            for (Map.Entry<Integer, Integer> entry : team2Scores.entrySet()) {
                stmt.setInt(1, entry.getValue());
                stmt.setInt(2, team2BallsMap.get(i));
                stmt.setInt(3, team2Wickets.get(entry.getKey()));
                stmt.setInt(4, Integer.parseInt(matchStats.get("id")));
                stmt.setInt(5, entry.getKey());
                stmt.addBatch();
                i++;
            }

            stmt.executeBatch();

            if (wicketer_id != -1) {
                query = "UPDATE player_stats SET wicketer_id = ? WHERE match_id = ? AND player_id = ?";
                stmt = conn.prepareStatement(query);
                stmt.setInt(1, wicketer_id);
                stmt.setInt(2, Integer.parseInt(matchStats.get("id")));
                stmt.setInt(3, currentPlayerId);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            System.out.println("Error updating match stats: " + e.getMessage());
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }

    private int updateWicketTaker(int ballCount) {
        int bowlerIndex;

        if (ballCount <= 66) {
            int over = ballCount / 6;
            int ball = ballCount % 6;
            bowlerIndex = over + (ball > 0 ? 1 : 0);
        } else {
            ballCount -= 66;
            int over = ballCount / 6;
            int ball = ballCount % 6;
            bowlerIndex = over + (ball > 0 ? 1 : 0);
        }

        if (bowlerIndex < 0 || bowlerIndex > 11) {
            return -1;
        }

        if (bowlerIndex == 0) {
            bowlerIndex = 1;
        }

        return bowlerIndex;
    }

    private Map<Integer, Integer> sortHashMapWithVals(Map<Integer, Integer> map) {
        List<Map.Entry<Integer, Integer>> list = new ArrayList<>(map.entrySet());
        list.sort(Comparator.comparingInt(Map.Entry::getKey));
        Map<Integer, Integer> sortedMap = new LinkedHashMap<>();

        for (Map.Entry<Integer, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }
}