package com.api.scoreboard.stats;

import com.api.scoreboard.commons.Match;
import com.api.scoreboard.match.embed.EmbedListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.Session;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class StatsListener {
    private static final Map<String, Session> sessions = new HashMap<>();
    private static final Map<String, CopyOnWriteArrayList<String>> matchSessions = new HashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void removeSession(String matchId, Session session) {
        sessions.remove(session.getId());
        matchSessions.getOrDefault(matchId, new CopyOnWriteArrayList<>()).remove(session.getId());
    }

    public static void fireStatsUpdate(String matchId) {
        String matchData = Match.getStats(matchId);
        sendStatsToAllSessions(matchId, matchData);
        EmbedListener.fireStatsUpdate(matchId, matchData);
    }

    public static void fireStatsUpdate(String matchId, String matchData) {
        sendStatsToAllSessions(matchId, matchData);
        EmbedListener.fireStatsUpdate(matchId, matchData);
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

        String matchStats = Match.getStats(matchId);
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
}
