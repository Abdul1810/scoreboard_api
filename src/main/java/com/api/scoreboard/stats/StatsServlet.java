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
import java.util.stream.Collectors;

@WebServlet("/api/stats/update")
public class StatsServlet extends HttpServlet {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> jsonResponse = new HashMap<>();

    private final static String BANNER_PATH = "\\uploads\\banners\\";
    private final static String TEAM_LOGO_PATH = "\\uploads\\teams\\";
    private final static String PLAYER_IMAGE_PATH = "\\uploads\\players\\";

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String matchId = request.getParameter("id");
        Map<String, String> requestData = objectMapper.readValue(request.getReader(), HashMap.class);
        String updateRequest = requestData.getOrDefault("update", "");
        String isNoball = requestData.getOrDefault("is_noball", "false");
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

        if (!Arrays.asList("1", "2", "4", "6", "out", "wide", "noball", "revert").contains(updateRequest)) {
            response.setStatus(400);
            jsonResponse.put("message", "Invalid update request");
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            return;
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = new Database().getConnection();
            String query = "SELECT" +
                    " t1.id AS team1_id, t2.id AS team2_id, t1.name AS team1_name, t2.name AS team2_name," +
                    " m.current_batting, m.is_completed, m.winner, m.active_batsman_index, m.passive_batsman_index," +
                    " m.active_bowler_index, m.tournament_id, m.highlights_path, m.user_id" +
                    " FROM matches m" +
                    " JOIN teams t1 ON m.team1_id = t1.id" +
                    " JOIN teams t2 ON m.team2_id = t2.id" +
                    " WHERE m.id = ?";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, Integer.parseInt(matchId));
            rs = stmt.executeQuery();
            int userId = (int) request.getSession().getAttribute("uid");

            if (!rs.next()) {
                response.setStatus(404);
                jsonResponse.put("message", "Match not found");
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                return;
            } else if (rs.getInt("user_id") != userId) {
                response.setStatus(403);
                jsonResponse.put("message", "Unauthorized access");
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

            Map<Integer, String> team1Players = new HashMap<>();
            Map<Integer, String> team2Players = new HashMap<>();
            Map<Integer, Map<String, Object>> team1PlayersMap = new HashMap<>();
            Map<Integer, Map<String, Object>> team2PlayersMap = new HashMap<>();

            List<String> team1WicketsMap = new ArrayList<>();
            List<String> team2WicketsMap = new ArrayList<>();
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
                    team1_balls += balls;
                    team1WicketsMap.add(wicketerName);
                    team1Players.put(playerId, rs.getString("player_name"));
                    team1PlayersMap.put(playerId, Map.of("runs", runs, "balls", balls, "fours", fours, "sixes", sixes, "wickets", wickets, "wide_balls", wideBalls, "no_balls", noBalls));
                } else if (teamId == team2Id) {
                    team2_balls += balls;
                    team2WicketsMap.add(wicketerName);
                    team2Players.put(playerId, rs.getString("player_name"));
                    team2PlayersMap.put(playerId, Map.of("runs", runs, "balls", balls, "fours", fours, "sixes", sixes, "wickets", wickets, "wide_balls", wideBalls, "no_balls", noBalls));
                }
            }

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

            team1PlayersMap = sortHashMapWithKeys(team1PlayersMap);
            team2PlayersMap = sortHashMapWithKeys(team2PlayersMap);

            int currentPlayerId = activeBatsmanIndex;
            if (currentPlayerId == -1) {
                response.setStatus(400);
                jsonResponse.put("message", "Invalid player ID");
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                return;
            }

            int currentPlayerIndex = currentPlayerId;
            currentPlayerId = "team1".equals(currentBattingTeam) ? team1PlayersMap.keySet().toArray(new Integer[0])[currentPlayerId - 1] : team2PlayersMap.keySet().toArray(new Integer[0])[currentPlayerId - 1];
            int currentPlayerOldScore = "team1".equals(currentBattingTeam) ? (int) team1PlayersMap.get(currentPlayerId).get("runs") : (int) team2PlayersMap.get(currentPlayerId).get("runs");

            if (updateRequest.equals("wide")) {
                if ("team1".equals(currentBattingTeam)) {
                    int bowlerIndex = activeBowlerIndex;
                    if (bowlerIndex != -1) {
                        int bowlerId = (int) team2PlayersMap.keySet().toArray()[bowlerIndex - 1];
                        Map<String, Object> bowlerStats = new HashMap<>(team2PlayersMap.get(bowlerId));
                        bowlerStats.put("wide_balls", (int) bowlerStats.get("wide_balls") + 1);
                        team2PlayersMap.put(bowlerId, bowlerStats);
                    }
                } else {
                    int bowlerIndex = activeBowlerIndex;
                    if (bowlerIndex != -1) {
                        int bowlerId = (int) team1PlayersMap.keySet().toArray()[bowlerIndex - 1];
                        Map<String, Object> bowlerStats = new HashMap<>(team1PlayersMap.get(bowlerId));
                        bowlerStats.put("wide_balls", (int) bowlerStats.get("wide_balls") + 1);
                        team1PlayersMap.put(bowlerId, bowlerStats);
                    }
                }
            } else if (isNoball.equals("true")) {
                if ("team1".equals(currentBattingTeam)) {
                    int bowlerIndex = activeBowlerIndex;
                    if (bowlerIndex != -1) {
                        int bowlerId = (int) team2PlayersMap.keySet().toArray()[bowlerIndex - 1];
                        Map<String, Object> bowlerStats = new HashMap<>(team2PlayersMap.get(bowlerId));
                        bowlerStats.put("no_balls", (int) bowlerStats.get("no_balls") + 1);
                        team2PlayersMap.put(bowlerId, bowlerStats);
                        updateFreehit((team1_balls + 1), team1Id, Integer.parseInt(matchId), team1FreehitsMap);
                        team1FreehitsMap.add(team1_balls + 1);
                    }
                } else {
                    int bowlerIndex = activeBowlerIndex;
                    if (bowlerIndex != -1) {
                        int bowlerId = (int) team1PlayersMap.keySet().toArray()[bowlerIndex - 1];
                        Map<String, Object> bowlerStats = new HashMap<>(team1PlayersMap.get(bowlerId));
                        bowlerStats.put("no_balls", (int) bowlerStats.get("no_balls") + 1);
                        team1PlayersMap.put(bowlerId, bowlerStats);
                        updateFreehit((team2_balls + 1), team2Id, Integer.parseInt(matchId), team2FreehitsMap);
                        team2FreehitsMap.add(team2_balls + 1);
                    }
                }
                if (!"out".equals(updateRequest) && !"wide".equals(updateRequest)) {
                    if ("team1".equals(currentBattingTeam)) {
                        Map<String, Object> playerStats = new HashMap<>(team1PlayersMap.get(currentPlayerId));
                        playerStats.put("runs", Integer.parseInt(updateRequest) + currentPlayerOldScore);
                        team1PlayersMap.put(currentPlayerId, playerStats);
                        if ("1".equals(updateRequest)) {
                            int temp = activeBatsmanIndex;
                            activeBatsmanIndex = passiveBatsmanIndex;
                            passiveBatsmanIndex = temp;
                        } else if ("4".equals(updateRequest)) {
                            playerStats = new HashMap<>(team1PlayersMap.get(currentPlayerId));
                            playerStats.put("fours", (int) playerStats.get("fours") + 1);
                            team1PlayersMap.put(currentPlayerId, playerStats);
                        } else if ("6".equals(updateRequest)) {
                            playerStats = new HashMap<>(team1PlayersMap.get(currentPlayerId));
                            playerStats.put("sixes", (int) playerStats.get("sixes") + 1);
                            team1PlayersMap.put(currentPlayerId, playerStats);
                        }
                    } else {
                        Map<String, Object> playerStats = new HashMap<>(team2PlayersMap.get(currentPlayerId));
                        playerStats.put("runs", Integer.parseInt(updateRequest) + currentPlayerOldScore);
                        team2PlayersMap.put(currentPlayerId, playerStats);
                        if ("1".equals(updateRequest)) {
                            int temp = activeBatsmanIndex;
                            activeBatsmanIndex = passiveBatsmanIndex;
                            passiveBatsmanIndex = temp;
                        } else if ("4".equals(updateRequest)) {
                            playerStats = new HashMap<>(team2PlayersMap.get(currentPlayerId));
                            playerStats.put("fours", (int) playerStats.get("fours") + 1);
                            team2PlayersMap.put(currentPlayerId, playerStats);
                        } else if ("6".equals(updateRequest)) {
                            playerStats = new HashMap<>(team2PlayersMap.get(currentPlayerId));
                            playerStats.put("sixes", (int) playerStats.get("sixes") + 1);
                            team2PlayersMap.put(currentPlayerId, playerStats);
                        }
                    }
                }
            } else if (updateRequest.equals("out")) {
                if ("team1".equals(currentBattingTeam)) {
                    team1_balls++;
                    Map<String, Object> playerStats = new HashMap<>(team1PlayersMap.get(currentPlayerId));
                    playerStats.put("balls", (int) playerStats.get("balls") + 1);
                    team1PlayersMap.put(currentPlayerId, playerStats);
                } else {
                    team2_balls++;
                    Map<String, Object> playerStats = new HashMap<>(team2PlayersMap.get(currentPlayerId));
                    playerStats.put("balls", (int) playerStats.get("balls") + 1);
                    team2PlayersMap.put(currentPlayerId, playerStats);
                }
                if ("team1".equals(currentBattingTeam)) {
                    if (!team1FreehitsMap.contains(team1_balls)) {
                        int bowlerIndex = activeBowlerIndex;
                        if (bowlerIndex != -1) {
                            int bowlerId = (int) team2PlayersMap.keySet().toArray()[bowlerIndex - 1];
                            wicketer_id = bowlerId;
                            System.out.println("bowlerId: " + bowlerId);
                            System.out.println("bowlerIndex: " + bowlerIndex);
                            Map<String, Object> bowlerStats = new HashMap<>(team2PlayersMap.get(bowlerId));
                            bowlerStats.put("wickets", (int) bowlerStats.get("wickets") + 1);
                            team2PlayersMap.put(bowlerId, bowlerStats);
                            team1WicketsMap.set(activeBatsmanIndex - 1, team2Players.get(bowlerId));
                            activeBatsmanIndex = -1;
                        }
                    }
                } else {
                    if (!team2FreehitsMap.contains(team2_balls)) {
                        int bowlerIndex = activeBowlerIndex;
                        if (bowlerIndex != -1) {
                            int bowlerId = (int) team1PlayersMap.keySet().toArray()[bowlerIndex - 1];
                            wicketer_id = bowlerId;
                            Map<String, Object> bowlerStats = new HashMap<>(team1PlayersMap.get(bowlerId));
                            bowlerStats.put("wickets", (int) bowlerStats.get("wickets") + 1);
                            team1PlayersMap.put(bowlerId, bowlerStats);
                            team2WicketsMap.set(activeBatsmanIndex - 1, team1Players.get(bowlerId));
                            activeBatsmanIndex = -1;
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
                    Map<String, Object> playerStats = new HashMap<>(team1PlayersMap.get(currentPlayerId));
                    playerStats.put("balls", (int) playerStats.get("balls") + 1);
                    team1PlayersMap.put(currentPlayerId, playerStats);
                    if (team1_balls % 6 == 0) {
                        int temp = activeBatsmanIndex;
                        activeBatsmanIndex = passiveBatsmanIndex;
                        passiveBatsmanIndex = temp;
                        activeBowlerIndex = -1;
                    }
                } else {
                    team2_balls++;
                    Map<String, Object> playerStats = new HashMap<>(team2PlayersMap.get(currentPlayerId));
                    playerStats.put("balls", (int) playerStats.get("balls") + 1);
                    team2PlayersMap.put(currentPlayerId, playerStats);
                    if (team2_balls % 6 == 0) {
                        int temp = activeBatsmanIndex;
                        activeBatsmanIndex = passiveBatsmanIndex;
                        passiveBatsmanIndex = temp;
                        activeBowlerIndex = -1;
                    }
                }
                if ("team1".equals(currentBattingTeam)) {
                    Map<String, Object> playerStats = new HashMap<>(team1PlayersMap.get(currentPlayerId));
                    playerStats.put("runs", Integer.parseInt(updateRequest) + currentPlayerOldScore);
                    team1PlayersMap.put(currentPlayerId, playerStats);
                    if ("1".equals(updateRequest)) {
                        int temp = activeBatsmanIndex;
                        activeBatsmanIndex = passiveBatsmanIndex;
                        passiveBatsmanIndex = temp;
                    } else if ("4".equals(updateRequest)) {
                        playerStats = new HashMap<>(team1PlayersMap.get(currentPlayerId));
                        playerStats.put("fours", (int) playerStats.get("fours") + 1);
                        team1PlayersMap.put(currentPlayerId, playerStats);
                    } else if ("6".equals(updateRequest)) {
                        playerStats = new HashMap<>(team1PlayersMap.get(currentPlayerId));
                        playerStats.put("sixes", (int) playerStats.get("sixes") + 1);
                        team1PlayersMap.put(currentPlayerId, playerStats);
                    }
                } else {
                    Map<String, Object> playerStats = new HashMap<>(team2PlayersMap.get(currentPlayerId));
                    playerStats.put("runs", Integer.parseInt(updateRequest) + currentPlayerOldScore);
                    team2PlayersMap.put(currentPlayerId, playerStats);
                    if ("1".equals(updateRequest)) {
                        int temp = activeBatsmanIndex;
                        activeBatsmanIndex = passiveBatsmanIndex;
                        passiveBatsmanIndex = temp;
                    } else if ("4".equals(updateRequest)) {
                        playerStats = new HashMap<>(team2PlayersMap.get(currentPlayerId));
                        playerStats.put("fours", (int) playerStats.get("fours") + 1);
                        team2PlayersMap.put(currentPlayerId, playerStats);
                    } else if ("6".equals(updateRequest)) {
                        playerStats = new HashMap<>(team2PlayersMap.get(currentPlayerId));
                        playerStats.put("sixes", (int) playerStats.get("sixes") + 1);
                        team2PlayersMap.put(currentPlayerId, playerStats);
                    }
                }
            }

            matchStats.put("team1_wickets", String.valueOf(team2PlayersMap.values().stream().mapToInt(player -> (int) player.get("wickets")).sum()));
            matchStats.put("team2_wickets", String.valueOf(team1PlayersMap.values().stream().mapToInt(player -> (int) player.get("wickets")).sum()));

            if ("team1".equals(currentBattingTeam)) {
                if (team1_balls == 120 || matchStats.get("team1_wickets").equals("10")) {
                    matchStats.put("current_batting", "team2");
                    activeBatsmanIndex = -1;
                    passiveBatsmanIndex = -1;
                    activeBowlerIndex = -1;
                }
            } else if ("team2".equals(currentBattingTeam)) {
                int team1Score = team1PlayersMap.values().stream().mapToInt(player -> (int) player.get("runs")).sum() + team2PlayersMap.values().stream().mapToInt(player -> (int) player.get("wide_balls")).sum() + team2PlayersMap.values().stream().mapToInt(player -> (int) player.get("no_balls")).sum();
                int team2Score = team2PlayersMap.values().stream().mapToInt(player -> (int) player.get("runs")).sum() + team1PlayersMap.values().stream().mapToInt(player -> (int) player.get("wide_balls")).sum() + team1PlayersMap.values().stream().mapToInt(player -> (int) player.get("no_balls")).sum();
                if (team2Score > team1Score) {
                    if (tournamentId != 0) {
                        updateTournament(team2Id, tournamentId, userId);
                    }
                    matchStats.put("winner", "team2");
                    matchStats.put("is_completed", "true");
                }
                if (team2_balls == 120 || matchStats.get("team2_wickets").equals("10")) {
                    if (team1Score > team2Score) {
                        if (tournamentId != 0) {
                            updateTournament(team1Id, tournamentId, userId);
                        }
                        matchStats.put("winner", "team1");
                        matchStats.put("is_completed", "true");
                    } else {
                        matchStats.put("winner", "tie");
                        matchStats.put("is_completed", "true");
                    }
                }
            }

            updateMatchStats(matchStats, activeBatsmanIndex, passiveBatsmanIndex, activeBowlerIndex, wicketer_id, currentPlayerId, team1PlayersMap, team2PlayersMap);
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
            matchData.put("team1_score", team1PlayersMap.values().stream().mapToInt(player -> (int) player.get("runs")).sum() + team1_wide_runs + team1_no_balls);
            matchData.put("team2_score", team2PlayersMap.values().stream().mapToInt(player -> (int) player.get("runs")).sum() + team2_wide_runs + team2_no_balls);
            matchData.put("team1_wickets", team1PlayersMap.values().stream().mapToInt(player -> (int) player.get("wickets")).sum());
            matchData.put("team2_wickets", team2PlayersMap.values().stream().mapToInt(player -> (int) player.get("wickets")).sum());
            matchData.put("team1_runs", team1PlayersMap.values().stream().map(player -> (int) player.get("runs")).collect(Collectors.toList()));
            matchData.put("team2_runs", team2PlayersMap.values().stream().map(player -> (int) player.get("runs")).collect(Collectors.toList()));
            matchData.put("team1_outs", team1PlayersMap.values().stream().map(player -> (int) player.get("wickets")).collect(Collectors.toList()));
            matchData.put("team2_outs", team2PlayersMap.values().stream().map(player -> (int) player.get("wickets")).collect(Collectors.toList()));
            matchData.put("team1_balls", team1_balls);
            matchData.put("team2_balls", team2_balls);
            matchData.put("team1_wickets_map", team1WicketsMap);
            matchData.put("team2_wickets_map", team2WicketsMap);
            matchData.put("team1_balls_map", team1PlayersMap.values().stream().map(player -> (int) player.get("balls")).collect(Collectors.toList()));
            matchData.put("team2_balls_map", team2PlayersMap.values().stream().map(player -> (int) player.get("balls")).collect(Collectors.toList()));
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

    private void updateMatchStats(Map<String, String> matchStats, int activeBatsmanIndex, int passiveBatsmanIndex, int activeBowlerIndex, int wicketer_id, int currentPlayerId, Map<Integer, Map<String, Object>> team1PlayersMap, Map<Integer, Map<String, Object>> team2PlayersMap) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = new Database().getConnection();
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
            for (Map.Entry<Integer, Map<String, Object>> entry : team1PlayersMap.entrySet()) {
                stmt.setInt(1, (int) entry.getValue().get("runs"));
                stmt.setInt(2, (int) entry.getValue().get("balls"));
                stmt.setInt(3, (int) entry.getValue().get("wickets"));
                stmt.setInt(4, (int) entry.getValue().get("wide_balls"));
                stmt.setInt(5, (int) entry.getValue().get("no_balls"));
                stmt.setInt(6, (int) entry.getValue().get("fours"));
                stmt.setInt(7, (int) entry.getValue().get("sixes"));
                stmt.setInt(8, Integer.parseInt(matchStats.get("id")));
                stmt.setInt(9, entry.getKey());
                stmt.addBatch();
                i++;
            }

            i = 0;
            for (Map.Entry<Integer, Map<String, Object>> entry : team2PlayersMap.entrySet()) {
                stmt.setInt(1, (int) entry.getValue().get("runs"));
                stmt.setInt(2, (int) entry.getValue().get("balls"));
                stmt.setInt(3, (int) entry.getValue().get("wickets"));
                stmt.setInt(4, (int) entry.getValue().get("wide_balls"));
                stmt.setInt(5, (int) entry.getValue().get("no_balls"));
                stmt.setInt(6, (int) entry.getValue().get("fours"));
                stmt.setInt(7, (int) entry.getValue().get("sixes"));
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
            conn = new Database().getConnection();
            if (ballList.contains(ballCount)) {
                return;
            }
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

    private Map<Integer, Map<String, Object>> sortHashMapWithKeys(Map<Integer, Map<String, Object>> map) {
        List<Map.Entry<Integer, Map<String, Object>>> list = new ArrayList<>(map.entrySet());
        list.sort(Comparator.comparingInt(Map.Entry::getKey));
        Map<Integer, Map<String, Object>> sortedMap = new LinkedHashMap<>();

        for (Map.Entry<Integer, Map<String, Object>> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    private void updateTournament(int winningTeamId, int tournamentId, int userId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = new Database().getConnection();
            String query = "SELECT team_id FROM tournament_winners WHERE tournament_id = ?";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, tournamentId);

            rs = stmt.executeQuery();
            if (rs.next()) {
                int oldWinnerId = rs.getInt("team_id");
                Match.create(conn, userId, oldWinnerId, winningTeamId, tournamentId);
                conn = new Database().getConnection();
                query = "DELETE FROM tournament_winners WHERE tournament_id = ?";
                stmt = conn.prepareStatement(query);
                stmt.setInt(1, tournamentId);
                stmt.executeUpdate();
                MatchListener.fireMatchesUpdate(userId);
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
                if (conn != null) conn.close();
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
            conn = new Database().getConnection();
            stmt = conn.prepareStatement("SELECT name, logo FROM teams WHERE id = ?");
            stmt.setInt(1, teamId);
            rs = stmt.executeQuery();

            String teamName = "";
            String teamLogo = "";
            Map<String, Object> highestRuns = new HashMap<>();
            Map<String, Object> highestSixes = new HashMap<>();
            Map<String, Object> highestWickets = new HashMap<>();

            if (rs.next()) {
                teamName = rs.getString("name");
                teamLogo = rs.getString("logo");
            }

            /*
            WITH max_stats AS (
                SELECT MAX(runs) AS max_runs,
                    MAX(sixes) AS max_sixes,
                    MAX(wickets) AS max_wickets
                FROM player_stats
                WHERE match_id = 54 AND team_id = 12
            )

            SELECT ps.sixes, ps.runs, ps.wickets, p.name, p.avatar
            FROM player_stats ps
            JOIN players p ON p.id = ps.player_id
            JOIN max_stats ms ON
                ps.match_id = 54
                AND ps.team_id = 12
                AND (
                    ps.runs = ms.max_runs OR
                    ps.sixes = ms.max_sixes OR
                    ps.wickets = ms.max_wickets
                );
                 */
            String query = "WITH max_stats AS " +
                    "(SELECT MAX(runs) AS max_runs, MAX(sixes) AS max_sixes, MAX(wickets) AS max_wickets FROM player_stats " +
                    "WHERE match_id = ? AND team_id = ?) " +
                    "SELECT ps.sixes, ps.runs, ps.wickets, p.name, p.avatar FROM player_stats ps " +
                    "JOIN players p ON p.id = ps.player_id " +
                    "JOIN max_stats ms ON ps.match_id = ? AND ps.team_id = ? AND " +
                    "(ps.runs = ms.max_runs OR ps.sixes = ms.max_sixes OR ps.wickets = ms.max_wickets)";

            stmt = conn.prepareStatement(query);
            stmt.setInt(1, matchId);
            stmt.setInt(2, teamId);
            stmt.setInt(3, matchId);
            stmt.setInt(4, teamId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                if ((int) highestRuns.getOrDefault("runs", 0) < rs.getInt("runs")) {
                    highestRuns.put("name", rs.getString("name"));
                    highestRuns.put("avatar", rs.getString("avatar"));
                    highestRuns.put("runs", rs.getInt("runs"));
                }

                if ((int) highestSixes.getOrDefault("sixes", 0) < rs.getInt("sixes")) {
                    highestSixes.put("name", rs.getString("name"));
                    highestSixes.put("avatar", rs.getString("avatar"));
                    highestSixes.put("sixes", rs.getInt("sixes"));
                }

                if ((int) highestWickets.getOrDefault("wickets", 0) < rs.getInt("wickets")) {
                    highestWickets.put("name", rs.getString("name"));
                    highestWickets.put("avatar", rs.getString("avatar"));
                    highestWickets.put("wickets", rs.getInt("wickets"));
                }
            }

            System.out.println("Highest Runs: " + highestRuns);
            System.out.println("Highest Sixes: " + highestSixes);
            System.out.println("Highest Wickets: " + highestWickets);

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
            String winnerText = "Winners: " + teamName + " Team";
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
            if (!highestSixes.getOrDefault("sixes", 0).equals(0)) {
                BufferedImage highestSixesPlayerImage = ImageIO.read(new File(PLAYER_IMAGE_PATH + highestSixes.get("avatar")));
                g2d.drawImage(highestSixesPlayerImage, startX, textY, avatarSize, avatarSize, null);
                String text = highestSixes.get("name") + " (" + highestSixes.get("sixes") + ")";
                int textX = startX + (avatarSize - g2d.getFontMetrics().stringWidth(text)) / 2;

                g2d.setColor(Color.GRAY);
                g2d.drawString(text, textX + 2, textY + avatarSize + 32);

                g2d.setColor(Color.WHITE);
                g2d.drawString(text, textX, textY + avatarSize + 30);
            }

            if (!highestRuns.getOrDefault("runs", 0).equals(0)) {
                BufferedImage highestRunsPlayerImage = ImageIO.read(new File(PLAYER_IMAGE_PATH + highestRuns.get("avatar")));
                int avatarX = startX + avatarSize + avatarSpacing;
                g2d.drawImage(highestRunsPlayerImage, avatarX, textY, avatarSize, avatarSize, null);
                String text = highestRuns.get("name") + " (" + highestRuns.get("runs") + ")";
                int textX = avatarX + (avatarSize - g2d.getFontMetrics().stringWidth(text)) / 2;

                g2d.setColor(Color.GRAY);
                g2d.drawString(text, textX + 2, textY + avatarSize + 32);

                g2d.setColor(Color.WHITE);
                g2d.drawString(text, textX, textY + avatarSize + 30);
            }

            if (!highestWickets.getOrDefault("wickets", 0).equals(0)) {
                BufferedImage highestWicketsPlayerImage = ImageIO.read(new File(PLAYER_IMAGE_PATH + highestWickets.get("avatar")));
                int avatarX = startX + 2 * (avatarSize + avatarSpacing);
                g2d.drawImage(highestWicketsPlayerImage, avatarX, textY, avatarSize, avatarSize, null);
                String text = highestWickets.get("name") + " (" + highestWickets.get("wickets") + ")";
                int textX = avatarX + (avatarSize - g2d.getFontMetrics().stringWidth(text)) / 2;

                g2d.setColor(Color.GRAY);
                g2d.drawString(text, textX + 2, textY + avatarSize + 32);

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