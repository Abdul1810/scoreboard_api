package com.api.score;

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
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void contextInitialized(ServletContextEvent event) {
        context = event.getServletContext();
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        context = null;
    }

    public static void addSession(String sessionId, Session session) {
        sessions.put(sessionId, session);

        Map<String, Integer> scores = (Map<String, Integer>) context.getAttribute("scores");
        if (scores == null) {
            scores = new HashMap<>();
            scores.put("team1", 0);
            scores.put("team2", 0);
            context.setAttribute("scores", scores);
        }

        try {
            session.getBasicRemote().sendText(objectMapper.writeValueAsString(scores));
        } catch (IOException e) {
            System.out.println("error" + e.getMessage());
        }
    }

    public static void removeSession(String sessionId) {
        sessions.remove(sessionId);
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
        Map<String, Integer> scores = (Map<String, Integer>) context.getAttribute("scores");
        List<Session> sendingSessions = new ArrayList<>(sessions.values());
        while (!sendingSessions.isEmpty()) {
            List<Session> toRemove = new ArrayList<>();
            for (Session session : sendingSessions) {
                try {
                    System.out.println("Send content to session: " + session.getId());
                    if (!session.isOpen()) {
                        toRemove.add(session);
                        continue;
                    }
                    session.getBasicRemote().sendText(objectMapper.writeValueAsString(scores));
                    toRemove.add(session);
                } catch (Exception e) {
                    System.out.println("Error sending content to session: " + session.getId());
                }
            }
            sendingSessions.removeAll(toRemove);
        }
    }
}
