package com.api;

import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

@ServerEndpoint("/text")
public class WSText2FileServlet {
    private final File file = new File("C:\\Users\\ACER\\Downloads\\text.txt");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private FileWriter writer;
    private StringBuilder content;

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("Open session: " + session.getId());
        try {
            writer = new FileWriter(file, true);
            try (FileReader reader = new FileReader(file)) {
                content = new StringBuilder();
                int i;
                while ((i = reader.read()) != -1) {
                    content.append((char) i);
                }
                System.out.println("reading text content: " + content);
                Map<String, String> response = new HashMap<>();
                response.put("type", "content");
                response.put("message", content.toString());
                session.getBasicRemote().sendText(objectMapper.writeValueAsString(response));
            } catch (Exception e) {
                System.out.println("not working" + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("error" + e.getMessage());
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
                System.out.println("text to add: " + messageContent);
                content.insert(cursorPos, messageContent);
                if (cursorPos != (content.length() - 1)) {
                    writer = new FileWriter(file);
                    writer.write(content.toString());
                    writer.flush();
                } else {
                    System.out.println(messageContent);
                    writer.write(messageContent);
                    writer.flush();
                }
            } else if (operation.equals("SUB")) {
                int count = Integer.parseInt(messageContent);
                content.delete(cursorPos, cursorPos + count);
                writer = new FileWriter(file);
                writer.write(content.toString());
                writer.flush();
            }
            Map<String, String> response = new HashMap<>();
            response.put("type", "sync");
            response.put("message", "sync done");
            session.getBasicRemote().sendText(objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            try {
                System.out.println("error" + e.getMessage());
                Map<String, String> response = new HashMap<>();
                response.put("type", "sync");
                response.put("message", e.getMessage());
                session.getBasicRemote().sendText(objectMapper.writeValueAsString(response));
            } catch (Exception ex) {
                System.out.println("error" + ex.getMessage());
            }
        }
    }

    @OnClose
    public void onClose(Session session) {
        try {
            writer.close();
        } catch (Exception e) {
            System.out.println("error" + e.getMessage());
        }
        System.out.println("Close session: " + session.getId());
    }
}
