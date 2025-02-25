package com.api.scoreboard;

import com.api.util.Database;
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

    public static void addSession(String matchId, Session session) {
        System.out.println("Adding session: " + session.getId());
        System.out.println("For match: " + matchId);
        sessions.put(session.getId(), session);
        matchSessions.putIfAbsent(matchId, new CopyOnWriteArrayList<>());
        matchSessions.get(matchId).add(session.getId());

        Map<String, String> matchStats = fetchMatchStatsFromDatabase(matchId);
        try {
            session.getBasicRemote().sendText(objectMapper.writeValueAsString(matchStats));
        } catch (IOException e) {
            System.out.println("Error sending stats: " + e.getMessage());
        }
    }

    public static void removeSession(String matchId, Session session) {
        sessions.remove(session.getId());
        matchSessions.getOrDefault(matchId, new CopyOnWriteArrayList<>()).remove(session.getId());
    }

    public static void fireStatsUpdate(String matchId) {
        sendStatsToAllSessions(matchId);
    }

    public static void fireStatsRemove(String matchId) {
        System.out.println("Match completed: " + matchId);
        System.out.println(sessions);
        System.out.println(matchSessions);
        if (matchSessions.get(matchId) != null) matchSessions.get(matchId).forEach(System.out::println);
        disconnectAllSessions(matchId);
    }

    private static void sendStatsToAllSessions(String matchId) {
        Map<String, String> matchStats = fetchMatchStatsFromDatabase(matchId);
        List<String> sendingSessions = new ArrayList<>(matchSessions.getOrDefault(matchId, new CopyOnWriteArrayList<>()));

        for (String sessionId : sendingSessions) {
            try {
                Session session = sessions.get(sessionId);
                if (session != null && session.isOpen()) {
                    session.getBasicRemote().sendText(objectMapper.writeValueAsString(matchStats));
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

    private static Map<String, String> fetchMatchStatsFromDatabase(String matchId) {
        Map<String, String> matchStats = new HashMap<>();
        String query = "SELECT team1_score, team2_score, team1_wickets, team2_wickets, " + "team1_balls, team2_balls, current_batting, is_completed, winner " + "FROM match_stats WHERE match_id = ?";

        if (matchId == null || matchId.trim().isEmpty()) {
            System.err.println("Invalid match ID provided.");
            return matchStats;
        }

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = Database.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, matchId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    matchStats.put("team1_score", rs.getString("team1_score"));
                    matchStats.put("team2_score", rs.getString("team2_score"));
                    matchStats.put("team1_wickets", rs.getString("team1_wickets"));
                    matchStats.put("team2_wickets", rs.getString("team2_wickets"));
                    matchStats.put("team1_balls", rs.getString("team1_balls"));
                    matchStats.put("team2_balls", rs.getString("team2_balls"));
                    matchStats.put("current_batting", rs.getString("current_batting"));
                    matchStats.put("is_completed", rs.getString("is_completed"));
                    matchStats.put("winner", rs.getString("winner"));
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching match stats from database: " + e.getMessage());
        } finally {
            try {
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
        return matchStats;
    }
}
