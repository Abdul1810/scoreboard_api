package com.api.scoreboard;

import com.api.util.Database;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.*;
import java.util.*;

import static com.api.util.Utils.validatePositiveIntegers;

@WebServlet("/update-stats")
public class StatsServlet extends HttpServlet {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> jsonResponse = new HashMap<>();

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String matchId = request.getParameter("id");
        Map<String, String> stats = objectMapper.readValue(request.getReader(), HashMap.class);
        if (matchId == null || matchId.trim().isEmpty()) {
            response.setStatus(400);
            jsonResponse.put("message", "Invalid match ID");
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
            int team1Balls = rs.getInt("team1_balls");
            int team2Balls = rs.getInt("team2_balls");
            String isCompleted = rs.getString("is_completed");
            String winner = rs.getString("winner");

            if ("true".equals(isCompleted)) {
                response.setStatus(400);
                jsonResponse.put("message", "Match already completed");
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                return;
            }

            Map<String, String> matchStats = new HashMap<>();
            matchStats.put("id", matchId);
            matchStats.put("current_batting", currentBattingTeam);
            matchStats.put("is_completed", isCompleted);
            matchStats.put("winner", winner);
            matchStats.put("team1_balls", String.valueOf(team1Balls));
            matchStats.put("team2_balls", String.valueOf(team2Balls));
            List<Integer> team1WicketsMap = objectMapper.readValue(rs.getString("team1_wickets_map"), List.class);
            List<Integer> team2WicketsMap = objectMapper.readValue(rs.getString("team2_wickets_map"), List.class);


            query = "SELECT * FROM player_stats WHERE match_id = ? ORDER BY player_id ASC;";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, Integer.parseInt(matchId));
            rs = stmt.executeQuery();

            Map<Integer, Integer> team1Scores = new HashMap<>();
            Map<Integer, Integer> team2Scores = new HashMap<>();
            Map<Integer, Integer> team1Wickets = new HashMap<>();
            Map<Integer, Integer> team2Wickets = new HashMap<>();

            while (rs.next()) {
                int playerId = rs.getInt("player_id");
                int runs = rs.getInt("runs");
                int wickets = rs.getInt("wickets");
                int teamId = rs.getInt("team_id");

                if (teamId == team1Id) {
                    team1Scores.put(playerId, runs);
                    team1Wickets.put(playerId, wickets);
                } else if (teamId == team2Id) {
                    team2Scores.put(playerId, runs);
                    team2Wickets.put(playerId, wickets);
                }
            }

            team1Scores = sortHashMapWithVals(team1Scores);
            team2Scores = sortHashMapWithVals(team2Scores);
            team1Wickets = sortHashMapWithVals(team1Wickets);
            team2Wickets = sortHashMapWithVals(team2Wickets);

            System.out.println("Team 1 Scores: ");
            Arrays.stream(team1Scores.keySet().toArray()).forEach(System.out::println);

            int currentPlayerId = -1;
            if ("team1".equals(currentBattingTeam)) {
                currentPlayerId = team2Wickets.values().stream().mapToInt(Integer::intValue).sum() + 1;
            } else {
                currentPlayerId = team1Wickets.values().stream().mapToInt(Integer::intValue).sum() + 1;
            }

            if (currentPlayerId == -1) {
                response.setStatus(400);
                jsonResponse.put("message", "Invalid player ID");
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                return;
            }
            System.out.println("Current Player ID: " + currentPlayerId);
            System.out.println("Current Batting Team: ");

            int currentPlayerIndex = currentPlayerId;
            currentPlayerId = "team1".equals(currentBattingTeam) ? (int) team1Scores.keySet().toArray()[currentPlayerId - 1] : (int) team2Scores.keySet().toArray()[currentPlayerId - 1];
            int currentPlayerOldScore = "team1".equals(currentBattingTeam) ? team1Scores.get(currentPlayerId) : team2Scores.get(currentPlayerId);
            System.out.println("Current Player ID: " + currentPlayerId);
            System.out.println("Current Player Old Score: " + currentPlayerOldScore);

            if ("true".equals(stats.get("out"))) {
                if ("team1".equals(currentBattingTeam)) {
                    System.out.println(stats);
                    System.out.println("Given score: " + stats.get("score"));
                    System.out.println("Current Player Old Score: " + currentPlayerOldScore);
                    team1Scores.put(currentPlayerId, Integer.parseInt(stats.get("score")) + currentPlayerOldScore);
                    matchStats.put("team1_wickets", String.valueOf(team2Wickets.values().stream().mapToInt(Integer::intValue).sum() + 1));
                    int bowlerIndex = updateWicketTaker(team2Wickets, stats.get("balls"));
                    System.out.println("Bowler Index: " + bowlerIndex);
                    System.out.println("Team 2 Wickets: ");
                    team2Wickets.forEach((k, v) -> System.out.println(k + " " + v));
                    if (bowlerIndex != -1) {
                        int bowlerId = (int) team2Wickets.keySet().toArray()[bowlerIndex - 1];
                        team2Wickets.put(bowlerId, team2Wickets.get(bowlerId) + 1);
                    }
                    team2WicketsMap.set(currentPlayerIndex - 1, bowlerIndex);
                } else {
                    team2Scores.put(currentPlayerId, Integer.parseInt(stats.get("score")) + currentPlayerOldScore);
                    matchStats.put("team2_wickets", String.valueOf(team1Wickets.values().stream().mapToInt(Integer::intValue).sum() + 1));
                    int bowlerIndex = updateWicketTaker(team1Wickets, stats.get("balls"));
                    if (bowlerIndex != -1) {
                        int bowlerId = (int) team1Wickets.keySet().toArray()[bowlerIndex - 1];
                        team1Wickets.put(bowlerId, team1Wickets.get(bowlerId) + 1);
                    }
                    team1WicketsMap.set(currentPlayerIndex - 1, bowlerIndex);
                }
            } else {
                if ("team1".equals(currentBattingTeam)) {
                    team1Scores.put(currentPlayerId, Integer.parseInt(stats.get("score")) + currentPlayerOldScore);
                } else {
                    team2Scores.put(currentPlayerId, Integer.parseInt(stats.get("score")) + currentPlayerOldScore);
                }
            }

            if (!validateStats(stats, currentBattingTeam, matchStats, response, currentPlayerOldScore)) return;
            matchStats.put("team1_wickets", String.valueOf(team1Wickets.values().stream().mapToInt(Integer::intValue).sum()));
            matchStats.put("team2_wickets", String.valueOf(team2Wickets.values().stream().mapToInt(Integer::intValue).sum()));

            System.out.println(matchStats);
            if ("team1".equals(currentBattingTeam)) {
                if (Integer.parseInt(stats.get("balls")) < team1Balls) {
                    response.setStatus(400);
                    jsonResponse.put("message", "Invalid Data team1");
                    response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                    return;
                }
                matchStats.put("team1_balls", stats.get("balls"));
                if (Integer.parseInt(matchStats.get("team1_balls")) == 120 || Integer.parseInt(matchStats.get("team2_wickets")) == 10 || Integer.parseInt(stats.get("balls")) == 120) {
                    matchStats.put("current_batting", "team2");
                }
            } else if ("team2".equals(currentBattingTeam)) {
                if (Integer.parseInt(stats.get("balls")) < team2Balls) {
                    response.setStatus(400);
                    jsonResponse.put("message", "Invalid Data team2");
                    response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                    return;
                }
                matchStats.put("team2_balls", stats.get("balls"));
                int team1Score = team1Scores.values().stream().mapToInt(Integer::intValue).sum();
                int team2Score = team2Scores.values().stream().mapToInt(Integer::intValue).sum();
                if (team2Score > team1Score) {
                    matchStats.put("winner", "team2");
                    matchStats.put("is_completed", "true");
                }
                if ("120".equals(matchStats.get("team2_balls")) || "10".equals(matchStats.get("team1_wickets"))) {
                    findWinner(matchStats, team1Score, team2Score);
                }
            }
            System.out.println(matchStats);

            updateMatchStats(matchStats, team1Scores, team2Scores, team1Wickets, team2Wickets, team1WicketsMap, team2WicketsMap);
            Map<String, Object> matchData = new HashMap<>();
            matchData.put("team1_score", team1Scores.values().stream().mapToInt(Integer::intValue).sum());
            matchData.put("team2_score", team2Scores.values().stream().mapToInt(Integer::intValue).sum());
            matchData.put("team1_wickets", matchStats.get("team1_wickets"));
            matchData.put("team2_wickets", matchStats.get("team2_wickets"));
            matchData.put("team1_balls", matchStats.get("team1_balls"));
            matchData.put("team2_balls", matchStats.get("team2_balls"));
            matchData.put("team1_runs", team1Scores);
            matchData.put("team2_runs", team2Scores);
            matchData.put("team1_outs", team1Wickets);
            matchData.put("team2_outs", team2Wickets);
            matchData.put("team1_wickets_map", team1WicketsMap);
            matchData.put("team2_wickets_map", team2WicketsMap);
            matchData.put("current_batting", matchStats.get("current_batting"));
            matchData.put("is_completed", matchStats.get("is_completed"));
            matchData.put("winner", matchStats.get("winner"));

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

    private boolean validateStats(Map<String, String> stats, String team, Map<String, String> matchStats, HttpServletResponse response, int currentPlayerOldScore) throws IOException {
        if (!validatePositiveIntegers(stats.get("score"), stats.get("balls"))) {
            response.setStatus(400);
            jsonResponse.put("message", "Invalid Data scores " + team);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            return false;
        }
        System.out.println(stats.get("score"));
        System.out.println(stats.get("balls"));
        if (Integer.parseInt(stats.get("score")) < currentPlayerOldScore || Integer.parseInt(stats.get("balls")) < Integer.parseInt(matchStats.get(team + "_balls"))) {
            response.setStatus(400);
            jsonResponse.put("message", "Corrupted Data " + team);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            return false;
        }

        if (Integer.parseInt(stats.get("balls")) > 120) {
            response.setStatus(400);
            jsonResponse.put("message", "Invalid Data balls " + team);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            return false;
        }

        return true;
    }

    private void findWinner(Map<String, String> matchStats, int team1Score, int team2Score) {
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

    private void updateMatchStats(Map<String, String> matchStats, Map<Integer, Integer> team1Scores, Map<Integer, Integer> team2Scores, Map<Integer, Integer> team1Wickets, Map<Integer, Integer> team2Wickets, List<Integer> team1WicketsMap, List<Integer> team2WicketsMap) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = Database.getConnection();
            String query = "UPDATE matches SET current_batting = ?, is_completed = ?, winner = ?, team1_balls = ?, team2_balls = ?, " +
                    "team1_wickets_map = ?, team2_wickets_map = ? WHERE id = ?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, matchStats.get("current_batting"));
            stmt.setString(2, matchStats.get("is_completed"));
            stmt.setString(3, matchStats.get("winner"));
            stmt.setInt(4, Integer.parseInt(matchStats.get("team1_balls")));
            stmt.setInt(5, Integer.parseInt(matchStats.get("team2_balls")));
            stmt.setString(6, objectMapper.writeValueAsString(team1WicketsMap));
            stmt.setString(7, objectMapper.writeValueAsString(team2WicketsMap));
            stmt.setInt(8, Integer.parseInt(matchStats.get("id")));
            stmt.executeUpdate();

            query = "UPDATE player_stats SET runs = ?, wickets = ? WHERE match_id = ? AND player_id = ?";
            stmt = conn.prepareStatement(query);

            for (Map.Entry<Integer, Integer> entry : team1Scores.entrySet()) {
                stmt.setInt(1, entry.getValue());
                stmt.setInt(2, team1Wickets.get(entry.getKey()));
                stmt.setInt(3, Integer.parseInt(matchStats.get("id")));
                stmt.setInt(4, entry.getKey());
                stmt.addBatch();
            }

            for (Map.Entry<Integer, Integer> entry : team2Scores.entrySet()) {
                stmt.setInt(1, entry.getValue());
                stmt.setInt(2, team2Wickets.get(entry.getKey()));
                stmt.setInt(3, Integer.parseInt(matchStats.get("id")));
                stmt.setInt(4, entry.getKey());
                stmt.addBatch();
            }

            stmt.executeBatch();
        } catch (SQLException e) {
            System.out.println("Error updating match stats: " + e.getMessage());
        } catch (JsonProcessingException e) {
            System.out.println("Error processing JSON: " + e.getMessage());
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }

    private int updateWicketTaker(Map<Integer, Integer> wickets, String balls) {
        int ballCount = Integer.parseInt(balls);
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

    private boolean validatePositiveIntegers(String... values) {
        for (String value : values) {
            try {
                int intValue = Integer.parseInt(value);
                if (intValue < 0) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    private Map<Integer, Integer> sortHashMapWithVals(Map<Integer, Integer> map) {
        List<Map.Entry<Integer, Integer>> list = new ArrayList<>(map.entrySet());
        list.sort(Comparator.comparingInt(Map.Entry::getKey));
        Map<Integer, Integer> sortedMap = new LinkedHashMap<>();

        for (Map.Entry<Integer, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
            System.out.println(entry.getKey() + " " + entry.getValue());
        }

        return sortedMap;
    }
}