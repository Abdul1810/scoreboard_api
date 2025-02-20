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

//    public static void addSession(String sessionId, Session session) {
//        sessions.put(sessionId, session);
//
//        Map<String, Integer> scores = (Map<String, Integer>) context.getAttribute("scores");
//        if (scores == null) {
//            scores = new HashMap<>();
//            scores.put("team1", 0);
//            scores.put("team2", 0);
//            context.setAttribute("scores", scores);
//        }
//
//        try {
//            session.getBasicRemote().sendText(objectMapper.writeValueAsString(scores));
//        } catch (IOException e) {
//            System.out.println("error" + e.getMessage());
//        }
//    }
//
//    public static void removeSession(String sessionId) {
//        sessions.remove(sessionId);
//    }

    public static void addSession(String matchId, Session session) {
        sessions.put(session.getId(), session);
        List<String> sessions = matchSessions.get(matchId);
        if (sessions == null) {
            sessions = new ArrayList<>();
        }
        sessions.add(session.getId());
        matchSessions.put(matchId, sessions);

        Map<String, Map<String, Integer>> scores = (Map<String, Map<String, Integer>>) context.getAttribute("scores");
        if (scores == null) {
            scores = new HashMap<>();
        }
        Map<String, Integer> matchScores = scores.get(matchId);
        if (matchScores == null) {
            matchScores = new HashMap<>();
            matchScores.put("team1", 0);
            matchScores.put("team2", 0);
            scores.put(matchId, matchScores);
        }
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
        if ("scores".equals(event.getName())) {
            System.out.println("Attribute replaced: " + event.getName());
            System.out.println("New value: " + context.getAttribute("scores"));
            sendScoreToALlSessions();
        }
    }

    private void sendScoreToALlSessions() {
        Map<String, Map<String, Integer>> scores = (Map<String, Map<String, Integer>>) context.getAttribute("scores");
        for (Map.Entry<String, Map<String, Integer>> entry : scores.entrySet()) {
            String matchId = entry.getKey();
            Map<String, Integer> matchScores = entry.getValue();
            List<String> sendingSessions = matchSessions.get(matchId);
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
    }
}
