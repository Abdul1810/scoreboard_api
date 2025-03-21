package com.api.scoreboard.match.embed;

import com.api.scoreboard.commons.Match;
import com.api.util.Database;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.Session;
import java.io.IOException;
import java.sql.Connection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class EmbedListener {
    private static final Map<String, Session> sessions = new HashMap<>();
    private static final Map<String, CopyOnWriteArrayList<String>> matchSessions = new HashMap<>();
    private static final Map<String, CopyOnWriteArrayList<String>> embedMatchSessions = new HashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void addSession(String embedCode, Session session, String matchId) {
        Connection conn = null;
        try {
            conn = new Database().getConnection();
            Map<String, Object> matchData = Match.get(conn, Integer.parseInt(matchId));
            session.getBasicRemote().sendText(objectMapper.writeValueAsString(Collections.singletonMap("details", matchData)));
            String stats = Match.getStats(matchId);
            session.getBasicRemote().sendText(stats);
        } catch (Exception e) {
            System.out.println("Error while fetching match data : " + e.getMessage());
            try {
                session.close();
            } catch (IOException ex) {
                System.out.println("Error while closing session : " + ex.getMessage());
            }
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception e) {
                System.out.println("Error while closing connection : " + e.getMessage());
            }
        }
        sessions.put(session.getId(), session);
        if (matchSessions.containsKey(matchId)) {
            matchSessions.get(matchId).add(session.getId());
        } else {
            CopyOnWriteArrayList<String> sessionIds = new CopyOnWriteArrayList<>();
            sessionIds.add(session.getId());
            matchSessions.put(matchId, sessionIds);
        }
        if (embedMatchSessions.containsKey(embedCode)) {
            embedMatchSessions.get(embedCode).add(session.getId());
        } else {
            CopyOnWriteArrayList<String> sessionIds = new CopyOnWriteArrayList<>();
            sessionIds.add(session.getId());
            embedMatchSessions.put(embedCode, sessionIds);
        }
    }

    public static void removeSession(String embedCode, Session session) {
        sessions.remove(session.getId());
        if (embedMatchSessions.containsKey(embedCode)) {
            matchSessions.forEach((matchId, sessionIds) -> {
                sessionIds.remove(session.getId());
            });
            embedMatchSessions.get(embedCode).remove(session.getId());
        }
    }

    public static void fireEmbedRemove(String embedCode) {
        if (embedMatchSessions.containsKey(embedCode)) {
            embedMatchSessions.get(embedCode).forEach(sessionId -> {
                Session session = sessions.get(sessionId);
                if (session != null && session.isOpen()) {
                    try {
                        session.close();
                    } catch (IOException e) {
                        System.out.println("Error while sending message to session: " + sessionId);
                    }
                }
            });
        }
    }

    public static void fireMatchRemove(String matchId) {
        if (matchSessions.containsKey(matchId)) {
            matchSessions.get(matchId).forEach(sessionId -> {
                Session session = sessions.get(sessionId);
                if (session != null && session.isOpen()) {
                    try {
                        session.close();
                    } catch (IOException e) {
                        System.out.println("Error while sending message to session: " + sessionId);
                    }
                }
            });
        }
    }

    public static void fireStatsUpdate(String matchId, String matchData) {
        sendStatsToAllSessions(matchId, matchData);
    }

    private static void sendStatsToAllSessions(String matchId, String matchData) {
        if (matchSessions.containsKey(matchId)) {
            matchSessions.get(matchId).forEach(sessionId -> {
                Session session = sessions.get(sessionId);
                if (session != null && session.isOpen()) {
                    try {
                        session.getBasicRemote().sendText(matchData);
                    } catch (IOException e) {
                        System.out.println("Error while sending message to session: " + sessionId);
                    }
                }
            });
        }
    }
}