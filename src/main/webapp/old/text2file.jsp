<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Text Writer</title>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <script>
        let socket;
        let lastText = "";

        document.addEventListener("DOMContentLoaded", function () {
            socket = new WebSocket("ws://localhost:8080/text");
            const text = document.getElementById("text");
            const result = document.getElementById("result");
            const preview = document.getElementById("preview");

            text.focus();
            text.addEventListener("input", updateText);

            function updateText() {
                preview.innerHTML = text.value;
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
                try {
                    const datas = JSON.parse(event.data);
                    if (datas.type === "ADD") {
                        const start = parseInt(datas.position, 10);
                        text.value = lastText.substring(0, start) + datas.message + lastText.substring(start);
                        lastText = text.value;
                        preview.innerHTML = lastText;
                        result.innerText = "sync done";
                    } else if (datas.type === "SUB") {
                        const start = parseInt(datas.position, 10);
                        const end = start + parseInt(datas.message, 10);
                        text.value = lastText.substring(0, start) + lastText.substring(end);
                        lastText = text.value;
                        preview.innerHTML = lastText;
                        result.innerText = "sync done";
                    } else if (datas.type === "content") {
                        text.value = datas.message;
                        lastText = datas.message;
                        preview.innerHTML = lastText;
                        result.innerText = "sync done";
                    } else if (datas.type === "sync") {
                        result.innerText = datas.message;
                    }
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
<div id="writer" style="display: flex; gap: 20px; align-items: flex-start;">
    <div style="flex: 1;">
        <label for="text" id="result">Enter text</label>
        <textarea name="text" rows="10" cols="40" id="text" style="width: 100%;"></textarea>
    </div>

    <div id="preview"
         style="flex: 1; border: 1px solid #ccc; padding: 10px; min-height: 160px; background: #f9f9f9;">
    </div>
</div>

</body>
</html>