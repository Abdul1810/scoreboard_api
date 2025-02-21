package com.api.scoreboard.match;

import com.api.util.Database;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebListener;
import jakarta.websocket.Session;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebListener
public class MatchListener {
    private static final Map<String, Session> matchSessions = new HashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void addSession(String sessionId, Session session) {
        matchSessions.put(sessionId, session);
        List<Map<String, String>> matches = fetchMatchesFromDatabase();

        try {
            session.getBasicRemote().sendText(objectMapper.writeValueAsString(matches));
        } catch (IOException e) {
            System.out.println("error" + e.getMessage());
        }
    }

    public static void removeSession(String sessionId) {
        matchSessions.remove(sessionId);
    }

    public static void fireMatchesUpdate() {
        sendMatchesToAllSessions();
    }

    private static void sendMatchesToAllSessions() {
        List<Map<String, String>> matches = fetchMatchesFromDatabase();
        List<Session> sendingSessions = new ArrayList<>(matchSessions.values());
        while (!sendingSessions.isEmpty()) {
            List<Session> toRemove = new ArrayList<>();
            for (Session session : sendingSessions) {
                try {
                    System.out.println("Send content to session: " + session.getId());
                    if (!session.isOpen()) {
                        toRemove.add(session);
                        continue;
                    }
                    session.getBasicRemote().sendText(objectMapper.writeValueAsString(matches));
                    toRemove.add(session);
                } catch (Exception e) {
                    System.out.println("Error sending content to session: " + session.getId());
                }
            }
            sendingSessions.removeAll(toRemove);
        }
    }

    private static List<Map<String, String>> fetchMatchesFromDatabase() {
        List<Map<String, String>> matches = new ArrayList<>();
        String query = "SELECT id, team1, team2 FROM matches";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = Database.getConnection();

            stmt = conn.prepareStatement(query);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, String> match = new HashMap<>();
                match.put("id", rs.getString("id"));
                match.put("team1", rs.getString("team1"));
                match.put("team2", rs.getString("team2"));
                matches.add(match);
            }
        } catch (Exception e) {
            System.err.println("Error fetching matches from database: " + e.getMessage());
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
            } catch (Exception e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }

        return matches;
    }
}
