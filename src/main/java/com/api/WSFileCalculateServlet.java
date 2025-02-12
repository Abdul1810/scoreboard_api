package com.api;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import com.api.util.Calculator;

import java.nio.ByteBuffer;

@ServerEndpoint("/file")
public class WSFileCalculateServlet {
    String[] lines;
    boolean[] calculatedOnes;

    @OnOpen
    public void onOpen(Session session) {
        session.setMaxIdleTimeout(10 * 60 * 1000);
        session.setMaxTextMessageBufferSize(1024 * 1024);
        session.setMaxBinaryMessageBufferSize(1024 * 1024);
        System.out.println("Open session: " + session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("Message: " + message + " from session: " + session.getId());
        String[] parts = message.split(" ");
        int from = Integer.parseInt(parts[0]);
        int to = Integer.parseInt(parts[1]);

        for (int i=from; i<to; i++) {
            if (calculatedOnes != null && calculatedOnes[i]) {
                continue;
            }
            double result = Calculator.calculate(lines[i]);
            try {
                session.getBasicRemote().sendText(i + "," + result);
                calculatedOnes[i] = true;
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    @OnMessage
    public void onMessage(ByteBuffer file, Session session) {
        String fileContent = new String(file.array());
        lines = fileContent.split("\n");
        calculatedOnes = new boolean[lines.length];
        System.out.println("received " + lines.length);
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("Close session: " + session.getId());
    }
}
