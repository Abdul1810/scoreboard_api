package com.api.scoreboard;

import com.api.util.Database;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;
import jakarta.websocket.Session;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebListener
public class StatsListener {
    private static final Map<String, Session> sessions = new HashMap<>();
    private static final Map<String, List<String>> matchSessions = new HashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void addSession(String matchId, Session session) {
        sessions.put(session.getId(), session);
        List<String> sessions = matchSessions.get(matchId);
        if (sessions == null) {
            sessions = new ArrayList<>();
        }
        sessions.add(session.getId());
        matchSessions.put(matchId, sessions);

        Map<String, String> matchStats = fetchMatchStatsFromDatabase(matchId);
        try {
            session.getBasicRemote().sendText(objectMapper.writeValueAsString(matchStats));
        } catch (IOException e) {
            System.out.println("error" + e.getMessage());
        }
    }

    public static void removeSession(String matchId, Session session) {
        sessions.remove(session.getId());
        List<String> sessions = matchSessions.get(matchId);
        sessions.remove(session.getId());
        matchSessions.put(matchId, sessions);
    }

    public static void fireStatsUpdate(String matchId) {
        sendStatsToALlSessions(matchId);
    }

    public static void fireStatsRemove(String matchId) {
        disconnectAllSessions(matchId);
    }

    private static void sendStatsToALlSessions(String matchId) {
        Map<String, String> matchStats = fetchMatchStatsFromDatabase(matchId);
        System.out.println("Send stats to all sessions for match: " + matchId);
        List<String> sendingSessions = new ArrayList<>(matchSessions.get(matchId));
        System.out.println("Sending to sessions: " + sendingSessions);
        System.out.println("sessions: " + sessions);
        while (!sendingSessions.isEmpty()) {
            List<String> toRemove = new ArrayList<>();
            for (String sessionId : sendingSessions) {
                try {
                    System.out.println("Send content to session: " + sessionId);
                    Session session = sessions.get(sessionId);
                    if (!session.isOpen()) {
                        toRemove.add(sessionId);
                        continue;
                    }
                    session.getBasicRemote().sendText(objectMapper.writeValueAsString(matchStats));
                    toRemove.add(sessionId);
                } catch (Exception e) {
                    System.out.println("Error sending content to session: " + sessionId);
                }
            }
            sendingSessions.removeAll(toRemove);
        }
    }

    private static void disconnectAllSessions(String matchId) {
        List<String> sessions = matchSessions.get(matchId);
        System.out.println("Disconnecting all sessions for match: " + matchId);
        System.out.println("Sessions: " + sessions);
        for (String sessionId : sessions) {
            try {
                Session session = StatsListener.sessions.get(sessionId);
                if (session != null && session.isOpen()) {
                    session.close();
                }
            } catch (IOException e) {
                System.out.println("Error closing session: " + sessionId);
            }
        }
    }

    private static Map<String, String> fetchMatchStatsFromDatabase(String matchId) {
        Map<String, String> matchStats = new HashMap<>();
        String query = "SELECT team1, team2, team1_wickets, team2_wickets, " +
                "team1_balls, team2_balls, current_batting, is_completed, winner " +
                "FROM match_stats WHERE match_id = ?";

        if (matchId == null || matchId.trim().isEmpty()) {
            System.err.println("Invalid match ID provided.");
            return matchStats;
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = Database.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, matchId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                matchStats.put("team1", rs.getString("team1"));
                matchStats.put("team2", rs.getString("team2"));
                matchStats.put("team1_wickets", rs.getString("team1_wickets"));
                matchStats.put("team2_wickets", rs.getString("team2_wickets"));
                matchStats.put("team1_balls", rs.getString("team1_balls"));
                matchStats.put("team2_balls", rs.getString("team2_balls"));
                matchStats.put("current_batting", rs.getString("current_batting"));
                matchStats.put("is_completed", rs.getString("is_completed"));
                matchStats.put("winner", rs.getString("winner"));
            }
        } catch (Exception e) {
            System.err.println("Error fetching match stats from database: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }

        return matchStats;
    }
}
