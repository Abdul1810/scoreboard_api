package com.api;

import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.*;
import java.nio.file.*;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.fasterxml.jackson.databind.ObjectMapper;

@ServerEndpoint("/text")
public class WSText2FileServlet {
    private final Path filePath = Paths.get("C:\\Users\\ACER\\Downloads\\text.txt");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private StringBuilder content;
    private Thread watchThread;

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("Open session: " + session.getId());
        session.setMaxIdleTimeout(10 * 60 * 1000);
        session.setMaxTextMessageBufferSize(1024 * 1024);
        session.setMaxBinaryMessageBufferSize(1024 * 1024);

        sessions.put(session.getId(), session);
        sendFileToSession(session);

        if (watchThread == null) {
            System.out.println("Start file watcher");
            startFileWatcher();
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("Request: " + message);
        try {
            Map<String, Object> map = objectMapper.readValue(message, Map.class);
            String operation = (String) map.getOrDefault("operation", "ADD");
            int cursorPos = Integer.parseInt(map.getOrDefault("position", 0).toString());
            String messageContent = (String) map.getOrDefault("content", "");

            if (operation.equals("ADD")) {
                System.out.println("Text to add: " + messageContent);
                content.insert(cursorPos, messageContent);
                Files.write(filePath, content.toString().getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
            } else if (operation.equals("SUB")) {
                int count = Integer.parseInt(messageContent);
                content.delete(cursorPos, cursorPos + count);
                Files.write(filePath, content.toString().getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
            }

//            Map<String, String> response = new HashMap<>();
//            response.put("type", "sync");
//            response.put("message", "sync done");
//            session.getBasicRemote().sendText(objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            try {
                System.out.println("Error: " + e.getMessage());
                Map<String, String> response = new HashMap<>();
                response.put("type", "sync");
                response.put("message", e.getMessage());
                session.getBasicRemote().sendText(objectMapper.writeValueAsString(response));
            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }
    }

    private void sendFileToSession(Session session) {
        try {
            if (content == null) {
                List<String> lines = Files.readAllLines(filePath);
                content = new StringBuilder();
                for (int i = 0; i < lines.size(); i++) {
                    content.append(lines.get(i));
                    if (i < lines.size() - 1) {
                        content.append("\n");
                    }
                }
                System.out.println("content " + content);
                Map<String, String> response = new HashMap<>();
                response.put("type", "content");
                response.put("message", content.toString());
                try {
                    session.getBasicRemote().sendText(objectMapper.writeValueAsString(response));
                    System.out.println("Send content to session: " + session.getId());
                } catch (IOException e) {
                    System.out.println("Error sending content to session: " + session.getId());
                }
            } else {
                Map<String, String> response = new HashMap<>();
                response.put("type", "content");
                response.put("message", content.toString());
                try {
                    session.getBasicRemote().sendText(objectMapper.writeValueAsString(response));
                    System.out.println("Send content to session: " + session.getId());
                } catch (IOException e) {
                    System.out.println("Error sending content to session: " + session.getId());
                }
            }
        } catch (IOException e) {
            System.out.println("error" + e.getMessage());
        }
        Map<String, Object> response = new HashMap<>();
        response.put("type", "sync");
        response.put("message", "sync done");
        try {
            session.getBasicRemote().sendText(objectMapper.writeValueAsString(response));
        } catch (IOException e) {
            System.out.println("Error sending sync to session: " + session.getId());
        }
    }

    private void startFileWatcher() {
        watchThread = new Thread(() -> {
            System.out.println("thread start");
            try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
                Path directory = filePath.getParent();
                System.out.println("dir" + directory);
                directory.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                while (true) {
                    WatchKey key = watchService.take();
                    System.out.println("key" + key);
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            Path modifiedFile = (Path) event.context();
                            if (modifiedFile.equals(filePath.getFileName())) {
                                System.out.println("new file " + modifiedFile);
                                sendContentChangeToall();
                            }
                        }
                    }
                    boolean valid = key.reset();
                    if (!valid) {
                        break;
                    }
                }
            } catch (IOException | InterruptedException e) {
                System.out.println("Error in file watcher: " + e.getMessage());
            }
        });
        watchThread.start();
    }

    private void sendContentChangeToall() {
        try {
            List<String> lines = Files.readAllLines(filePath);
            System.out.println("lines " + lines);
            System.out.println("len " + lines.size());

            StringBuilder tempContent = new StringBuilder();
            for (int i = 0; i < lines.size(); i++) {
                tempContent.append(lines.get(i));
                if (i < lines.size() - 1) {
                    tempContent.append("\n");
                }
            }

            System.out.println("old" + content);
            System.out.println("new" + tempContent);
            if (content.toString().trim().contentEquals(tempContent)) {
                return;
            }
            String lastText = content.toString();
            String fileContentNow = tempContent.toString();

            int minLen = Math.min(lastText.length(), fileContentNow.length());
            int start = 0;
            while (start < minLen && lastText.charAt(start) == fileContentNow.charAt(start)) {
                start++;
            }

            int endOld = lastText.length() - 1;
            int endNew = fileContentNow.length() - 1;
            while (endOld >= start && endNew >= start && lastText.charAt(endOld) == fileContentNow.charAt(endNew)) {
                endOld--;
                endNew--;
            }

            Map<String, String> response = new HashMap<>();
            if (lastText.length() > fileContentNow.length()) {
                response.put("type", "SUB");
                response.put("position", String.valueOf(start));
                response.put("message", String.valueOf(lastText.substring(start, endOld + 1).length()));
            } else {
                response.put("type", "ADD");
                response.put("position", String.valueOf(start));
                response.put("message", fileContentNow.substring(start, endNew + 1));
            }
            content = tempContent;

            List<Session> sendingSessions = new ArrayList<>(sessions.values());
            System.out.println("total sessions " + sessions.size());
            System.out.println("sending sessions " + sendingSessions.size());
            while (!sendingSessions.isEmpty()) {
                List<Session> toRemove = new ArrayList<>();
                for (Session session : sendingSessions) {
                    try {
                        System.out.println("Send content to session: " + session.getId());
                        if (!session.isOpen()) {
                            toRemove.add(session);
                            continue;
                        }
                        session.getBasicRemote().sendText(objectMapper.writeValueAsString(response));
                        toRemove.add(session);
                    } catch (Exception e) {
                        System.out.println("Error sending content to session: " + session.getId());
                    }
                }
                sendingSessions.removeAll(toRemove);
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("Close session: " + session.getId());
        sessions.remove(session.getId());
        if (sessions.isEmpty()) {
            System.out.println("file watching kill");
            watchThread.interrupt();
            watchThread = null;
        }
    }
}
