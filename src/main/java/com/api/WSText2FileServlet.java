package com.api;

import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.*;

import java.nio.file.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

@ServerEndpoint("/text")
public class WSText2FileServlet {
    private final Path filePath = Paths.get("C:\\Users\\ACER\\Downloads\\text.txt");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private StringBuilder content;

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("Open session: " + session.getId());
        try {
            // Read the content of the file at the time of connection
            List<String> lines = Files.readAllLines(filePath);
            content = new StringBuilder();
            for (String line : lines) {
                content.append(line).append(System.lineSeparator());
            }
            System.out.println("Reading text content: " + content);

            Map<String, String> response = new HashMap<>();
            response.put("type", "content");
            response.put("message", content.toString());
            session.getBasicRemote().sendText(objectMapper.writeValueAsString(response));
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("Request: " + message);
        try {
            HashMap<String, Object> map = objectMapper.readValue(message, HashMap.class);
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

            Map<String, String> response = new HashMap<>();
            response.put("type", "sync");
            response.put("message", "Sync done");
            session.getBasicRemote().sendText(objectMapper.writeValueAsString(response));
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

    @OnClose
    public void onClose(Session session) {
        System.out.println("Close session: " + session.getId());
    }
}
