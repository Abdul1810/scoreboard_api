<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String num = request.getParameter("num");
    if (num != null && !num.isEmpty()) {
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
                    case "+": result += number; break;
                    case "-": result -= number; break;
                    case "*": result *= number; break;
                    case "/":
                        if (number != 0) {
                            result /= number;
                        } else {
                            throw new ArithmeticException("Division by zero");
                        }
                        break;
                    default: break;
                }
            }
            out.println("<h3>Result: " + result + "</h3>");
        } catch (Exception e) {
            out.println("<h3 style='color:red;'>Error: " + e.getMessage() + "</h3>");
        }
    }
%>