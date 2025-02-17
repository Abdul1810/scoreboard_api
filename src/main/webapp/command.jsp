<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Command Line</title>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <script>
        let socket;
        document.addEventListener("DOMContentLoaded", function () {
            socket = new WebSocket("ws://localhost:8080/cmd");
            const text = document.getElementById("text");
            const preview = document.getElementById("preview");

            text.focus();
            text.addEventListener("keydown", function (event) {
                if (event.key === "Enter") {
                    event.preventDefault();
                    sendCommand();
                }
            });

            function sendCommand() {
                const command = text.value;
                socket.send(command);
                preview.innerHTML = "";
            }

            socket.onmessage = function (event) {
                try {
                    document.getElementById("preview").innerHTML += event.data + "<br />";
                } catch (e) {
                    console.error("Error parsing message:", e);
                }
            };
        });
    </script>
</head>
<body style="text-align: center; font-family: Arial, sans-serif;">
<h1>Text Writer</h1>
<p>
    Java, Servlet, WebSocket, JSP
</p>
<div id="writer" style="display: flex; gap: 20px; align-items: flex-start; flex-wrap: wrap; max-width: 100%;">
    <div style="flex: 1; min-width: 300px;">
        <label for="text">enter cmd</label>
        <textarea name="text" id="text" rows="10" style="width: 100%;"></textarea>
    </div>

    <div style="flex: 1; min-width: 300px;">
        <label for="preview">out</label>
        <div id="preview" style="border: 1px solid #ccc; padding: 10px; min-height: 160px; background: #f9f9f9;"></div>
    </div>
</div>


</body>
</html>