package com.api.scoreboard.stats;

import com.api.scoreboard.commons.Match;
import com.api.util.Database;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@ServerEndpoint("/ws/stats")
public class StatsSocket {
    Map<String, String> sessionMapWithMatchId = new HashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        session.setMaxIdleTimeout(60 * 60 * 1000);
        session.setMaxTextMessageBufferSize(1024 * 1024);
        session.setMaxBinaryMessageBufferSize(10 * 1024 * 1024);

        System.out.println("Open sessions " + session.getId());
        String matchId = session.getRequestParameterMap().get("id").get(0);
        if (matchId == null) {
            try {
                session.getBasicRemote().sendText("not-found");
                session.close();
            } catch (Exception e) {
                System.out.println("Error closing session");
            }
            return;
//        } else {
//            Connection conn = null;
//            try {
//                conn = new Database().getConnection();
//                if (!Match.isOwner(conn, Integer.parseInt(matchId), (int) session.getUserProperties().get("uid"))) {
//                    try {
//                        session.close();
//                    } catch (Exception e) {
//                        System.out.println("Error closing session");
//                    }
//                    return;
//                }
//            } catch (Exception e) {
//                System.out.println("Error while checking match owner : " + e.getMessage());
//                try {
//                    session.close();
//                } catch (Exception ex) {
//                    System.out.println("Error closing session");
//                }
//                return;
//            } finally {
//                try {
//                    if (conn != null) {
//                        conn.close();
//                    }
//                } catch (Exception e) {
//                    System.out.println("Error while closing connection : " + e.getMessage());
//                }
//            }
        }
        StatsListener.addSession(matchId, session);
        sessionMapWithMatchId.put(session.getId(), matchId);
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("Close session" + session.getId());
        String matchId = sessionMapWithMatchId.get(session.getId());
        StatsListener.removeSession(matchId, session);
    }
}
