<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    String num = session.getAttribute("num") != null ? session.getAttribute("num").toString() : "";
    String result = session.getAttribute("result") != null ? session.getAttribute("result").toString() : "";
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>Java Training</title>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <script>
        let socket;

        document.addEventListener("DOMContentLoaded", function () {
            const displayInput = document.getElementById('display');
            displayInput.focus();
            // displayInput.setSelectionRange(displayInput.value.length, displayInput.value.length);
            displayInput.addEventListener('input', function () {
                displayInput.value = displayInput.value.replace(/[^0-9+\-*/]/g, '');
                sendToSocket();
            });

            socket = new WebSocket("ws://localhost:8080/ws");
            socket.onopen = function() {
                console.log("ws connected");
            };

            socket.onmessage = function(event) {
                document.getElementById("result").innerText = event.data;
            };

            socket.onerror = function(error) {
                console.error("Error : " + error);
            };

            socket.onclose = function() {
                console.log("Closed");
            };
        });
        function sendToSocket() {
            const display = document.getElementById("display").value;
            if (socket && socket.readyState === WebSocket.OPEN) {
                socket.send(display);
            }
        }
        function addNumber(value) {
            const display = document.getElementById("display");
            display.value += value;
            sendToSocket();
        }
        function clearDisplay() {
            document.getElementById("display").value = "";
            sendToSocket();
        }
        function removeLastNumber() {
            const display = document.getElementById("display").value;
            document.getElementById("display").value = display.substring(0, display.length - 1);
            sendToSocket();
        }
    </script>
</head>
<body style="display: grid; place-items: center;">
<h1>Hello, Java!</h1>
<p>Java, Servlet, Websocket, JSP</p>
<%--<input type="text" id="result" readonly style="margin-right: 10px;" value="<%= result %>"/>--%>
<p id="result" style="margin-right: 10px;">Enter a expression</p>
<input type="text" id="display" name="num" style="margin-right: 10px;" required value="<%= num %>" />
<hr/>
<div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px;">
    <% for (int i = 1; i <= 9; i++) { %>
    <button type="button" value="<%=i%>" onclick="addNumber(this.value)"><%=i%></button>
    <% } %>
    <button type="button" value="0" onclick="addNumber(this.value)">0</button>
    <button type="button" value="C" onclick="clearDisplay()">C</button>
    <button type="button" value="⌫" onclick="removeLastNumber()">⌫</button>
    <button type="button" value="+" onclick="addNumber(this.value)">+</button>
    <button type="button" value="-" onclick="addNumber(this.value)">-</button>
    <button type="button" value="*" onclick="addNumber(this.value)">*</button>
    <button type="button" value="/" onclick="addNumber(this.value)">/</button>
</div>

</body>
</html>
