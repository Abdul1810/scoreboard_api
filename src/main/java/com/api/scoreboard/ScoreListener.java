package com.api.scoreboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;
import jakarta.websocket.Session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebListener
public class ScoreListener implements ServletContextAttributeListener, ServletContextListener {
    private static ServletContext context;
    private static final Map<String, Session> sessions = new HashMap<>();
    private static final Map<String, List<String>> matchSessions = new HashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void contextInitialized(ServletContextEvent event) {
        context = event.getServletContext();
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        context = null;
    }

    public static void addSession(String matchId, Session session) {
        sessions.put(session.getId(), session);
        List<String> sessions = matchSessions.get(matchId);
        if (sessions == null) {
            sessions = new ArrayList<>();
        }
        sessions.add(session.getId());
        matchSessions.put(matchId, sessions);

        Map<String, String> matchScores = (Map<String, String>) context.getAttribute("match_" + matchId);
        try {
            session.getBasicRemote().sendText(objectMapper.writeValueAsString(matchScores));
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

    @Override
    public void attributeReplaced(ServletContextAttributeEvent event) {
        if (event.getName().startsWith("match")) {
            System.out.println("Attribute replaced: " + event.getName());
            System.out.println("New value: " + context.getAttribute(event.getName()));
            String matchId = event.getName().split("_")[1];
            sendScoreToALlSessions(matchId);
        }
    }

    @Override
    public void attributeRemoved(ServletContextAttributeEvent event) {
        if (event.getName().startsWith("match")) {
            System.out.println("Attribute removed: " + event.getName());
            System.out.println("New value: " + context.getAttribute(event.getName()));
            String matchId = event.getName().split("_")[1];
            disconnectAllSessions(matchId);
        }
    }

    private void sendScoreToALlSessions(String matchId) {
        Map<String, String> matchScores = (Map<String, String>) context.getAttribute("match_" + matchId);
        System.out.println("Send score to all sessions for match: " + matchId);
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
                    session.getBasicRemote().sendText(objectMapper.writeValueAsString(matchScores));
                    toRemove.add(sessionId);
                } catch (Exception e) {
                    System.out.println("Error sending content to session: " + sessionId);
                }
            }
            sendingSessions.removeAll(toRemove);
        }
    }

    private void disconnectAllSessions(String matchId) {
        List<String> sessions = matchSessions.get(matchId);
        System.out.println("Disconnecting all sessions for match: " + matchId);
        System.out.println("Sessions: " + sessions);
        for (String sessionId : sessions) {
            try {
                Session session = ScoreListener.sessions.get(sessionId);
                session.close();
            } catch (IOException e) {
                System.out.println("Error closing session: " + sessionId);
            }
        }
    }
}
