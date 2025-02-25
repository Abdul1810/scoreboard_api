package com.api.scoreboard.team;

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

public class TeamListener {
    private static final Map<String, Session> teamSessions = new HashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void addSession(String sessionId, Session session) {
        teamSessions.put(sessionId, session);
        List<Map<String, Object>> teams = fetchTeamsFromDatabase();

        try {
            session.getBasicRemote().sendText(objectMapper.writeValueAsString(teams));
        } catch (IOException e) {
            System.out.println("error" + e.getMessage());
        }
    }

    public static void removeSession(String sessionId) {
        teamSessions.remove(sessionId);
    }

    public static void fireTeamsUpdate() {
        sendTeamsToAllSessions();
    }

    private static void sendTeamsToAllSessions() {
        List<Map<String, Object>> teams = fetchTeamsFromDatabase();
        List<Session> sendingSessions = new ArrayList<>(teamSessions.values());
        while (!sendingSessions.isEmpty()) {
            List<Session> toRemove = new ArrayList<>();
            for (Session session : sendingSessions) {
                try {
                    System.out.println("Send content to session: " + session.getId());
                    if (session == null || !session.isOpen()) {
                        toRemove.add(session);
                        continue;
                    }
                    session.getBasicRemote().sendText(objectMapper.writeValueAsString(teams));
                    toRemove.add(session);
                } catch (Exception e) {
                    System.out.println("Error sending content to session: " + session.getId());
                }
            }
            sendingSessions.removeAll(toRemove);
        }
    }

    private static List<Map<String, Object>> fetchTeamsFromDatabase() {
        List<Map<String, Object>> teams = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = Database.getConnection();

            String query = "SELECT * FROM teams";
            stmt = conn.prepareStatement(query);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> team = new HashMap<>();
                team.put("id", rs.getInt("id"));
                team.put("name", rs.getString("name"));
                team.put("players", new ArrayList<>());
                for (int i = 1; i <= 11; i++) {
                    ((List<String>) team.get("players")).add(rs.getString("player" + i));
                }
                teams.add(team);
            }
        } catch (Exception e) {
            System.err.println("Error fetching teams from database: " + e.getMessage());
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

        return teams;
    }
}
