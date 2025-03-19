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
                "ORDER BY m.id ASC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = new Database().getConnection();
            stmt = conn.prepareStatement(query);
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
