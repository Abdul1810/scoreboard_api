package com.api.old.calculator;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.nio.ByteBuffer;
import com.api.util.Calculator;

@ServerEndpoint("/old-file")
public class OLDWSFileCalculateServlet {
//    private final Queue<String> resQueue = new LinkedBlockingQueue<>();

    @OnOpen
    public void onOpen(Session session) {
        session.setMaxIdleTimeout(10 * 60 * 1000);
        session.setMaxTextMessageBufferSize(1024 * 1024);
        session.setMaxBinaryMessageBufferSize(1024 * 1024);
        System.out.println("Open session: " + session.getId());
    }

    @OnMessage
    public void onMessage(ByteBuffer file, Session session) {
        System.out.println("message");
        String fileContent = new String(file.array());

        String[] lines = fileContent.split("\n");
        System.out.println("Lines: " + lines.length);
        lines[lines.length - 1] = lines[lines.length - 1] + "\n";
        RemoteEndpoint.Basic remote = session.getBasicRemote();

        for (String line : lines) {
            try {
                String message = line + " = " + Calculator.calculate(line) + "\n";
                remote.sendText(message);
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

//        RemoteEndpoint.Async remote = session.getAsyncRemote();
//        CompletableFuture.runAsync(() -> {
//            for (String line : lines) {
//                try {
//                    String message = line + " = " + Calculator.calculate(line) + "\n";
//                    Future<Void> future = remote.sendText(message);
//                    future.get();
//                } catch (Exception e) {
//                    System.out.println("Error: " + e.getMessage());
//                    try {
//                        Future<Void> future = remote.sendText(line + " = Error: " + e.getMessage());
//                        future.get();
//                    } catch (Exception ex) {
//                        System.out.println("Error: " + ex.getMessage());
//                    }
//                }
//            }
//        }).exceptionally(e -> {
//            System.out.println(e.getMessage());
//            return null;
//        });

//        StringBuilder batchmsg = new StringBuilder();
//        for (int i = 0; i < lines.length; i++) {
//            try {
//                batchmsg.append(lines[i]).append(" = ").append(Calculator.calculate(lines[i])).append("\n");
//            } catch (Exception e) {
//                System.out.println("Error: " + e.getMessage());
//                batchmsg.append(lines[i]).append(" = Error: ").append(e.getMessage()).append("\n");
//            }
//
//            if (i != 0 && i % 100 == 0) {
//                sendBatchRequest(batchmsg.toString(), session);
//                batchmsg = new StringBuilder();
//            }
//        }
//
//        if (batchmsg.length() > 0) {
//            sendBatchRequest(batchmsg.toString(), session);
//        }

//        for (String line : lines) {
//            try {
//                resQueue.add(line + " = " + Calculator.calculate(line) + "\n");
//            } catch (Exception e) {
//                System.out.println("Error: " + e.getMessage());
//                resQueue.add(line + " = Error: " + e.getMessage() + "\n");
//            }
//            // sleep(1000);
//        }
//        RemoteEndpoint.Async remote = session.getAsyncRemote();
//        CompletableFuture.runAsync(() -> {
//            while (!resQueue.isEmpty()) {
//                String message = resQueue.poll();
//                if (message != null) {
//                    try {
//                        Future<Void> future = remote.sendText(message);
//                        future.get();
//                    } catch (Exception e) {
//                        System.out.println("error " + e.getMessage());
//                    }
//                }
//            }
//        }).exceptionally(e -> {
//            System.out.println(e.getMessage());
//            return null;
//        });

//    private void sendBatchRequest(String msg, Session session) {
//        RemoteEndpoint.Basic remote = session.getBasicRemote();
//        try {
//            remote.sendText(msg);
//        } catch (Exception e) {
//            System.out.println("Error: " + e.getMessage());
//        }
//    }
