package com.api;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import com.api.util.Calculator;

@ServerEndpoint("/file")
public class WSFileCalculateServlet {

    @OnOpen
    public void onOpen(Session session) {
        session.setMaxIdleTimeout(10 * 60 * 1000);
        session.setMaxTextMessageBufferSize(1024 * 1024);
        session.setMaxBinaryMessageBufferSize(1024 * 1024);
        System.out.println("Open session: " + session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        String[] lines = message.split("\n");
        RemoteEndpoint.Basic remote = session.getBasicRemote();

        for (String line : lines) {
            if (line == null || line.isEmpty()) {
                continue;
            }
            try {
                remote.sendText(Calculator.calculate(line) + "");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                try {
                    remote.sendText(line + " = Error: " + e.getMessage());
                } catch (Exception ex) {
                    System.out.println("Error " + ex.getMessage());
                }
            }
        }
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("Close session: " + session.getId());
    }
}
