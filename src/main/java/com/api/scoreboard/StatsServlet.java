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
//            CREATE TABLE match_stats
//            (
//                    id              INT AUTO_INCREMENT PRIMARY KEY,
//                    match_id        INT      NOT NULL,
//                    team1_stats_id  INT      NOT NULL,
//                    team2_stats_id  INT      NOT NULL,
//                    is_completed    ENUM('true', 'false') NOT NULL DEFAULT 'false',
//            winner          ENUM('team1', 'team2', 'none', 'tie') NOT NULL DEFAULT 'none',
//            current_batting ENUM('team1', 'team2') NOT NULL DEFAULT 'team1',
//            created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
//            FOREIGN KEY (match_id) REFERENCES matches (id) ON DELETE CASCADE,
//    FOREIGN KEY (team1_stats_id) REFERENCES team_stats (id) ON DELETE CASCADE,
//            FOREIGN KEY (team2_stats_id) REFERENCES team_stats (id) ON DELETE CASCADE
//);
//
//    CREATE TABLE team_stats
//            (
//                    id               INT AUTO_INCREMENT PRIMARY KEY,
//                    team_id          INT           NOT NULL,
//                    player1_runs     INT DEFAULT 0,
//                    player2_runs     INT DEFAULT 0,
//                    player3_runs     INT DEFAULT 0,
//                    player4_runs     INT DEFAULT 0,
//                    player5_runs     INT DEFAULT 0,
//                    player6_runs     INT DEFAULT 0,
//                    player7_runs     INT DEFAULT 0,
//                    player8_runs     INT DEFAULT 0,
//                    player9_runs     INT DEFAULT 0,
//                    player10_runs    INT DEFAULT 0,
//                    player11_runs    INT DEFAULT 0,
//                    player1_wickets  INT DEFAULT 0,
//                    player2_wickets  INT DEFAULT 0,
//                    player3_wickets  INT DEFAULT 0,
//                    player4_wickets  INT DEFAULT 0,
//                    player5_wickets  INT DEFAULT 0,
//                    player6_wickets  INT DEFAULT 0,
//                    player7_wickets  INT DEFAULT 0,
//                    player8_wickets  INT DEFAULT 0,
//                    player9_wickets  INT DEFAULT 0,
//                    player10_wickets INT DEFAULT 0,
//                    player11_wickets INT DEFAULT 0,
//                    balls            INT DEFAULT 0 NOT NULL,
//                    total_score      INT GENERATED ALWAYS AS (
//                            player1_runs + player2_runs + player3_runs + player4_runs + player5_runs +
//                                    player6_runs + player7_runs + player8_runs + player9_runs + player10_runs + player11_runs
//                    ) VIRTUAL,
//                    total_wickets    INT GENERATED ALWAYS AS (
//                            player1_wickets + player2_wickets + player3_wickets + player4_wickets + player5_wickets +
//                                    player6_wickets + player7_wickets + player8_wickets + player9_wickets + player10_wickets + player11_wickets
//                    ) VIRTUAL,
//                    FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE
//            );
            String query = "SELECT ms.*, ts1.*, ts2.* " +
                    "FROM match_stats ms " +
                    "JOIN team_stats ts1 ON ms.team1_stats_id = ts1.id " +
                    "JOIN team_stats ts2 ON ms.team2_stats_id = ts2.id " +
                    "WHERE ms.match_id = ?";
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
                team1_scores.add(rs.getInt("ts1.player" + i + "_runs"));
                team2_scores.add(rs.getInt("ts2.player" + i + "_runs"));
            }

            for (int i = 1; i <= 11; i++) {
                team1_wickets.add(rs.getInt("ts1.player" + i + "_wickets"));
                team2_wickets.add(rs.getInt("ts2.player" + i + "_wickets"));
            }

            int current_player = rs.getInt("ts2.total_wickets") + 1;
            String currentBattingTeam = rs.getString("ms.current_batting");
            if (currentBattingTeam.equals("team2")) {
                current_player = rs.getInt("ts1.total_wickets") + 1;
            }

            int currentPlayerOldScore = rs.getInt(currentBattingTeam.equals("team1") ? "ts1.player" + current_player + "_runs" : "ts2.player" + current_player + "_runs");

            Map<String, String> matchStats = new HashMap<>();
            matchStats.put("team1_wickets", rs.getString("ts1.total_wickets"));
            matchStats.put("team2_wickets", rs.getString("ts2.total_wickets"));
            matchStats.put("team1_balls", rs.getString("ts1.balls"));
            matchStats.put("team2_balls", rs.getString("ts2.balls"));
            matchStats.put("current_batting", rs.getString("ms.current_batting"));
            matchStats.put("is_completed", rs.getString("ms.is_completed"));
            matchStats.put("winner", rs.getString("ms.winner"));
//            matchStats.put("team1_wickets", rs.getString("team1_wickets"));
//            matchStats.put("team2_wickets", rs.getString("team2_wickets"));
//            matchStats.put("team1_balls", rs.getString("team1_balls"));
//            matchStats.put("team2_balls", rs.getString("team2_balls"));
//            matchStats.put("current_batting", currentBattingTeam);
//            matchStats.put("is_completed", rs.getString("is_completed"));
//            matchStats.put("winner", rs.getString("winner"));

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
            String query = "UPDATE match_stats SET current_batting = ?, is_completed = ?, winner = ? WHERE match_id = ?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, matchStats.get("current_batting"));
            stmt.setString(2, matchStats.get("is_completed"));
            stmt.setString(3, matchStats.get("winner"));
            stmt.setInt(4, Integer.parseInt(matchId));

            stmt.executeUpdate();

            query = "UPDATE team_stats SET player1_runs = ?, player1_wickets = ?, player2_runs = ?, player2_wickets = ?, " +
                    "player3_runs = ?, player3_wickets = ?, player4_runs = ?, player4_wickets = ?, player5_runs = ?, " +
                    "player5_wickets = ?, player6_runs = ?, player6_wickets = ?, player7_runs = ?, player7_wickets = ?, " +
                    "player8_runs = ?, player8_wickets = ?, player9_runs = ?, player9_wickets = ?, player10_runs = ?, " +
                    "player10_wickets = ?, player11_runs = ?, player11_wickets = ? , balls = ? WHERE team_id = ?";
            stmt = conn.prepareStatement(query);

            for (int i = 1; i <= 11; i++) {
                stmt.setInt(i * 2 - 1, team1_scores.get(i - 1));
                stmt.setInt(i * 2, team1_wickets.get(i - 1));
            }
            stmt.setInt(23, Integer.parseInt(matchStats.get("team1_balls")));
            stmt.setInt(24, 1);
            stmt.executeUpdate();

            for (int i = 1; i <= 11; i++) {
                stmt.setInt(i * 2 - 1, team2_scores.get(i - 1));
                stmt.setInt(i * 2, team2_wickets.get(i - 1));
            }
            stmt.setInt(23, Integer.parseInt(matchStats.get("team2_balls")));
            stmt.setInt(24, 2);
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

        String bowlerColumn = "ts" + (opponentTeam.equals("team1") ? "2" : "1") + ".player" + bowlerIndex + "_wickets";
//        String bowlerColumn = opponentTeam + "_player" + bowlerIndex + "_wickets";
        int currentWickets = rs.getInt(bowlerColumn);
        matchStats.put(bowlerColumn, String.valueOf(currentWickets + 1));

        return bowlerIndex;
    }
}