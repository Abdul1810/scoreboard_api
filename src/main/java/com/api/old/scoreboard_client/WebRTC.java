package com.api.old.scoreboard_client;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint("/ws/webrtc")
public class WebRTC {
    private static final Set<Session> peers = new CopyOnWriteArraySet<>();

    @OnOpen
    public void onOpen(Session session) {
        peers.add(session);
        System.out.println("Peer connected: " + session.getId());
    }

    @OnClose
    public void onClose(Session session) {
        peers.remove(session);
        System.out.println("Peer disconnected: " + session.getId());
    }

    @OnMessage
    public void onMessage(Session session, String message) throws IOException {
        for (Session peer : peers) {
            if (!peer.equals(session)) {
                peer.getBasicRemote().sendText(message);
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("Error on session " + session.getId() + ": " + throwable.getMessage());
    }
}
