package com.api.calculator;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/cal")
public class AdvanceCalculateServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        System.out.println("Request: " + request.getParameter("num"));
        String num = request.getParameter("num");
        if (num != null && !num.isEmpty()) {
            PrintWriter out = response.getWriter();
            try {
                String[] numbers = num.split("[+\\-*/]");
                String[] operators = num.split("[0-9]+");

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
                out.println("<h3>Result: " + result + "</h3>");
                HttpSession session = request.getSession();
                session.setAttribute("result", result);
                session.setAttribute("num", num);
                response.sendRedirect("/");
            } catch (Exception e) {
                out.println("<h3 style='color:red;'>Error: " + e.getMessage() + "</h3>");
                HttpSession session = request.getSession();
                session.setAttribute("error", e.getMessage());
                session.setAttribute("num", num);
                response.sendRedirect("/");
            }
        }
    }

//    @Override
//    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
//        // System.out.println("Request: " + request.getParameterNames());
//        System.out.println("Request: " + request.getParameter("num"));
//        String num = request.getParameter("num"); // 3+3/2
//        String[] numbers = num.split("[+\\-*/]");
//        String[] operators = num.split("[0-9]+");
//
//        for (int i = 0; i < numbers.length - 1; i++) {
//            System.out.print(numbers[i] + operators[i + 1]);
//        }
//        System.out.println(numbers[numbers.length - 1]);
//        double result = Double.parseDouble(numbers[0]);
//        try {
//            for (int i = 0; i < operators.length; i++) {
//                double number = Double.parseDouble(numbers[i]);
//                switch (operators[i]) {
//                    case "+":
//                        result += number;
//                        break;
//                    case "-":
//                        result -= number;
//                        break;
//                    case "*":
////                        if (result == 0) {
////                            result = number;
////                        } else {
//                        result *= number;
////                        }
//                        break;
//                    case "/":
////                        if (result == 0) {
////                            result = number;
////                        } else {
//                        result /= number;
////                        }
//                        break;
//                    default:
//                        result = number;
//                        break;
//                }
//            }
////            out.println("<html><body>");
////            out.println("<h1>Result: " + result + "</h1>");
////            out.println("<button onclick=\"window.history.back()\">Go Back</button>");
////            out.println("</body></html>");
////            response.setContentType("application/json");
////            response.setCharacterEncoding("UTF-8");
////            out.println("{\"result\": " + result + "}");
//            PrintWriter out = response.getWriter();
//            out.println(result);
//        } catch (Exception e) {
//            System.out.println("Error: " + e.getMessage());
//            PrintWriter out = response.getWriter();
//            out.println(e.getMessage());
////            out.println("<html><body>");
////            out.println("<h1>Error: " + e.getMessage() + "</h1>");
////            out.println("<button onclick=\"window.history.back()\">Go Back</button>");
////            out.println("</body></html>");
//        }
//    }
}