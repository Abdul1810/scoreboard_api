<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Zoho Training - Day 4</title>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <script>
        let socket;
        let count = 0;
        let lines = [];

        function initWS() {
            socket = new WebSocket("ws://localhost:8080/file");
            socket.onopen = function () {
                console.log("success");
            };

            socket.onmessage = function (event) {
                count++;
                // document.getElementById("result").children[count - 1].innerText += " = " + event.data;
                document.getElementById("result").innerText += " = " + event.data + "\n";
                sendNextLine();
            };

            socket.onerror = function (error) {
                console.error("error: " + error);
            };

            socket.onclose = function () {
                console.log("WebSocket Closed");
            };
        }

        document.addEventListener("DOMContentLoaded", function () {
            initWS();
        });

        function sendFileInSocket(file) {
            count = 0;
            document.getElementById("result").innerText = "";
            document.getElementById("progress").innerText = "";

            if (!socket || socket.readyState !== WebSocket.OPEN) {
                initWS();
                socket.onopen = function () {
                    sendFileInSocket(file);
                };
                return;
            }

            const reader = new FileReader();
            reader.onload = function (e) {
                console.log("starts");
                lines = e.target.result.trim().split("\n");
                sendNextLine();
            };
            reader.readAsText(file);
        }

        function sendNextLine() {
            if (lines.length > 0) {
                let line = lines.shift();
                if (socket && socket.readyState === WebSocket.OPEN) {
                    socket.send(line);
                    // let el = document.createElement("p");
                    // el.innerText = line;
                    // document.getElementById("result").appendChild(el);
                    document.getElementById("result").innerText += line;
                    document.getElementById("progress").innerText = "Progress: " + (count + 1);
                }
            }
        }
    </script>
</head>
<body style="text-align: center; font-family: Arial, sans-serif;">
<h1>Hello, ZOHO!</h1>
<p>Java, Servlet, WebSocket, JSP</p>
<input type="file" accept="text/plain" onchange="sendFileInSocket(this.files[0])"/>
<div id="progress"></div>
<h3>Result:</h3>
<div id="result">Add a file to see the result</div>
</body>
</html>
