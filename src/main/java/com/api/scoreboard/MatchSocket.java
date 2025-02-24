package com.api.scoreboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@ServerEndpoint("/ws/matches")
public class MatchSocket {
    private static final Map<String, Session> sessions = new HashMap<>();
    private static String updatedSessionId = "";

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Object> jsonResponse = new HashMap<>();

    @OnOpen
    public void onOpen(Session session) throws IOException {
        session.setMaxIdleTimeout(60 * 60 * 1000);
        session.setMaxTextMessageBufferSize(1024 * 1024);
        session.setMaxBinaryMessageBufferSize(10 * 1024 * 1024);

        if (sessions.isEmpty()) {
            updatedSessionId = session.getId();
        } else {
            jsonResponse.put("action", "REQUEST");
            jsonResponse.put("id", session.getId());
            sessions.get(updatedSessionId).getBasicRemote().sendText(objectMapper.writeValueAsString(jsonResponse));
            jsonResponse.clear();
        }
        sessions.put(session.getId(), session);
        System.out.println("Open session" + session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session) throws IOException {
        System.out.println("Message from " + session.getId() + ": " + message);
        Map<String, Object> msgData = objectMapper.readValue(message, Map.class);

        if (msgData.get("action").equals("CREATE")) {
            System.out.println("Create match");
            updatedSessionId = session.getId();
            sendToAllSessionExcept(session, msgData);
        } else if (msgData.get("action").equals("DELETE")) {
            System.out.println("Delete match");
            updatedSessionId = session.getId();
            sendToAllSessionExcept(session, msgData);
        } else if (msgData.get("action").equals("RESPONSE")) {
            System.out.println("Match requested");
            String requestedSessionId = (String) msgData.get("id");

            jsonResponse.put("action", "RECEIVE");
            jsonResponse.put("message", "Match requested");
            jsonResponse.put("data", msgData.get("data"));
            if (sessions.containsKey(requestedSessionId)) {
                sessions.get(requestedSessionId).getBasicRemote().sendText(objectMapper.writeValueAsString(jsonResponse));
            }
        }
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session.getId());
        if (session.getId().equals(updatedSessionId)) {
            if (!sessions.isEmpty()) {
                updatedSessionId = sessions.keySet().iterator().next();
            } else {
                updatedSessionId = "";
            }
        }
        System.out.println("Close session" + session.getId());
    }

    private void sendToAllSessionExcept(Session session, Map<String, Object> data) {
        System.out.println("Session size: " + sessions);
        sessions.forEach((sessionId, s) -> {
            if (!sessionId.equals(session.getId())) {
                try {
                    s.getBasicRemote().sendText(objectMapper.writeValueAsString(data));
                } catch (IOException e) {
                    System.out.println("Error sending message: " + e.getMessage());
                }
            }
        });
    }
}
