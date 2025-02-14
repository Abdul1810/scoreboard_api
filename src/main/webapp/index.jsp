<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Text Writer</title>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <script>
        let socket;
        // let lastLength = 0;
        let lastText = "";

        document.addEventListener("DOMContentLoaded", function () {
            socket = new WebSocket("ws://localhost:8080/text");
            const text = document.getElementById("text");
            const result = document.getElementById("result");

            text.focus();
            text.addEventListener("input", updateText);

            function updateText() {
                if (socket.readyState === WebSocket.OPEN) {
                    const currentText = text.value;
                    const minLen = Math.min(lastText.length, currentText.length);
                    let start = 0;
                    while (start < minLen && lastText[start] === currentText[start]) {
                        start++;
                    }

                    let endOld = lastText.length - 1;
                    let endNew = currentText.length - 1;
                    while (endOld >= start && endNew >= start && lastText[endOld] === currentText[endNew]) {
                        endOld--;
                        endNew--;
                    }

                    if (lastText.length > currentText.length) {
                        const json = {
                            operation: "SUB",
                            position: start,
                            content: `${lastText.substring(start, endOld + 1).length}`
                        };
                        socket.send(JSON.stringify(json));
                    } else if (lastText.length < currentText.length) {
                        const json = {
                            operation: "ADD",
                            position: start,
                            content: currentText.substring(start, endNew + 1)
                        };
                        socket.send(JSON.stringify(json));
                    }

                    lastText = currentText;
                }
            }

            socket.onmessage = function (event) {
                const datas = event.data.split(",");
                const operation = datas.shift();
                let content = datas.join(",");

                if (operation === "STATUS") {
                    result.innerText = content;
                } else if (operation === "CONTENT") {
                    text.textContent = content;
                    lastText = content;
                }
            };
        });
    </script>
</head>
<body style="text-align: center; font-family: Arial, sans-serif;">
<h1>Text Writer</h1>
<p>
    Java, Servlet, Websocket, JSP
</p>
<div id="writer" style="display: flex; flex-direction: column; align-items: center;">
    <label for="text" id="result">
        enter text
    </label>
    <hr/>
    <textarea name="text" rows="25" cols="80" id="text"></textarea>
</div>
</body>
</html>
