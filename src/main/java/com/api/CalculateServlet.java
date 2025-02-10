package com.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.annotation.WebServlet;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/api/calculate")
public class CalculateServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String number1 = request.getParameter("num1");
        int num = Integer.parseInt(number1);
        String number2 = request.getParameter("num2");
        int num2 = Integer.parseInt(number2);
        String operator = request.getParameter("ope");
        System.out.println("num1: " + num + " num2: " + num2 + " operator: " + operator);
        double result = 0;
        try {
            switch (operator) {
                case "+":
                    result = num + num2;
                    break;
                case "-":
                    result = num - num2;
                    break;
                case "*":
                    result = num * num2;
                    break;
                case "/":
                    result = (double) num / num2;
                    break;
                default:
                    break;
            }
            PrintWriter out = response.getWriter();
            out.println("<html><body>");
            out.println("<h1>Result: " + result + "</h1>");
            out.println("<button onclick=\"window.history.back()\">Go Back</button>");
            out.println("</body></html>");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            PrintWriter out = response.getWriter();
            out.println("<html><body>");
            out.println("<h1>Error: " + e.getMessage() + "</h1>");
            out.println("<button onclick=\"window.history.back()\">Go Back</button>");
            out.println("</body></html>");
        }
    }
}