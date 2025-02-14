package com.api.calculator;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.nio.ByteBuffer;

import static com.api.util.Calculator.calculate;

@ServerEndpoint("/file")
public class WSFileCalculateServlet {
    boolean[] calculatedOnes;
    String[] lines;
//    private final static int MAX_CACHE_SIZE = 300;
//    private final static Map<String, Double> caches = new LinkedHashMap<String, Double>(100, 0.90f, true) {
//        @Override
//        protected boolean removeEldestEntry(Map.Entry<String, Double> eldest) {
//            return size() > MAX_CACHE_SIZE;
//        }
//    };

    @OnMessage
    public void onMessage(String message, Session session) throws IOException {
        String[] parts = message.split(" ");
        int from = Integer.parseInt(parts[0]);
        int to = Integer.parseInt(parts[1]);

        for (int i = from; i < to; i++) {
            if (calculatedOnes != null && calculatedOnes[i]) {
                continue;
            }
//            if (caches.containsKey(lines[i])) {
//                System.out.println("cache using");
//                calculatedOnes[i] = true;
//                session.getBasicRemote().sendText(i + "," + caches.get(lines[i]));
//            } else {
//                caches.put(lines[i], result);
//                System.out.println("cache save");
//            }
                double result = calculate(lines[i]);
                calculatedOnes[i] = true;
                session.getBasicRemote().sendText(i + "," + result);
        }
    }

    @OnMessage
    public void onMessage(ByteBuffer file, Session session) {
        String fileContent = new String(file.array());
        lines = fileContent.split("\n");
        calculatedOnes = new boolean[lines.length];
        System.out.println("received " + lines.length);
    }

    @OnOpen
    public void onOpen(Session session) {
        session.setMaxIdleTimeout(10 * 60 * 1000);
        session.setMaxTextMessageBufferSize(1024 * 1024);
        session.setMaxBinaryMessageBufferSize(1024 * 1024);
        System.out.println("Open session: " + session.getId());
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("Close session: " + session.getId());
    }
}
