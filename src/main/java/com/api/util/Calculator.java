package com.api.util;

import java.util.Stack;

public class Calculator {
    public static double calculate(String expression) {
        return evaluate(expression.replaceAll("\\s+", ""));
    }

    private static double evaluate(String expr) {
        Stack<Double> values = new Stack<>();
        Stack<Character> operators = new Stack<>();
        int i = 0;

        while (i < expr.length()) {
            char c = expr.charAt(i);
            if (Character.isDigit(c) || c == '.') {
                StringBuilder num = new StringBuilder();
                while (i < expr.length() && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
                    num.append(expr.charAt(i++));
                }
                values.push(Double.parseDouble(num.toString()));
                i--;
            } else if (c == '(') {
                operators.push(c);
            } else if (c == ')') {
                while (!operators.isEmpty() && operators.peek() != '(') {
                    values.push(applyOp(operators.pop(), values.pop(), values.pop()));
                }
                operators.pop();
            } else if ("+-*/".indexOf(c) != -1) {
                while (!operators.isEmpty() && precedence(operators.peek()) >= precedence(c)) {
                    values.push(applyOp(operators.pop(), values.pop(), values.pop()));
                }
                operators.push(c);
            }
            i++;
        }

        while (!operators.isEmpty()) {
            values.push(applyOp(operators.pop(), values.pop(), values.pop()));
        }

        return values.pop();
    }

    private static int precedence(char op) {
        return (op == '+' || op == '-') ? 1 : (op == '*' || op == '/') ? 2 : 0;
    }

    private static double applyOp(char op, double b, double a) {
        switch (op) {
            case '+':
                return a + b;
            case '-':
                return a - b;
            case '*':
                return a * b;
            case '/':
                if (b == 0)
                    throw new ArithmeticException("Division by zero");
                return a / b;
            default:
                return 0;
        }
    }
}
