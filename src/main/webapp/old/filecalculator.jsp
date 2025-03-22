<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Java Training</title>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <script>
        let socket;
        let fromVisibleLine = 0;
        let toVisibleLine = 0;
        let count = 0;
        // let debounceTimer;

        function updateScroll() {
            const results = document.getElementById("result").children;
            const viewHeight = window.innerHeight;
            let firstVisible = -1, lastVisible = -1;

            for (let i = 0; i < results.length; i++) {
                const { top, bottom } = results[i].getBoundingClientRect();
                if (bottom <= 0) continue;
                if (top >= viewHeight) break;
                if (firstVisible === -1) {
                    firstVisible = i + 1;
                }
                lastVisible = i + 1;
            }
            fromVisibleLine = firstVisible === -1 ? 0 : firstVisible;
            toVisibleLine = lastVisible === -1 ? 0 : lastVisible;

            document.getElementById("progress").innerText = `Progress: ${fromVisibleLine} - ${toVisibleLine}`;
            socket.send(`${fromVisibleLine - 1} ${toVisibleLine}`);
        }

        function initWS() {
            socket = new WebSocket("ws://localhost:8080/file");

            socket.onopen = function () {
                console.log("connect");
            };

            socket.onmessage = function (event) {
                const msg = event.data.split(",");
                document.getElementById("result").children[parseInt(msg[0])].innerText += " = " + msg[1];
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
            document.addEventListener("scroll", updateScroll);
        });

        function listContentFile(file) {
            count = 0;
            document.getElementById("result").innerText = "";
            document.getElementById("progress").innerText = "";

            if (!socket || socket.readyState !== WebSocket.OPEN) {
                initWS();
                socket.onopen = function () {
                    listContentFile(file);
                };
                return;
            }

            const reader = new FileReader();
            reader.onload = function (e) {
                console.log("printing");
                const lines = e.target.result.split("\n");

                for (let line of lines) {
                    let ele = document.createElement("p");
                    ele.innerText = line;
                    document.getElementById("result").appendChild(ele);
                }
                socket.send(new Blob([e.target.result]));
                updateScroll();
            };
            reader.readAsText(file);
        }
    </script>
</head>
<body style="text-align: center; font-family: Arial, sans-serif;">
<h1>Hello, JAVA!</h1>
<p>Java, Servlet, WebSocket, JSP</p>
<input type="file" accept="text/plain" onchange="listContentFile(this.files[0])"/>
<div id="progress" style="position: fixed; bottom: 0; left: 80%; transform: translateX(-50%); background-color: white; padding: 5px;">
</div>
<h3>Result:</h3>
<div id="result">Add a file to see the result</div>
</body>
</html>

<%--function updateVisibleLineCount() {--%>
<%--    const resultDiv = document.getElementById("result");--%>
<%--    const children = resultDiv.children;--%>
<%--    let visibleCount = 0;--%>

<%--    for (let child of children) {--%>
<%--        const rect = child.getBoundingClientRect();--%>
<%--        if (rect.top >= 0 && rect.bottom <= window.innerHeight) {--%>
<%--            visibleCount++;--%>
<%--        }--%>
<%--    }--%>

<%--    document.getElementById("progress").innerText = "Visible lines: " + visibleCount;--%>
<%--}--%>


<%--clearTimeout(debounceTimer);--%>
<%--debounceTimer = setTimeout(() => {--%>
<%--    const resultDiv = document.getElementById("result");--%>
<%--    const children = resultDiv.children;--%>
<%--    let firstVisible = -1;--%>
<%--    let lastVisible = -1;--%>

<%--    for (let i = 0; i < children.length; i++) {--%>
<%--        const rect = children[i].getBoundingClientRect();--%>
<%--        if (rect.top >= 0 && rect.bottom <= window.innerHeight) {--%>
<%--            if (firstVisible === -1) {--%>
<%--                firstVisible = i + 1;--%>
<%--            }--%>
<%--            lastVisible = i + 1;--%>
<%--        }--%>
<%--    }--%>

<%--    fromVisibleLine = firstVisible === -1 ? 0 : firstVisible;--%>
<%--    toVisibleLine = lastVisible === -1 ? 0 : lastVisible;--%>
<%--    count = fromVisibleLine - 1;--%>

<%--    document.getElementById("progress").innerText = `Progress: ${fromVisibleLine} - ${toVisibleLine}`;--%>
<%--    socket.send(`${fromVisibleLine - 1} ${toVisibleLine - 1}`);--%>
<%--}, 100);--%>