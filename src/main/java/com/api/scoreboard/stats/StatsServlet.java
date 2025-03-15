package com.api.scoreboard.stats;

import com.api.scoreboard.commons.Match;
import com.api.scoreboard.match.MatchListener;
import com.api.util.Database;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.List;

@WebServlet("/update-stats")
public class StatsServlet extends HttpServlet {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> jsonResponse = new HashMap<>();

    private final static String BANNER_PATH = "F:\\Code\\JAVA\\zoho_training\\uploads\\banners\\";
    private final static String TEAM_LOGO_PATH = "F:\\Code\\JAVA\\zoho_training\\uploads\\teams\\";
    private final static String PLAYER_IMAGE_PATH = "F:\\Code\\JAVA\\zoho_training\\uploads\\players\\";

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String matchId = request.getParameter("id");
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

        if (!Arrays.asList("1", "2", "4", "6", "out", "wide", "noball").contains(updateRequest)) {
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
            String query = "SELECT"+
                    " t1.id AS team1_id, t2.id AS team2_id, t1.name AS team1_name, t2.name AS team2_name,"+
                    " m.current_batting, m.is_completed, m.winner, m.active_batsman_index, m.passive_batsman_index, m.active_bowler_index, m.tournament_id, m.highlights_path"+
                    " FROM matches m"+
                    " JOIN teams t1 ON m.team1_id = t1.id"+
                    " JOIN teams t2 ON m.team2_id = t2.id"+
                    " WHERE m.id = ?";
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
            int activeBowlerIndex = rs.getInt("active_bowler_index");
            int tournamentId = rs.getInt("tournament_id");

            if ("true".equals(isCompleted)) {
                response.setStatus(400);
                jsonResponse.put("message", "Match already completed");
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                return;
            }

            Map<String, String> matchStats = new HashMap<>();
            matchStats.put("id", matchId);
            matchStats.put("team1_id", String.valueOf(team1Id));
            matchStats.put("team2_id", String.valueOf(team2Id));
            matchStats.put("team1_name", rs.getString("team1_name"));
            matchStats.put("team2_name", rs.getString("team2_name"));
            matchStats.put("current_batting", currentBattingTeam);
            matchStats.put("active_batsman_index", String.valueOf(activeBatsmanIndex));
            matchStats.put("passive_batsman_index", String.valueOf(passiveBatsmanIndex));
            matchStats.put("active_bowler_index", String.valueOf(activeBowlerIndex));
            matchStats.put("is_completed", isCompleted);
            matchStats.put("highlights_path", rs.getString("highlights_path"));
            matchStats.put("winner", winner);

            query = "SELECT ps.player_id, ps.runs, ps.wickets, ps.team_id, ps.wicketer_id, ps.balls, ps.fours, ps.sixes, ps.wide_balls, ps.no_balls, " +
                    "w.name AS wicketer_name, p.name AS player_name " +
                    "FROM player_stats ps " +
                    "JOIN team_players tp ON ps.player_id = tp.player_id " +
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
            Map<Integer, Map<String, Object>> team1PlayersMap = new HashMap<>();
            Map<Integer, Map<String, Object>> team2PlayersMap = new HashMap<>();

            List<String> team1WicketsMap = new ArrayList<>();
            List<String> team2WicketsMap = new ArrayList<>();
            List<Integer> team1BallsMap = new ArrayList<>();
            List<Integer> team2BallsMap = new ArrayList<>();
            List<Integer> team1FreehitsMap = new ArrayList<>();
            List<Integer> team2FreehitsMap = new ArrayList<>();
            List<Integer> team1BattingOrder = new ArrayList<>();
            List<Integer> team2BattingOrder = new ArrayList<>();
            List<Integer> team1BowlingOrder = new ArrayList<>();
            List<Integer> team2BowlingOrder = new ArrayList<>();
            int team1_balls = 0;
            int team2_balls = 0;
            int wicketer_id = -1;

            while (rs.next()) {
                int playerId = rs.getInt("player_id");
                int runs = rs.getInt("runs");
                int balls = rs.getInt("balls");
                int wideBalls = rs.getInt("wide_balls");
                int noBalls = rs.getInt("no_balls");
                int fours = rs.getInt("fours");
                int sixes = rs.getInt("sixes");
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
                    team1PlayersMap.put(playerId, Map.of("runs", runs, "balls", balls, "fours", fours, "sixes", sixes, "wickets", wickets, "wide_balls", wideBalls, "no_balls", noBalls));
                } else if (teamId == team2Id) {
                    team2Scores.put(playerId, runs);
                    team2Wickets.put(playerId, wickets);
                    team2_balls += balls;
                    team2WicketsMap.add(wicketerName);
                    team2Players.put(playerId, rs.getString("player_name"));
                    team2BallsMap.add(balls);
                    team2PlayersMap.put(playerId, Map.of("runs", runs, "balls", balls, "fours", fours, "sixes", sixes, "wickets", wickets, "wide_balls", wideBalls, "no_balls", noBalls));
                }
            }

            team1Scores = sortHashMapWithVals(team1Scores);
            team2Scores = sortHashMapWithVals(team2Scores);
            team1Wickets = sortHashMapWithVals(team1Wickets);
            team2Wickets = sortHashMapWithVals(team2Wickets);

            query = "SELECT free_hit_balls, batting_order, bowling_order, team_id FROM team_order WHERE match_id = ?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, matchId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                if (team1Id == rs.getInt("team_id")) {
                    team1FreehitsMap = objectMapper.readValue(rs.getString("free_hit_balls"), ArrayList.class);
                    team1BattingOrder = objectMapper.readValue(rs.getString("batting_order"), ArrayList.class);
                    team1BowlingOrder = objectMapper.readValue(rs.getString("bowling_order"), ArrayList.class);
                } else if (team2Id == rs.getInt("team_id")) {
                    team2FreehitsMap = objectMapper.readValue(rs.getString("free_hit_balls"), ArrayList.class);
                    team2BattingOrder = objectMapper.readValue(rs.getString("batting_order"), ArrayList.class);
                    team2BowlingOrder = objectMapper.readValue(rs.getString("bowling_order"), ArrayList.class);
                }
            }

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

            if (updateRequest.equals("wide")) {
                if ("team1".equals(currentBattingTeam)) {
//                    int bowlerIndex = updateWicketTaker(team1_balls, team2BowlingOrder);
                    int bowlerIndex = activeBowlerIndex;
                    if (bowlerIndex != -1) {
                        int bowlerId = (int) team2Wickets.keySet().toArray()[bowlerIndex - 1];
                        Map<String, Object> bowlerStats = new HashMap<>(team2PlayersMap.get(bowlerId));
                        bowlerStats.put("wide_balls", (int) bowlerStats.get("wide_balls") + 1);
                        team2PlayersMap.put(bowlerId, bowlerStats);
                    }
                } else {
//                    int bowlerIndex = updateWicketTaker(team2_balls);
                    int bowlerIndex = activeBowlerIndex;
                    if (bowlerIndex != -1) {
                        int bowlerId = (int) team1Wickets.keySet().toArray()[bowlerIndex - 1];
                        Map<String, Object> bowlerStats = new HashMap<>(team1PlayersMap.get(bowlerId));
                        bowlerStats.put("wide_balls", (int) bowlerStats.get("wide_balls") + 1);
                        team1PlayersMap.put(bowlerId, bowlerStats);
                    }
                }
            } else if (updateRequest.equals("noball")) {
                if ("team1".equals(currentBattingTeam)) {
//                    int bowlerIndex = updateWicketTaker(team1_balls);
                    int bowlerIndex = activeBowlerIndex;
                    if (bowlerIndex != -1) {
                        int bowlerId = (int) team2Wickets.keySet().toArray()[bowlerIndex - 1];
                        Map<String, Object> bowlerStats = new HashMap<>(team2PlayersMap.get(bowlerId));
                        bowlerStats.put("no_balls", (int) bowlerStats.get("no_balls") + 1);
                        team2PlayersMap.put(bowlerId, bowlerStats);
                        updateFreehit((team1_balls + 1), team1Id, Integer.parseInt(matchId), team1FreehitsMap);
                        team1FreehitsMap.add(team1_balls + 1);
                    }
                } else {
//                    int bowlerIndex = updateWicketTaker(team2_balls);
                    int bowlerIndex = activeBowlerIndex;
                    if (bowlerIndex != -1) {
                        int bowlerId = (int) team1Wickets.keySet().toArray()[bowlerIndex - 1];
                        Map<String, Object> bowlerStats = new HashMap<>(team1PlayersMap.get(bowlerId));
                        bowlerStats.put("no_balls", (int) bowlerStats.get("no_balls") + 1);
                        team1PlayersMap.put(bowlerId, bowlerStats);
                        updateFreehit((team2_balls + 1), team2Id, Integer.parseInt(matchId), team2FreehitsMap);
                        team2FreehitsMap.add(team2_balls + 1);
                    }
                }
            } else if (updateRequest.equals("out")) {
                if ("team1".equals(currentBattingTeam)) {
                    team1_balls++;
                    team1BallsMap.set(activeBatsmanIndex - 1, team1BallsMap.get(activeBatsmanIndex - 1) + 1);
                } else {
                    team2_balls++;
                    team2BallsMap.set(activeBatsmanIndex - 1, team2BallsMap.get(activeBatsmanIndex - 1) + 1);
                }
                if ("team1".equals(currentBattingTeam)) {
                    if (!team1FreehitsMap.contains(team1_balls)) {
//                        int bowlerIndex = updateWicketTaker(team1_balls);
                        int bowlerIndex = activeBowlerIndex;
                        if (bowlerIndex != -1) {
                            int bowlerId = (int) team2Wickets.keySet().toArray()[bowlerIndex - 1];
                            wicketer_id = bowlerId;
                            System.out.println("bowlerId: " + bowlerId);
                            System.out.println("bowlerIndex: " + bowlerIndex);
                            team2Wickets.put(bowlerId, team2Wickets.getOrDefault(bowlerId, 0) + 1);
                            team1WicketsMap.set(activeBatsmanIndex - 1, team2Players.get(bowlerId));
                            activeBatsmanIndex = -1;
//                            for (int i = 0; i < team1WicketsMap.size(); i++) {
//                                if (team1WicketsMap.get(i) == null && i != passiveBatsmanIndex - 1) {
//                                    activeBatsmanIndex = i + 1;
//                                    break;
//                                }
//                            }
                        }
                    }
                } else {
                    if (!team2FreehitsMap.contains(team2_balls)) {
//                        int bowlerIndex = updateWicketTaker(team2_balls);
                        int bowlerIndex = activeBowlerIndex;
                        if (bowlerIndex != -1) {
                            int bowlerId = (int) team1Wickets.keySet().toArray()[bowlerIndex - 1];
                            wicketer_id = bowlerId;
                            team1Wickets.put(bowlerId, team1Wickets.getOrDefault(bowlerId, 0) + 1);
                            team2WicketsMap.set(activeBatsmanIndex - 1, team1Players.get(bowlerId));
                            activeBatsmanIndex = -1;
//                            for (int i = 0; i < team2WicketsMap.size(); i++) {
//                                if (team2WicketsMap.get(i) == null && i != passiveBatsmanIndex - 1) {
//                                    activeBatsmanIndex = i + 1;
//                                    break;
//                                }
//                            }
                        }
                    }
                }

                if (currentBattingTeam.equals("team1") && team1_balls % 6 == 0) {
                    int temp = activeBatsmanIndex;
                    activeBatsmanIndex = passiveBatsmanIndex;
                    passiveBatsmanIndex = temp;
                    activeBowlerIndex = -1;
                } else if (currentBattingTeam.equals("team2") && team2_balls % 6 == 0) {
                    int temp = activeBatsmanIndex;
                    activeBatsmanIndex = passiveBatsmanIndex;
                    passiveBatsmanIndex = temp;
                    activeBowlerIndex = -1;
                }
            } else {
                if ("team1".equals(currentBattingTeam)) {
                    team1_balls++;
                    team1BallsMap.set(activeBatsmanIndex - 1, team1BallsMap.get(activeBatsmanIndex - 1) + 1);
                    if (team1_balls % 6 == 0) {
                        int temp = activeBatsmanIndex;
                        activeBatsmanIndex = passiveBatsmanIndex;
                        passiveBatsmanIndex = temp;
                        activeBowlerIndex = -1;
                    }
                } else {
                    team2_balls++;
                    team2BallsMap.set(activeBatsmanIndex - 1, team2BallsMap.get(activeBatsmanIndex - 1) + 1);
                    if (team2_balls % 6 == 0) {
                        int temp = activeBatsmanIndex;
                        activeBatsmanIndex = passiveBatsmanIndex;
                        passiveBatsmanIndex = temp;
                        activeBowlerIndex = -1;
                    }
                }
                if ("team1".equals(currentBattingTeam)) {
                    team1Scores.put(currentPlayerId, Integer.parseInt(updateRequest) + currentPlayerOldScore);
                    if ("1".equals(updateRequest)) {
                        int temp = activeBatsmanIndex;
                        activeBatsmanIndex = passiveBatsmanIndex;
                        passiveBatsmanIndex = temp;
                    } else if ("4".equals(updateRequest)) {
                        Map<String, Object> playerStats = new HashMap<>(team1PlayersMap.get(currentPlayerId));
                        playerStats.put("fours", (int) playerStats.get("fours") + 1);
                        team1PlayersMap.put(currentPlayerId, playerStats);
                    } else if ("6".equals(updateRequest)) {
                        Map<String, Object> playerStats = new HashMap<>(team1PlayersMap.get(currentPlayerId));
                        playerStats.put("sixes", (int) playerStats.get("sixes") + 1);
                        team1PlayersMap.put(currentPlayerId, playerStats);
                    }
                } else {
                    team2Scores.put(currentPlayerId, Integer.parseInt(updateRequest) + currentPlayerOldScore);
                    if ("1".equals(updateRequest)) {
                        int temp = activeBatsmanIndex;
                        activeBatsmanIndex = passiveBatsmanIndex;
                        passiveBatsmanIndex = temp;
                    } else if ("4".equals(updateRequest)) {
                        Map<String, Object> playerStats = new HashMap<>(team2PlayersMap.get(currentPlayerId));
                        playerStats.put("fours", (int) playerStats.get("fours") + 1);
                        team2PlayersMap.put(currentPlayerId, playerStats);
                    } else if ("6".equals(updateRequest)) {
                        Map<String, Object> playerStats = new HashMap<>(team2PlayersMap.get(currentPlayerId));
                        playerStats.put("sixes", (int) playerStats.get("sixes") + 1);
                        team2PlayersMap.put(currentPlayerId, playerStats);
                    }
                }
            }

            matchStats.put("team1_wickets", String.valueOf(team2Wickets.values().stream().mapToInt(Integer::intValue).sum()));
            matchStats.put("team2_wickets", String.valueOf(team1Wickets.values().stream().mapToInt(Integer::intValue).sum()));

            if ("team1".equals(currentBattingTeam)) {
                if (team2Wickets.values().stream().mapToInt(Integer::intValue).sum() == 10 || team1_balls == 120) {
                    matchStats.put("current_batting", "team2");
                    activeBatsmanIndex = -1;
                    passiveBatsmanIndex = -1;
                    activeBowlerIndex = -1;
                }
            } else if ("team2".equals(currentBattingTeam)) {
                int team1Score = team1Scores.values().stream().mapToInt(Integer::intValue).sum();
                int team2Score = team2Scores.values().stream().mapToInt(Integer::intValue).sum();
                if (team2Score > team1Score) {
                    if (tournamentId != 0) {
                        updateTournament(team1Id, team2Id, Integer.parseInt(matchId), tournamentId);
                    }
                    matchStats.put("winner", "team2");
                    matchStats.put("is_completed", "true");
                }
                if (team2_balls == 120 || team1Wickets.values().stream().mapToInt(Integer::intValue).sum() == 10) {
                    if (team1Score > team2Score) {
                        if (tournamentId != 0) {
                            updateTournament(team2Id, team1Id, Integer.parseInt(matchId), tournamentId);
                        }
                        matchStats.put("winner", "team1");
                        matchStats.put("is_completed", "true");
                    } else {
                        matchStats.put("winner", "tie");
                        matchStats.put("is_completed", "true");
                    }
                }
            }

            updateMatchStats(matchStats, team1Scores, team2Scores, team1Wickets, team2Wickets, activeBatsmanIndex, passiveBatsmanIndex, activeBowlerIndex, wicketer_id, team1BallsMap, team2BallsMap, currentPlayerId, team1PlayersMap, team2PlayersMap);
            Map<String, Object> matchData = new HashMap<>();
            int team1_wide_runs = team2PlayersMap.values().stream().mapToInt(player -> (int) player.get("wide_balls")).sum();
            int team2_wide_runs = team1PlayersMap.values().stream().mapToInt(player -> (int) player.get("wide_balls")).sum();
            int team1_no_balls = team2PlayersMap.values().stream().mapToInt(player -> (int) player.get("no_balls")).sum();
            int team2_no_balls = team1PlayersMap.values().stream().mapToInt(player -> (int) player.get("no_balls")).sum();

            matchData.put("id", matchStats.get("id"));
            matchData.put("current_batting", matchStats.get("current_batting"));
            matchData.put("is_completed", matchStats.get("is_completed"));
            matchData.put("is_highlights_uploaded", matchStats.get("highlights_path") == null && Objects.equals(matchStats.get("is_completed"), "true") ? "false" : "true");
            matchData.put("winner", matchStats.get("winner"));
            matchData.put("active_batsman_index", activeBatsmanIndex);
            matchData.put("passive_batsman_index", passiveBatsmanIndex);
            matchData.put("active_bowler_index", activeBowlerIndex);
            matchData.put("team1_score", team1Scores.values().stream().mapToInt(Integer::intValue).sum() + team1_wide_runs + team1_no_balls);
            matchData.put("team2_score", team2Scores.values().stream().mapToInt(Integer::intValue).sum() + team2_wide_runs + team2_no_balls);
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
            matchData.put("team1_freehits_map", team1FreehitsMap);
            matchData.put("team2_freehits_map", team2FreehitsMap);
            matchData.put("team1_batting_order", team1BattingOrder);
            matchData.put("team2_batting_order", team2BattingOrder);
            matchData.put("team1_bowling_order", team1BowlingOrder);
            matchData.put("team2_bowling_order", team2BowlingOrder);

            StatsListener.fireStatsUpdate(matchId, objectMapper.writeValueAsString(matchData));
            response.setStatus(200);
            jsonResponse.put("message", "success");
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } catch (SQLException e) {
            jsonResponse.put("message", "Database error: " + e.getMessage());
            response.setStatus(500);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } catch (Exception e) {
            jsonResponse.put("message", "Error: " + e.getMessage());
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

    private void updateMatchStats(Map<String, String> matchStats, Map<Integer, Integer> team1Scores, Map<Integer, Integer> team2Scores, Map<Integer, Integer> team1Wickets, Map<Integer, Integer> team2Wickets, int activeBatsmanIndex, int passiveBatsmanIndex, int activeBowlerIndex, int wicketer_id, List<Integer> team1BallsMap, List<Integer> team2BallsMap, int currentPlayerId, Map<Integer, Map<String, Object>> team1PlayersMap, Map<Integer, Map<String, Object>> team2PlayersMap) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = Database.getConnection();
            String query = "UPDATE matches SET current_batting = ?, is_completed = ?, winner = ?, active_batsman_index = ?, passive_batsman_index = ?, active_bowler_index = ? WHERE id = ?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, matchStats.get("current_batting"));
            stmt.setString(2, matchStats.get("is_completed"));
            stmt.setString(3, matchStats.get("winner"));
            stmt.setInt(4, activeBatsmanIndex);
            stmt.setInt(5, passiveBatsmanIndex);
            stmt.setInt(6, activeBowlerIndex);
            stmt.setInt(7, Integer.parseInt(matchStats.get("id")));
            stmt.executeUpdate();

            query = "UPDATE player_stats SET runs = ?, balls = ?, wickets = ?, wide_balls = ?, no_balls = ?, fours = ?, sixes = ? WHERE match_id = ? AND player_id = ?";

            stmt = conn.prepareStatement(query);
            int i = 0;
            for (Map.Entry<Integer, Integer> entry : team1Scores.entrySet()) {
                stmt.setInt(1, entry.getValue());
                stmt.setInt(2, team1BallsMap.get(i));
                stmt.setInt(3, team1Wickets.get(entry.getKey()));
                stmt.setInt(4, (int) team1PlayersMap.get(entry.getKey()).get("wide_balls"));
                stmt.setInt(5, (int) team1PlayersMap.get(entry.getKey()).get("no_balls"));
                stmt.setInt(6, (int) team1PlayersMap.get(entry.getKey()).get("fours"));
                stmt.setInt(7, (int) team1PlayersMap.get(entry.getKey()).get("sixes"));
                stmt.setInt(8, Integer.parseInt(matchStats.get("id")));
                stmt.setInt(9, entry.getKey());
                stmt.addBatch();
                i++;
            }

            i = 0;
            for (Map.Entry<Integer, Integer> entry : team2Scores.entrySet()) {
                stmt.setInt(1, entry.getValue());
                stmt.setInt(2, team2BallsMap.get(i));
                stmt.setInt(3, team2Wickets.get(entry.getKey()));
                stmt.setInt(4, (int) team2PlayersMap.get(entry.getKey()).get("wide_balls"));
                stmt.setInt(5, (int) team2PlayersMap.get(entry.getKey()).get("no_balls"));
                stmt.setInt(6, (int) team2PlayersMap.get(entry.getKey()).get("fours"));
                stmt.setInt(7, (int) team2PlayersMap.get(entry.getKey()).get("sixes"));
                stmt.setInt(8, Integer.parseInt(matchStats.get("id")));
                stmt.setInt(9, entry.getKey());
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

            if (matchStats.get("is_completed").equals("true") && matchStats.get("winner").equals("team1")) {
                createBanner(Integer.parseInt(matchStats.get("team1_id")), Integer.parseInt(matchStats.get("id")));
            } else if (matchStats.get("is_completed").equals("true") && matchStats.get("winner").equals("team2")) {
                createBanner(Integer.parseInt(matchStats.get("team2_id")), Integer.parseInt(matchStats.get("id")));
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

    private void updateFreehit(int ballCount, int teamId, int matchId, List<Integer> ballList) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = Database.getConnection();
            ballList.add(ballCount);
            String query = "UPDATE team_order SET free_hit_balls = ? WHERE match_id = ? AND team_id = ?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, objectMapper.writeValueAsString(ballList));
            stmt.setInt(2, matchId);
            stmt.setInt(3, teamId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error updating free hit: " + e.getMessage());
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

//    private int updateWicketTaker(int ballCount, List<Integer> activeBowlerIndex) {
//
//        int bowlerIndex;
//
//        if (ballCount <= 66) {
//            int over = ballCount / 6;
//            int ball = ballCount % 6;
//            bowlerIndex = over + (ball > 0 ? 1 : 0);
//        } else {
//            ballCount -= 66;
//            int over = ballCount / 6;
//            int ball = ballCount % 6;
//            bowlerIndex = over + (ball > 0 ? 1 : 0);
//        }
//
//        if (bowlerIndex < 0 || bowlerIndex > 11) {
//            return -1;
//        }
//
//        if (bowlerIndex == 0) {
//            bowlerIndex = 1;
//        }
//
//        return bowlerIndex;
//    }

    private Map<Integer, Integer> sortHashMapWithVals(Map<Integer, Integer> map) {
        List<Map.Entry<Integer, Integer>> list = new ArrayList<>(map.entrySet());
        list.sort(Comparator.comparingInt(Map.Entry::getKey));
        Map<Integer, Integer> sortedMap = new LinkedHashMap<>();

        for (Map.Entry<Integer, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    private void updateTournament(int lossingTeamId, int winningTeamId, int matchId, int tournamentId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = Database.getConnection();
            String query = "SELECT team_id FROM tournament_winners WHERE tournament_id = ?";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, tournamentId);

            rs = stmt.executeQuery();
            if (rs.next()) {
                int oldWinnerId = rs.getInt("team_id");
                Match.create(conn, oldWinnerId, winningTeamId, tournamentId);
                conn = Database.getConnection();
                query = "DELETE FROM tournament_winners WHERE tournament_id = ?";
                stmt = conn.prepareStatement(query);
                stmt.setInt(1, tournamentId);
                stmt.executeUpdate();
                MatchListener.fireMatchesUpdate();
            } else {
                query = "INSERT INTO tournament_winners (tournament_id, team_id) VALUES (?, ?)";
                stmt = conn.prepareStatement(query);
                stmt.setInt(1, tournamentId);
                stmt.setInt(2, winningTeamId);
                stmt.executeUpdate();

                query = "SELECT COUNT(*) AS total_teams FROM matches WHERE tournament_id = ? AND is_completed = 'false'";
                stmt = conn.prepareStatement(query);
                stmt.setInt(1, tournamentId);
                rs = stmt.executeQuery();

                if (rs.next()) {
                    int totalTeams = rs.getInt("total_teams");
                    if (totalTeams == 1) {
                        query = "UPDATE tournaments SET status = 'completed', winner_id = ? WHERE id = ?";
                        stmt = conn.prepareStatement(query);
                        stmt.setInt(1, winningTeamId);
                        stmt.setInt(2, tournamentId);
                        stmt.executeUpdate();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error checking tournament: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }

    public void createBanner(int teamId, int matchId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = Database.getConnection();
            String query = "SELECT t.name AS team_name, t.logo AS team_logo, " +
                    "MAX(CASE WHEN ps.runs = (SELECT MAX(ps2.runs) FROM player_stats ps2 " +
                    "JOIN team_players tp2 ON ps2.player_id = tp2.player_id WHERE ps2.match_id = ? AND tp2.team_id = t.id) THEN p.name END) AS highest_runs_player, " +
                    "MAX(CASE WHEN ps.runs = (SELECT MAX(ps2.runs) FROM player_stats ps2 " +
                    "JOIN team_players tp2 ON ps2.player_id = tp2.player_id WHERE ps2.match_id = ? AND tp2.team_id = t.id) THEN p.avatar END) AS highest_runs_player_avatar, " +
                    "MAX(ps.runs) AS highest_runs, " +
                    "MAX(CASE WHEN ps.wickets = (SELECT MAX(ps2.wickets) FROM player_stats ps2 " +
                    "JOIN team_players tp2 ON ps2.player_id = tp2.player_id WHERE ps2.match_id = ? AND tp2.team_id = t.id) THEN p.name END) AS highest_wickets_player, " +
                    "MAX(CASE WHEN ps.wickets = (SELECT MAX(ps2.wickets) FROM player_stats ps2 " +
                    "JOIN team_players tp2 ON ps2.player_id = tp2.player_id WHERE ps2.match_id = ? AND tp2.team_id = t.id) THEN p.avatar END) AS highest_wickets_player_avatar, " +
                    "MAX(ps.wickets) AS highest_wickets, " +
                    "MAX(CASE WHEN ps.sixes = (SELECT MAX(ps2.sixes) FROM player_stats ps2 " +
                    "JOIN team_players tp2 ON ps2.player_id = tp2.player_id WHERE ps2.match_id = ? AND tp2.team_id = t.id) THEN p.name END) AS highest_sixes_player, " +
                    "MAX(CASE WHEN ps.sixes = (SELECT MAX(ps2.sixes) FROM player_stats ps2 " +
                    "JOIN team_players tp2 ON ps2.player_id = tp2.player_id WHERE ps2.match_id = ? AND tp2.team_id = t.id) THEN p.avatar END) AS highest_sixes_player_avatar, " +
                    "MAX(ps.sixes) AS highest_sixes " +
                    "FROM teams t " +
                    "JOIN team_players tp ON t.id = tp.team_id " +
                    "JOIN player_stats ps ON tp.player_id = ps.player_id " +
                    "JOIN players p ON ps.player_id = p.id " +
                    "WHERE ps.match_id = ? AND t.id = ? " +
                    "GROUP BY t.id, t.name, t.logo";

            stmt = conn.prepareStatement(query);
            stmt.setInt(1, matchId);
            stmt.setInt(2, matchId);
            stmt.setInt(3, matchId);
            stmt.setInt(4, matchId);
            stmt.setInt(5, matchId);
            stmt.setInt(6, matchId);
            stmt.setInt(7, matchId);
            stmt.setInt(8, teamId);

            rs = stmt.executeQuery();

            if (rs.next()) {
                String teamName = rs.getString("team_name");
                String teamLogo = rs.getString("team_logo");
                int highestRuns = rs.getInt("highest_runs");
                String highestRunsPlayer = rs.getString("highest_runs_player");
                String highestRunsPlayerAvatar = rs.getString("highest_runs_player_avatar");
                int highestWickets = rs.getInt("highest_wickets");
                String highestWicketsPlayer = rs.getString("highest_wickets_player");
                String highestWicketsPlayerAvatar = rs.getString("highest_wickets_player_avatar");
                int highestSixes = rs.getInt("highest_sixes");
                String highestSixesPlayer = rs.getString("highest_sixes_player");
                String highestSixesPlayerAvatar = rs.getString("highest_sixes_player_avatar");
                String bgImage = "bg.jpg";

                BufferedImage image = ImageIO.read(new File(BANNER_PATH + bgImage));
                Graphics2D g2d = image.createGraphics();

                int desiredLogoWidth = 300;
                int desiredLogoHeight = 300;

                BufferedImage teamLogoImage = ImageIO.read(new File(TEAM_LOGO_PATH + teamLogo));
                BufferedImage resizedTeamLogo = new BufferedImage(desiredLogoWidth, desiredLogoHeight, BufferedImage.TYPE_INT_ARGB);
                Graphics2D gLogo = resizedTeamLogo.createGraphics();
                gLogo.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                gLogo.drawImage(teamLogoImage, 0, 0, desiredLogoWidth, desiredLogoHeight, null);
                gLogo.dispose();

                int logoX = (image.getWidth() - desiredLogoWidth) / 2;
                int logoY = 40;

                g2d.setFont(new Font("Arial", Font.BOLD, 32));
                g2d.setColor(Color.WHITE);
                String winnerText = "Winner: " + teamName;
                int winnerX = (image.getWidth() - g2d.getFontMetrics().stringWidth(winnerText)) / 2;
                g2d.drawString(winnerText, winnerX, logoY);
                logoY += 30;

                g2d.drawImage(resizedTeamLogo, logoX, logoY, null);
                int textY = logoY + desiredLogoHeight + 30;
                g2d.setFont(new Font("Arial", Font.BOLD, 26));
                int avatarSize = 200;
                int avatarSpacing = 50;
                int totalAvatarWidth = 3 * avatarSize + 2 * avatarSpacing;
                int startX = (image.getWidth() - totalAvatarWidth) / 2;
                textY = logoY + desiredLogoHeight + 30;
                g2d.setFont(new Font("Arial", Font.BOLD, 26));
                String[] titles = {"Highest Sixes", "Man of the Match", "Highest Wickets"};
                int titleY = textY;
                for (int i = 0; i < titles.length; i++) {
                    int titleX = startX + i * (avatarSize + avatarSpacing) + (avatarSize - g2d.getFontMetrics().stringWidth(titles[i])) / 2;

                    g2d.setColor(Color.GRAY);
                    g2d.drawString(titles[i], titleX + 2, titleY + 2);

                    g2d.setColor(Color.WHITE);
                    g2d.drawString(titles[i], titleX, titleY);
                }

                textY += 20;
                if (highestSixesPlayerAvatar != null && !highestSixesPlayerAvatar.isEmpty() && highestSixes > 0) {
                    BufferedImage highestSixesPlayerImage = ImageIO.read(new File(PLAYER_IMAGE_PATH + highestSixesPlayerAvatar));
                    int avatarX = startX;
                    g2d.drawImage(highestSixesPlayerImage, avatarX, textY, avatarSize, avatarSize, null);
                    String text = highestSixesPlayer + " (" + highestSixes + ")";
                    int textX = avatarX + (avatarSize - g2d.getFontMetrics().stringWidth(text)) / 2;

                    g2d.setColor(Color.GRAY);
                    g2d.drawString(text, textX + 2, textY + avatarSize + 22);

                    g2d.setColor(Color.WHITE);
                    g2d.drawString(text, textX, textY + avatarSize + 30);
                }

                if (highestRunsPlayerAvatar != null && !highestRunsPlayerAvatar.isEmpty()) {
                    BufferedImage highestRunsPlayerImage = ImageIO.read(new File(PLAYER_IMAGE_PATH + highestRunsPlayerAvatar));
                    int avatarX = startX + avatarSize + avatarSpacing;
                    g2d.drawImage(highestRunsPlayerImage, avatarX, textY, avatarSize, avatarSize, null);
                    String text = highestRunsPlayer + " (" + highestRuns + ")";
                    int textX = avatarX + (avatarSize - g2d.getFontMetrics().stringWidth(text)) / 2;

                    g2d.setColor(Color.GRAY);
                    g2d.drawString(text, textX + 2, textY + avatarSize + 32);

                    g2d.setColor(Color.WHITE);
                    g2d.drawString(text, textX, textY + avatarSize + 30);
                }

                if (highestWicketsPlayerAvatar != null && !highestWicketsPlayerAvatar.isEmpty()) {
                    BufferedImage highestWicketsPlayerImage = ImageIO.read(new File(PLAYER_IMAGE_PATH + highestWicketsPlayerAvatar));
                    int avatarX = startX + 2 * (avatarSize + avatarSpacing);
                    g2d.drawImage(highestWicketsPlayerImage, avatarX, textY, avatarSize, avatarSize, null);
                    String text = highestWicketsPlayer + " (" + highestWickets + ")";
                    int textX = avatarX + (avatarSize - g2d.getFontMetrics().stringWidth(text)) / 2;

                    g2d.setColor(Color.GRAY);
                    g2d.drawString(text, textX + 2, textY + avatarSize + 22);

                    g2d.setColor(Color.WHITE);
                    g2d.drawString(text, textX, textY + avatarSize + 30);
                }

                g2d.dispose();
                String outputFilePath = BANNER_PATH + "banner_" + teamId + "_" + matchId + ".jpg";
                ImageIO.write(image, "png", new File(outputFilePath));

                query = "UPDATE matches SET banner_path = ? WHERE id = ?";
                stmt = conn.prepareStatement(query);
                stmt.setString(1, "banner_" + teamId + "_" + matchId + ".jpg");
                stmt.setInt(2, matchId);
                stmt.executeUpdate();

                System.out.println("Banner created successfully: " + outputFilePath);
            }
        } catch (SQLException e) {
            System.out.println("Error creating banner: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error reading image: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.out.println("Error Closing Resource: " + e.getMessage());
            }
        }
    }
}