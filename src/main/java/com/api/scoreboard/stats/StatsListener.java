package com.api.scoreboard.stats;

import com.api.util.Database;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.Session;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class StatsListener {
    private static final Map<String, Session> sessions = new HashMap<>();
    private static final Map<String, CopyOnWriteArrayList<String>> matchSessions = new HashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
//    private static final Map<String, String> matchStatsForSessions = new HashMap<>();

    public static void removeSession(String matchId, Session session) {
        sessions.remove(session.getId());
        matchSessions.getOrDefault(matchId, new CopyOnWriteArrayList<>()).remove(session.getId());
    }

    public static void fireStatsUpdate(String matchId) {
        String matchData = fetchMatchStatsFromDatabase(matchId);
        sendStatsToAllSessions(matchId, matchData);
    }

    public static void fireStatsUpdate(String matchId, String matchData) {
//        matchStatsForSessions.put(matchId, matchData);
        sendStatsToAllSessions(matchId, matchData);
    }

    public static void fireStatsRemove(String matchId) {
        System.out.println("Remove session with matchId: " + matchId);
        System.out.println(sessions);
        System.out.println(matchSessions);
        if (matchSessions.get(matchId) != null) matchSessions.get(matchId).forEach(System.out::println);
        disconnectAllSessions(matchId);
    }

    private static void sendStatsToAllSessions(String matchId, String matchData) {
        List<String> sendingSessions = new ArrayList<>(matchSessions.getOrDefault(matchId, new CopyOnWriteArrayList<>()));

        for (String sessionId : sendingSessions) {
            try {
                Session session = sessions.get(sessionId);
                if (session != null && session.isOpen()) {
                    session.getBasicRemote().sendText(matchData);
                } else {
                    matchSessions.get(matchId).remove(sessionId);
                }
            } catch (Exception e) {
                System.out.println("Error sending content to session: " + sessionId);
            }
        }
    }

    private static void disconnectAllSessions(String matchId) {
        CopyOnWriteArrayList<String> sendingSessions = matchSessions.get(matchId);
        if (sendingSessions == null) {
            return;
        }

        for (String sessionId : new ArrayList<>(sendingSessions)) {
            try {
                Session session = sessions.get(sessionId);
                if (session != null && session.isOpen()) {
                    session.close();
                }
                sendingSessions.remove(sessionId);
            } catch (IOException e) {
                System.out.println("Error closing session: " + sessionId);
            }
        }

        matchSessions.put(matchId, new CopyOnWriteArrayList<>());
    }

    public static void addSession(String matchId, Session session) {
        System.out.println("Total sessions: " + sessions.size());
        System.out.println("For match: " + matchId);
        sessions.put(session.getId(), session);
        matchSessions.putIfAbsent(matchId, new CopyOnWriteArrayList<>());
        matchSessions.get(matchId).add(session.getId());

//        String matchStats = matchStatsForSessions.get(matchId);
//        if (matchStats == null) {
//            matchStats = fetchMatchStatsFromDatabase(matchId);
//            matchStatsForSessions.put(matchId, matchStats);
//        }
        String matchStats = fetchMatchStatsFromDatabase(matchId);
        if (matchStats == null || matchStats.isEmpty() || matchStats.equals("{}")) {
            try {
                session.getBasicRemote().sendText("not-found");
            } catch (IOException e) {
                System.out.println("Error closing session: " + e.getMessage());
            }
        }
        System.out.println("Sending stats: " + matchStats);
        try {
            session.getBasicRemote().sendText(matchStats);
        } catch (IOException e) {
            System.out.println("Error sending stats: " + e.getMessage());
        }
    }

    private static String fetchMatchStatsFromDatabase(String matchId) {
        Map<String, Object> matchStats = new HashMap<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = Database.getConnection();
            String query = "SELECT * FROM matches WHERE id = ?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, matchId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                int team1Id = rs.getInt("team1_id");
                int team2Id = rs.getInt("team2_id");

                matchStats.put("current_batting", rs.getString("current_batting"));
                matchStats.put("is_completed", rs.getString("is_completed"));
                matchStats.put("highlights_path", rs.getString("highlights_path"));
                matchStats.put("winner", rs.getString("winner"));
                matchStats.put("active_batsman_index", rs.getInt("active_batsman_index"));
                matchStats.put("passive_batsman_index", rs.getInt("passive_batsman_index"));
                matchStats.put("active_bowler_index", rs.getInt("active_bowler_index"));

                query = "SELECT ps.player_id, ps.runs, ps.wickets, ps.team_id, ps.wicketer_id, ps.balls, ps.wide_balls, ps.no_balls, " +
                        "w.name AS wicketer_name " +
                        "FROM player_stats ps " +
                        "JOIN team_players tp ON ps.player_id = tp.player_id " +
                        "LEFT JOIN players w ON ps.wicketer_id = w.id " +
                        "WHERE ps.match_id = ? AND ps.team_id IN (?, ?)";

                stmt = conn.prepareStatement(query);
                stmt.setString(1, matchId);
                stmt.setInt(2, team1Id);
                stmt.setInt(3, team2Id);
                rs = stmt.executeQuery();

                Map<Integer, Integer> team1Runs = new HashMap<>();
                Map<Integer, Integer> team2Runs = new HashMap<>();
                Map<Integer, Integer> team1Wickets = new HashMap<>();
                Map<Integer, Integer> team2Wickets = new HashMap<>();
                int team1_balls = 0;
                int team2_balls = 0;
                int team1_wides = 0;
                int team2_wides = 0;
                int team1_no_balls = 0;
                int team2_no_balls = 0;
                List<String> team1WicketsMap = new ArrayList<>();
                List<String> team2WicketsMap = new ArrayList<>();
                List<String> team1BallsMap = new ArrayList<>();
                List<String> team2BallsMap = new ArrayList<>();
                List<Integer> team1Freehits = new ArrayList<>();
                List<Integer> team2Freehits = new ArrayList<>();
                List<Integer> team1BattingOrder = new ArrayList<>();
                List<Integer> team2BattingOrder = new ArrayList<>();
                List<Integer> team1BowlingOrder = new ArrayList<>();
                List<Integer> team2BowlingOrder = new ArrayList<>();

                while (rs.next()) {
                    int playerId = rs.getInt("player_id");
                    int runs = rs.getInt("runs");
                    int balls = rs.getInt("balls");
                    int wickets = rs.getInt("wickets");
                    int teamId = rs.getInt("team_id");
                    String wicketerName = rs.getString("wicketer_name");

                    if (teamId == team1Id) {
                        team1Runs.put(playerId, runs);
                        team1Wickets.put(playerId, wickets);
                        team1BallsMap.add(String.valueOf(balls));
                        team1_balls += balls;
                        team1_wides += rs.getInt("wide_balls");
                        team1_no_balls += rs.getInt("no_balls");
                        team1WicketsMap.add(wicketerName);
                    } else if (teamId == team2Id) {
                        team2Runs.put(playerId, runs);
                        team2Wickets.put(playerId, wickets);
                        team2BallsMap.add(String.valueOf(balls));
                        team2_balls += balls;
                        team2_wides += rs.getInt("wide_balls");
                        team2_no_balls += rs.getInt("no_balls");
                        team2WicketsMap.add(wicketerName);
                    }
                }

                query = "SELECT free_hit_balls, batting_order, bowling_order, team_id FROM team_order WHERE match_id = ?";
                stmt = conn.prepareStatement(query);
                stmt.setString(1, matchId);
                rs = stmt.executeQuery();

                while (rs.next()) {
                    if (team1Id == rs.getInt("team_id")) {
                        team1Freehits = objectMapper.readValue(rs.getString("free_hit_balls"), ArrayList.class);
                        team1BattingOrder = objectMapper.readValue(rs.getString("batting_order"), ArrayList.class);
                        team1BowlingOrder = objectMapper.readValue(rs.getString("bowling_order"), ArrayList.class);
                    } else if (team2Id == rs.getInt("team_id")) {
                        team2Freehits = objectMapper.readValue(rs.getString("free_hit_balls"), ArrayList.class);
                        team2BattingOrder = objectMapper.readValue(rs.getString("batting_order"), ArrayList.class);
                        team2BowlingOrder = objectMapper.readValue(rs.getString("bowling_order"), ArrayList.class);
                    }
                }

                matchStats.put("is_highlights_uploaded", matchStats.get("highlights_path") == null && Objects.equals(matchStats.get("is_completed"), "true") ? "false" : "true");
                matchStats.put("team1_score", team1Runs.values().stream().mapToInt(Integer::intValue).sum() + team2_wides + team2_no_balls);
                matchStats.put("team2_score", team2Runs.values().stream().mapToInt(Integer::intValue).sum() + team1_wides + team1_no_balls);
                matchStats.put("team1_wickets", team1Wickets.values().stream().mapToInt(Integer::intValue).sum());
                matchStats.put("team2_wickets", team2Wickets.values().stream().mapToInt(Integer::intValue).sum());
                matchStats.put("team1_runs", team1Runs);
                matchStats.put("team2_runs", team2Runs);
                matchStats.put("team1_outs", team1Wickets);
                matchStats.put("team2_outs", team2Wickets);
                matchStats.put("team1_balls", team1_balls);
                matchStats.put("team2_balls", team2_balls);
                matchStats.put("team1_wickets_map", team1WicketsMap);
                matchStats.put("team2_wickets_map", team2WicketsMap);
                matchStats.put("team1_balls_map", team1BallsMap);
                matchStats.put("team2_balls_map", team2BallsMap);
                matchStats.put("team1_freehits_map", team1Freehits);
                matchStats.put("team2_freehits_map", team2Freehits);
                matchStats.put("team1_batting_order", team1BattingOrder);
                matchStats.put("team2_batting_order", team2BattingOrder);
                matchStats.put("team1_bowling_order", team1BowlingOrder);
                matchStats.put("team2_bowling_order", team2BowlingOrder);
            }
        } catch (Exception e) {
            System.out.println("Error fetching match stats: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        try {
            System.out.println("Match stats: " + matchStats);
            return objectMapper.writeValueAsString(matchStats);
        } catch (JsonProcessingException e) {
            System.out.println("Error converting to JSON: " + e.getMessage());
            return null;
        }
    }
}
