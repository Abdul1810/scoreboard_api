package com.api;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.nio.ByteBuffer;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.api.util.Calculator;

@ServerEndpoint("/file")
public class WSFileCalculateServlet {

    private static final int MAX_THREADS = Runtime.getRuntime().availableProcessors(); // Or adjust as needed
    private static final ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);
    private static final int QUEUE_CAPACITY = 1000; // Adjust based on expected load
    private static final ConcurrentHashMap<String, AtomicInteger> sessionLineCounts = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        session.setMaxIdleTimeout(10 * 60 * 1000);
        session.setMaxTextMessageBufferSize(1024 * 1024);
        session.setMaxBinaryMessageBufferSize(1024 * 1024);
        String sessionId = session.getId();
        sessionLineCounts.put(sessionId, new AtomicInteger(0));
        System.out.println("Open session: " + sessionId);
    }

    @OnMessage
    public void onMessage(ByteBuffer file, Session session) {
        String sessionId = session.getId();
        AtomicInteger lineCount = sessionLineCounts.get(sessionId);
        if (lineCount == null) {
            System.err.println("Session not found for message processing.");
            return; // Handle the error appropriately
        }

        String fileContent = new String(file.array());
        String[] lines = fileContent.split("\n");

        // Remove trailing empty line if present
        if (lines.length > 0 && lines[lines.length - 1].isEmpty()) {
            lines = java.util.Arrays.copyOf(lines, lines.length - 1);
        }

        lineCount.addAndGet(lines.length); // Increment before processing

        try {
            RemoteEndpoint.Basic remote = session.getBasicRemote();

            for (String line : lines) {
                executor.submit(() -> processLine(line, remote, sessionId));
            }

        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
        }
    }

    private void processLine(String line, RemoteEndpoint.Basic remote, String sessionId) {
        try {
            String message = line + " = " + Calculator.calculate(line) + "\n";
            remote.sendText(message);
        } catch (Exception e) {
            System.err.println("Error calculating/sending result for line '" + line + "': " + e.getMessage());
            try {
                remote.sendText(line + " = Error: " + e.getMessage() + "\n");
            } catch (Exception ex) {
                System.err.println("Error sending error message: " + ex.getMessage());
            }
        }
    }


    @OnClose
    public void onClose(Session session) {
        String sessionId = session.getId();
        System.out.println("Close session: " + sessionId);
        sessionLineCounts.remove(sessionId);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        String sessionId = session.getId();
        System.err.println("Error in session " + sessionId + ": " + throwable.getMessage());
        sessionLineCounts.remove(sessionId);
    }
}