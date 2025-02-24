package com.api.scoreboard;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.api.util.Utils.validatePositiveIntegers;
import com.fasterxml.jackson.databind.ObjectMapper;

@ServerEndpoint("/ws/matches")
public class MatchSocket {
    private static final Map<String, Session> sessions = new HashMap<>();
    private static final Map<String, CopyOnWriteArrayList<String>> matchesSessionMap = new HashMap<>();

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Object> jsonResponse = new HashMap<>();

    @OnOpen
    public void onOpen(Session session) throws IOException {
        session.setMaxIdleTimeout(60 * 60 * 1000);
        session.setMaxTextMessageBufferSize(1024 * 1024);
        session.setMaxBinaryMessageBufferSize(10 * 1024 * 1024);
        if (!sessions.isEmpty()) {
            jsonResponse.put("action", "REQUEST");
            jsonResponse.put("id", session.getId());
            try {
                sessions.values().iterator().next().getBasicRemote().sendText(objectMapper.writeValueAsString(jsonResponse));
            } catch (IOException e) {
                System.out.println("Error sending message: " + e.getMessage());
            }
            jsonResponse.clear();
        }
        if (session.getRequestParameterMap().get("id") != null) {
            String matchId = session.getRequestParameterMap().get("id").get(0);
            if (matchesSessionMap.containsKey(matchId)) {
                matchesSessionMap.get(matchId).add(session.getId());
            } else {
                CopyOnWriteArrayList<String> sessionList = new CopyOnWriteArrayList<>();
                sessionList.add(session.getId());
                matchesSessionMap.put(matchId, sessionList);
            }
            jsonResponse.put("action", "REQUEST_MATCH");
            jsonResponse.put("id", session.getId());
            jsonResponse.put("match_id", matchId);
            try {
                sessions.values().iterator().next().getBasicRemote().sendText(objectMapper.writeValueAsString(jsonResponse));
            } catch (IOException e) {
                System.out.println("Error sending message: " + e.getMessage());
            }
        }
        sessions.put(session.getId(), session);
        System.out.println("Open session" + session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session) throws IOException {
        System.out.println("Message from " + session.getId() + ": " + message);
        Map<String, Object> msgData = objectMapper.readValue(message, Map.class);
        String action = (String) msgData.get("action");

        if (action.equals("CREATE")) {
            System.out.println("Create match");
            sendToAllSessionsExcept(session, msgData);
        } else if (action.equals("DELETE")) {
            System.out.println("Delete match");
            sendToAllSessionsExcept(session, msgData);
            disconnectMatchSessions((String) msgData.get("id"));
        } else if (action.equals("RESPONSE")) {
            System.out.println("Matches requested");
            String requestedSessionId = (String) msgData.get("id");
            jsonResponse.put("action", "RECEIVE");
            jsonResponse.put("message", "Match requested");
            jsonResponse.put("data", msgData.get("data"));
            if (sessions.containsKey(requestedSessionId)) {
                sessions.get(requestedSessionId).getBasicRemote().sendText(objectMapper.writeValueAsString(jsonResponse));
            }
        } else if (action.equals("RESPONSE_MATCH")) {
            System.out.println("Match requested");
            String requestedSessionId = (String) msgData.get("id");
            jsonResponse.put("action", "MODIFY");
            jsonResponse.put("message", "Match requested");
            jsonResponse.put("data", msgData.get("data"));
            if (sessions.containsKey(requestedSessionId)) {
                sessions.get(requestedSessionId).getBasicRemote().sendText(objectMapper.writeValueAsString(jsonResponse));
            }
        } else if (action.equals("MODIFY")) {
            System.out.println("Modify match" + msgData);
            Map<String, Object> data = (Map<String, Object>) msgData.get("data");
            System.out.println("Data: " + data);
            if (data.get("current_batting").equals("team1")) {
                if (!validatePositiveIntegers((String) data.get("team1_score"), (String) data.get("team1_wickets"), (String) data.get("team1_balls"))) {
                    System.out.println("Invalid Data team1");
                    return;
                } else if (Integer.parseInt((String) data.get("team1_balls")) > 120 || Integer.parseInt((String) data.get("team1_wickets")) > 10) {
                    System.out.println("Invalid Data team1");
                    return;
                } else {
                    if (Integer.parseInt((String) data.get("team1_balls")) == 120 || Integer.parseInt((String) data.get("team1_wickets")) == 10) {
                        data.put("current_batting", "team2");
                    }
                }
            } else if (data.get("current_batting").equals("team2")) {
                if (!validatePositiveIntegers((String) data.get("team2_score"), (String) data.get("team2_wickets"), (String) data.get("team2_balls"))) {
                    System.out.println("Invalid Data team2");
                    return;
                } else if (Integer.parseInt((String) data.get("team2_balls")) > 120 || Integer.parseInt((String) data.get("team2_wickets")) > 10) {
                    System.out.println("Invalid Data team2");
                    return;
                } else {
                    if (Integer.parseInt((String) data.get("team2_balls")) == 120 || Integer.parseInt((String) data.get("team2_wickets")) == 10) {
                        data.put("current_batting", "team1");
                        if (Integer.parseInt((String) data.get("team1_score")) > Integer.parseInt((String) data.get("team2_score"))) {
                            data.put("winner", "team1");
                        } else if (Integer.parseInt((String) data.get("team1_score")) < Integer.parseInt((String) data.get("team2_score"))) {
                            data.put("winner", "team2");
                        } else {
                            data.put("winner", "tie");
                        }
                        data.put("is_completed", "true");
                    } else {
                        if (Integer.parseInt((String) data.get("team2_score")) > Integer.parseInt((String) data.get("team1_score"))) {
                            data.put("winner", "team2");
                            data.put("is_completed", "true");
                        }
                    }
                }
            }
            System.out.println("MODIFY Data: " + data);
            System.out.println(msgData);
            sendToAllSessions(msgData);
        }
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session.getId());
        matchesSessionMap.forEach((matchId, sessionList) -> {
            sessionList.remove(session.getId());
        });
        System.out.println("Close session" + session.getId());
    }

    private void sendToAllSessions(Map<String, Object> data) {
        System.out.println("Session size: " + sessions.size());
        List<Session> sendingSession = new ArrayList<>();
        sessions.forEach((sessionId, s) -> {
            sendingSession.add(s);
        });

        while (!sendingSession.isEmpty()) {
            List<Session> toRemove = new ArrayList<>();
            for (Session s : sendingSession) {
                try {
                    s.getBasicRemote().sendText(objectMapper.writeValueAsString(data));
                    toRemove.add(s);
                } catch (IOException e) {
                    System.out.println("Error sending message: " + e.getMessage());
                }
            }
            sendingSession.removeAll(toRemove);
        }
    }

    private void sendToAllSessionsExcept(Session session, Map<String, Object> data) {
        System.out.println("Session size: " + sessions.size());
        List<Session> sendingSession = new ArrayList<>();
        sessions.forEach((sessionId, s) -> {
            if (!sessionId.equals(session.getId())) {
                sendingSession.add(s);
            }
        });

        while (!sendingSession.isEmpty()) {
            List<Session> toRemove = new ArrayList<>();
            for (Session s : sendingSession) {
                try {
                    s.getBasicRemote().sendText(objectMapper.writeValueAsString(data));
                    toRemove.add(s);
                } catch (IOException e) {
                    System.out.println("Error sending message: " + e.getMessage());
                }
            }
            sendingSession.removeAll(toRemove);
        }
    }

    private void disconnectMatchSessions(String matchId) {
        List<Session> removeSessions = new ArrayList<>();
        System.out.println(matchesSessionMap);
        System.out.println(matchId);
        matchesSessionMap.get(matchId).forEach((sessionId) -> {
            if (sessions.containsKey(sessionId)) {
                removeSessions.add(sessions.get(sessionId));
            }
        });
        System.out.println("Remove sessions: " + removeSessions.size());
        System.out.println("Match sessions: " + matchesSessionMap.get(matchId).size());
        removeSessions.forEach(System.out::println);
        while (!removeSessions.isEmpty()) {
            List<Session> toRemove = new ArrayList<>();
            for (Session s : removeSessions) {
                try {
                    s.close();
                    toRemove.add(s);
                } catch (IOException e) {
                    System.out.println("Error closing session: " + e.getMessage());
                }
            }
            removeSessions.removeAll(toRemove);
        }
    }
}
