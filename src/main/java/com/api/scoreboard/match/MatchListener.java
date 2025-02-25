package com.api.scoreboard.match;

import com.api.util.Database;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.Session;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
                    if (session == null || !session.isOpen()) {
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
        String query = "SELECT * FROM matches";
        Connection conn = null;
        PreparedStatement stmt = null;
        PreparedStatement stmt1 = null;
        PreparedStatement stmt2 = null;
        ResultSet rs = null;
        ResultSet rs1 = null;
        ResultSet rs2 = null;

        try {
            conn = Database.getConnection();
            stmt = conn.prepareStatement(query);
            rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, String> match = new HashMap<>();
                match.put("id", String.valueOf(rs.getInt("id")));

                conn = Database.getConnection();
                query = "SELECT * FROM teams WHERE id = ?";
                stmt1 = conn.prepareStatement(query);
                stmt1.setInt(1, rs.getInt("team1_id"));
                rs1 = stmt1.executeQuery();

                if (rs1.next()) {
                    match.put("team1", rs1.getString("name"));
                }

                conn = Database.getConnection();

                stmt2 = conn.prepareStatement(query);
                stmt2.setInt(1, rs.getInt("team2_id"));
                rs2 = stmt2.executeQuery();

                if (rs2.next()) {
                    match.put("team2", rs2.getString("name"));
                }

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
                if (rs1 != null) {
                    rs1.close();
                }
                if (stmt1 != null) {
                    stmt1.close();
                }
                if (rs2 != null) {
                    rs2.close();
                }
                if (stmt2 != null) {
                    stmt2.close();
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
