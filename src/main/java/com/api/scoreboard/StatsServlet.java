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

            String query = "SELECT * FROM match_stats WHERE match_id = ?";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, Integer.parseInt(matchId));
            rs = stmt.executeQuery();

            if (!rs.next()) {
                response.setStatus(404);
                jsonResponse.put("message", "Match not found");
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                return;
            }

            List<Integer> team1_scores = new ArrayList<>();
            List<Integer> team2_scores = new ArrayList<>();
            List<Integer> team1_wickets = new ArrayList<>();
            List<Integer> team2_wickets = new ArrayList<>();

            for (int i = 1; i <= 11; i++) {
                team1_scores.add(rs.getInt("team1_player" + i + "_runs"));
                team2_scores.add(rs.getInt("team2_player" + i + "_runs"));
            }

            for (int i = 1; i <= 11; i++) {
                team1_wickets.add(rs.getInt("team1_player" + i + "_wickets"));
                team2_wickets.add(rs.getInt("team2_player" + i + "_wickets"));
            }

            int current_player = rs.getInt("team1_wickets") + 1;
            String currentBattingTeam = rs.getString("current_batting");
            if (currentBattingTeam.equals("team2")) {
                current_player = rs.getInt("team2_wickets") + 1;
            }

            int currentPlayerOldScore = rs.getInt(currentBattingTeam + "_player" + current_player + "_runs");

            Map<String, String> matchStats = new HashMap<>();
            matchStats.put("team1_wickets", rs.getString("team1_wickets"));
            matchStats.put("team2_wickets", rs.getString("team2_wickets"));
            matchStats.put("team1_balls", rs.getString("team1_balls"));
            matchStats.put("team2_balls", rs.getString("team2_balls"));
            matchStats.put("current_batting", currentBattingTeam);
            matchStats.put("is_completed", rs.getString("is_completed"));
            matchStats.put("winner", rs.getString("winner"));

            if (stats.get("out").equals("true")) {
                if (currentBattingTeam.equals("team1")) {
                    team1_scores.set(current_player - 1, Integer.parseInt(stats.get("score")));
                    matchStats.put("team1_wickets", Integer.parseInt(matchStats.get("team1_wickets")) + 1 + "");
                    int bowlerIndex = updateWicketTaker(rs, matchStats, stats.get("balls"), "team2");
                    if (bowlerIndex > 0) {
                        team2_wickets.set(bowlerIndex - 1, team2_wickets.get(bowlerIndex - 1) + 1);
                    }
                } else {
                    team2_scores.set(current_player - 1, Integer.parseInt(stats.get("score")));
                    matchStats.put("team2_wickets", Integer.parseInt(matchStats.get("team2_wickets")) + 1 + "");
                    int bowlerIndex = updateWicketTaker(rs, matchStats, stats.get("balls"), "team1");
                    if (bowlerIndex > 0) {
                        team1_wickets.set(bowlerIndex - 1, team1_wickets.get(bowlerIndex - 1) + 1);
                    }
                }
            } else {
                if (currentBattingTeam.equals("team1")) {
                    team1_scores.set(current_player - 1, Integer.parseInt(stats.get("score")));
                } else {
                    team2_scores.set(current_player - 1, Integer.parseInt(stats.get("score")));
                }
            }

            if (matchStats.get("is_completed").equals("true")) {
                response.setStatus(400);
                jsonResponse.put("message", "Match already completed");
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                return;
            }

            if (currentBattingTeam.equals("team1")) {
                if (Integer.parseInt(stats.get("balls")) < Integer.parseInt(matchStats.get("team1_balls"))) {
                    response.setStatus(400);
                    jsonResponse.put("message", "Invalid Data team1");
                    response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                    return;
                }
                matchStats.put("team1_balls", stats.get("balls"));
                if (!validateStats(stats, "team1", matchStats, response, currentPlayerOldScore)) return;
                if (Integer.parseInt(matchStats.get("team1_balls")) == 120 || Integer.parseInt(matchStats.get("team1_wickets")) == 10 || Integer.parseInt(stats.get("balls")) == 120) {
                    matchStats.put("current_batting", "team2");
                }
            } else if (currentBattingTeam.equals("team2")) {
                if (Integer.parseInt(stats.get("balls")) < Integer.parseInt(matchStats.get("team2_balls"))) {
                    response.setStatus(400);
                    jsonResponse.put("message", "Invalid Data team2");
                    response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                    return;
                }
                matchStats.put("team2_balls", stats.get("balls"));
                if (!validateStats(stats, "team2", matchStats, response, currentPlayerOldScore)) return;
                int team1_score = team1_scores.stream().mapToInt(Integer::intValue).sum();
                int team2_score = team2_scores.stream().mapToInt(Integer::intValue).sum();
                if (team2_score > team1_score) {
                    matchStats.put("winner", "team2");
                    matchStats.put("is_completed", "true");
                }
                if (matchStats.get("team2_balls").equals("120") || matchStats.get("team2_wickets").equals("10")) {
                    findWinner(matchStats, team1_score, team2_score);
                }
            }

            updateMatchStats(matchStats, matchId, team1_scores, team2_scores, team1_wickets, team2_wickets);
            Map<String, Object> matchData = new HashMap<>();
            matchData.put("team1_score", team1_scores.stream().mapToInt(Integer::intValue).sum());
            matchData.put("team2_score", team2_scores.stream().mapToInt(Integer::intValue).sum());
            matchData.put("team1_wickets", matchStats.get("team1_wickets"));
            matchData.put("team2_wickets", matchStats.get("team2_wickets"));
            matchData.put("team1_balls", matchStats.get("team1_balls"));
            matchData.put("team2_balls", matchStats.get("team2_balls"));
            matchData.put("team1_runs", team1_scores);
            matchData.put("team2_runs", team2_scores);
            matchData.put("team1_outs", team1_wickets);
            matchData.put("team2_outs", team2_wickets);
            matchData.put("current_batting", matchStats.get("current_batting"));
            matchData.put("is_completed", matchStats.get("is_completed"));
            matchData.put("winner", matchStats.get("winner"));

//            if (matchStats.get("current_batting").equals("team1")) {
//                matchData.put("team1_current_score", team1_scores.stream().mapToInt(Integer::intValue).sum());
//            } else {
//                matchData.put("team2_current_score", team2_scores.stream().mapToInt(Integer::intValue).sum());
//            }

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
            jsonResponse.put("message", "Invalid Data " + team);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            return false;
        }
        if (Integer.parseInt(stats.get("score")) < currentPlayerOldScore || Integer.parseInt(stats.get("balls")) < Integer.parseInt(matchStats.get(team + "_balls"))) {
            response.setStatus(400);
            jsonResponse.put("message", "Corrupted Data " + team);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            return false;
        }

        if (Integer.parseInt(stats.get("balls")) > 120) {
            response.setStatus(400);
            jsonResponse.put("message", "Invalid Data " + team);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            return false;
        }

        return true;
    }

    private void findWinner(Map<String, String> matchStats, int team1_score, int team2_score) {
        if (team1_score > team2_score) {
            matchStats.put("winner", "team1");
            matchStats.put("is_completed", "true");
        } else if (team1_score < team2_score) {
            matchStats.put("winner", "team2");
            matchStats.put("is_completed", "true");
        } else {
            matchStats.put("winner", "tie");
            matchStats.put("is_completed", "true");
        }
    }

    private void updateMatchStats(Map<String, String> matchStats, String matchId, List<Integer> team1_scores, List<Integer> team2_scores, List<Integer> team1_wickets, List<Integer> team2_wickets) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = Database.getConnection();
            StringBuilder updateQuery = new StringBuilder("UPDATE match_stats SET team1_balls = ?, " +
                    "team2_balls = ?, current_batting = ?, is_completed = ?, winner = ?");

            // Add player runs updates
            for (int i = 1; i <= 11; i++) {
                updateQuery.append(", team1_player").append(i).append("_runs = ?");
            }
            for (int i = 1; i <= 11; i++) {
                updateQuery.append(", team2_player").append(i).append("_runs = ?");
            }

            // Add player wickets updates
            for (int i = 1; i <= 11; i++) {
                updateQuery.append(", team1_player").append(i).append("_wickets = ?");
            }
            for (int i = 1; i <= 11; i++) {
                updateQuery.append(", team2_player").append(i).append("_wickets = ?");
            }

            updateQuery.append(" WHERE match_id = ?");

            stmt = conn.prepareStatement(updateQuery.toString());

            int parameterIndex = 1;
            stmt.setString(parameterIndex++, matchStats.get("team1_balls"));
            stmt.setString(parameterIndex++, matchStats.get("team2_balls"));
            stmt.setString(parameterIndex++, matchStats.get("current_batting"));
            stmt.setString(parameterIndex++, matchStats.get("is_completed"));
            stmt.setString(parameterIndex++, matchStats.get("winner"));

            // Set player runs parameters
            for (int score : team1_scores) {
                stmt.setInt(parameterIndex++, score);
            }
            for (int score : team2_scores) {
                stmt.setInt(parameterIndex++, score);
            }

            for (int wickets : team1_wickets) {
                stmt.setInt(parameterIndex++, wickets);
            }
            for (int wickets : team2_wickets) {
                stmt.setInt(parameterIndex++, wickets);
            }

            // Set player wickets parameters
//            for (int i = 1; i <= 11; i++) {
//                String wickets = matchStats.get("team1_player" + i + "_wickets");
//                stmt.setInt(parameterIndex++, wickets != null ? Integer.parseInt(wickets) : 0);
//            }
//            for (int i = 1; i <= 11; i++) {
//                String wickets = matchStats.get("team2_player" + i + "_wickets");
//                stmt.setInt(parameterIndex++, wickets != null ? Integer.parseInt(wickets) : 0);
//            }

            stmt.setInt(parameterIndex, Integer.parseInt(matchId));

            stmt.executeUpdate();
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

    private int updateWicketTaker(ResultSet rs, Map<String, String> matchStats, String balls, String opponentTeam) throws SQLException {
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

        String bowlerColumn = opponentTeam + "_player" + bowlerIndex + "_wickets";
        int currentWickets = rs.getInt(bowlerColumn);
        matchStats.put(bowlerColumn, String.valueOf(currentWickets + 1));

        return bowlerIndex;
    }
}