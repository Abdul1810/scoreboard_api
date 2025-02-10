<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Zoho Training - Day 4</title>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <script>
        let socket;
        let count = 0;
        document.addEventListener("DOMContentLoaded", function () {
            socket = new WebSocket("ws://localhost:8080/file");
            socket.onopen = function () {
                console.log("ws connected");
            };

            socket.onmessage = function (event) {
                count++;
                document.getElementById("result").innerText += event.data;
                document.getElementById("progress").innerText = "Progress: " + count;
            };

            socket.onerror = function (error) {
                console.error("Error : " + error);
            };

            socket.onclose = function () {
                console.log("Closed");
            };
        });

        function sendFileInSocket(file) {
            document.getElementById("result").innerText = "";
            const reader = new FileReader();
            reader.onload = function (e) {
                console.log("Started Sending File");
                const arrayBuffer = e.target.result;
                if (socket && socket.readyState === WebSocket.OPEN) {
                    socket.send(arrayBuffer);
                }
            }
            reader.readAsArrayBuffer(file);
        }
    </script>
</head>
<body style="text-align: center;font-family: Arial,serif;">
<h1>Hello, ZOHO!</h1>
<p>Java, Servlet, Websocket, JSP</p>
<input type="file" accept="text/plain" onchange="sendFileInSocket(this.files[0])"/>
<h3>Result:</h3>
<div id="progress">
</div>
<div id="result">
    Add File to see the result
</div>
</body>
</html>
