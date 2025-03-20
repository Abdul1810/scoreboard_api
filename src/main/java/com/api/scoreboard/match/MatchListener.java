package com.api.scoreboard.match;

import com.api.util.Database;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.Session;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class MatchListener {
    private static final Map<String, Session> sessions = new HashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Map<String, CopyOnWriteArrayList<Session>> userSessions = new HashMap<>();

    public static void addSession(String sessionId, Session session, int userId) {
        sessions.put(sessionId, session);
        List<Map<String, String>> matches = fetchMatchesFromDatabase(userId);
        // userId: [session1, session2, ...]
        if (userSessions.containsKey(String.valueOf(userId))) {
            userSessions.get(String.valueOf(userId)).add(session);
        } else {
            CopyOnWriteArrayList<Session> userSessionList = new CopyOnWriteArrayList<>();
            userSessionList.add(session);
            userSessions.put(String.valueOf(userId), userSessionList);
        }

        try {
            session.getBasicRemote().sendText(objectMapper.writeValueAsString(matches));
        } catch (IOException e) {
            System.out.println("error" + e.getMessage());
        }
    }

    public static void removeSession(String sessionId) {
        sessions.remove(sessionId);
    }

    public static void fireMatchesUpdate(int userId) {
        sendMatchesToAllSessions(userId);
    }

    private static void sendMatchesToAllSessions(int userId) {
        List<Session> sendingSessions = new ArrayList<>(userSessions.get(String.valueOf(userId)));
        while (!sendingSessions.isEmpty()) {
            List<Session> toRemove = new ArrayList<>();
            for (Session session : sendingSessions) {
                List<Map<String, String>> matches = fetchMatchesFromDatabase(userId);
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

    private static void sendMatchesToAllSessions() {
        List<Session> sendingSessions = new ArrayList<>(sessions.values());
        while (!sendingSessions.isEmpty()) {
            List<Session> toRemove = new ArrayList<>();
            for (Session session : sendingSessions) {
                int userId = Integer.parseInt(session.getRequestParameterMap().get("userId").get(0));
                List<Map<String, String>> matches = fetchMatchesFromDatabase(userId);
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

    private static List<Map<String, String>> fetchMatchesFromDatabase(int userId) {
        List<Map<String, String>> matches = new ArrayList<>();
        String query = "SELECT m.id, t1.name AS team1, t2.name AS team2, m.created_at, " +
                "m.is_completed, m.highlights_path, m.winner, " +
                "CASE " +
                "   WHEN m.winner = 'team1' THEN t1.name " +
                "   WHEN m.winner = 'team2' THEN t2.name " +
                "   WHEN m.winner = 'tie' THEN 'Match Tied' " +
                "   ELSE 'Not decided' " +
                "END AS winner_name " +
                "FROM matches m " +
                "JOIN teams t1 ON m.team1_id = t1.id " +
                "JOIN teams t2 ON m.team2_id = t2.id " +
                "WHERE m.user_id = ? " +
                "ORDER BY m.id";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = new Database().getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, String> match = new HashMap<>();
                match.put("id", String.valueOf(rs.getInt("id")));
                match.put("team1", rs.getString("team1"));
                match.put("team2", rs.getString("team2"));
                match.put("date", new SimpleDateFormat("dd-MM-yyyy").format(rs.getTimestamp("created_at")));
                match.put("highlights_path", rs.getString("highlights_path"));
                match.put("is_completed", rs.getString("is_completed"));
                match.put("winner", rs.getString("winner_name"));

                matches.add(match);
            }
        } catch (Exception e) {
            System.out.println("Error fetching matches from database: " + e.getMessage());
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
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
        return matches;
    }
}
