<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Zoho Training - Day 6</title>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <script>
        let socket;
        let count = 0;

        function initWS() {
            socket = new WebSocket("ws://localhost:8080/file");

            socket.onopen = function () {
                console.log("connect");
            };

            socket.onmessage = function (event) {
                document.getElementById("result").children[count].innerText += " = " + event.data;
                document.getElementById("progress").innerText = "progress " + (count + 1);
                count++;
            };

            socket.onerror = function (error) {
                console.error("error: " + error);
            };

            socket.onclose = function () {
                console.log("closes");
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
                socket.send(e.target.result);
                const lines = e.target.result.split("\n");

                for (let line of lines) {
                    // if (socket && socket.readyState === WebSocket.OPEN) {
                        // socket.send(line);
                        let ele = document.createElement("p");
                        ele.innerText = line;
                        document.getElementById("result").appendChild(ele);
                    // }
                }
            };
            reader.readAsText(file);
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
