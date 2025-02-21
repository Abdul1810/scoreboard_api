package com.api.old.calculator;

import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;

@ServerEndpoint("/ws")
public class WSCalculateServlet {

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("Open session: " + session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("Request: " + message);
        try {
            String[] numbers = message.split("[+\\-*/]");
            String[] operators = message.split("[0-9]+");

            for (int i = 0; i < numbers.length - 1; i++) {
                System.out.print(numbers[i] + operators[i + 1]);
            }
            System.out.println(numbers[numbers.length - 1]);

            double result = Double.parseDouble(numbers[0]);
            for (int i = 1; i < numbers.length; i++) {
                double number = Double.parseDouble(numbers[i]);
                String operator = operators[i].trim();

                switch (operator) {
                    case "+":
                        result += number;
                        break;
                    case "-":
                        result -= number;
                        break;
                    case "*":
                        result *= number;
                        break;
                    case "/":
                        if (number != 0) {
                            result /= number;
                        } else {
                            throw new ArithmeticException("Division by zero");
                        }
                        break;
                    default:
                        break;
                }
            }
            session.getBasicRemote().sendText("Result: " + result);
        } catch (Exception e) {
            try {
                session.getBasicRemote().sendText("Error: " + e.getMessage());
            } catch (IOException ex) {
                System.out.println("Error sending message: " + ex.getMessage());
            }
        }
    }
}
