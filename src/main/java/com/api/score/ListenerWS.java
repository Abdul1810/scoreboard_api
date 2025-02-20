package com.api.score;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint("/score")
public class ListenerWS {
    @OnOpen
    public void onOpen(Session session) {
        session.setMaxIdleTimeout(60 * 60 * 1000);
        session.setMaxTextMessageBufferSize(1024 * 1024);
        session.setMaxBinaryMessageBufferSize(10 * 1024 * 1024);

        System.out.println("Open session" + session.getId());
        ScoreListener.addSession(session.getId(), session);
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("Close session" + session.getId());
        ScoreListener.removeSession(session.getId());
    }
}
