package com.api.scoreboard;

import com.api.util.Database;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.Session;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class StatsListener {
    private static final Map<String, Session> sessions = new HashMap<>();
    private static final Map<String, CopyOnWriteArrayList<String>> matchSessions = new HashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void addSession(String matchId, Session session) {
        System.out.println("Adding session: " + session.getId());
        System.out.println("For match: " + matchId);
        sessions.put(session.getId(), session);
        matchSessions.putIfAbsent(matchId, new CopyOnWriteArrayList<>());
        matchSessions.get(matchId).add(session.getId());

        String matchStats = fetchMatchStatsFromDatabase(matchId);
        System.out.println("Sending stats: " + matchStats);
        try {
            session.getBasicRemote().sendText(matchStats);
        } catch (IOException e) {
            System.out.println("Error sending stats: " + e.getMessage());
        }
    }

    public static void removeSession(String matchId, Session session) {
        sessions.remove(session.getId());
        matchSessions.getOrDefault(matchId, new CopyOnWriteArrayList<>()).remove(session.getId());
    }

    public static void fireStatsUpdate(String matchId, String matchData) {
        sendStatsToAllSessions(matchId, matchData);
    }

    public static void fireStatsRemove(String matchId) {
        System.out.println("Match completed: " + matchId);
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

    private static String fetchMatchStatsFromDatabase(String matchId) {
        Map<String, Object> matchStats = new HashMap<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = Database.getConnection();
            String query = "SELECT ms.*, ts1.*, ts2.* " +
                    "FROM match_stats ms " +
                    "JOIN team_stats ts1 ON ms.team1_stats_id = ts1.id " +
                    "JOIN team_stats ts2 ON ms.team2_stats_id = ts2.id " +
                    "WHERE ms.match_id = ?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, matchId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                List<Integer> team1Runs = fetchPlayerScores(rs, "ts1");
                List<Integer> team2Runs = fetchPlayerScores(rs, "ts2");
                List<Integer> team1Wickets = fetchPlayerWickets(rs, "ts1");
                List<Integer> team2Wickets = fetchPlayerWickets(rs, "ts2");

                matchStats.put("team1_score", rs.getInt("ts1.total_score"));
                matchStats.put("team2_score", rs.getInt("ts2.total_score"));
                matchStats.put("team1_wickets", rs.getInt("ts1.total_wickets"));
                matchStats.put("team2_wickets", rs.getInt("ts2.total_wickets"));
                matchStats.put("team1_balls", rs.getInt("ts1.balls"));
                matchStats.put("team2_balls", rs.getInt("ts2.balls"));
                matchStats.put("team1_runs", team1Runs);
                matchStats.put("team2_runs", team2Runs);
                matchStats.put("team1_outs", team1Wickets);
                matchStats.put("team2_outs", team2Wickets);
                matchStats.put("current_batting", rs.getString("ms.current_batting"));
                matchStats.put("is_completed", rs.getString("ms.is_completed"));
                matchStats.put("winner", rs.getString("ms.winner"));
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
            return objectMapper.writeValueAsString(matchStats);
        } catch (JsonProcessingException e) {
            System.out.println("Error converting to JSON: " + e.getMessage());
            return "{}";
        }
    }

    private static List<Integer> fetchPlayerScores(ResultSet rs, String tableAlias) throws SQLException {
        List<Integer> scores = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            scores.add(rs.getInt(tableAlias + ".player" + i + "_runs"));
        }
        return scores;
    }

    private static List<Integer> fetchPlayerWickets(ResultSet rs, String tableAlias) throws SQLException {
        List<Integer> wickets = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            wickets.add(rs.getInt(tableAlias + ".player" + i + "_wickets"));
        }
        return wickets;
    }
}
