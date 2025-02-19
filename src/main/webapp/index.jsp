<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Photo Stream</title>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <script>
        let socket;
        let chunks = [];
        let imgElement;
        let oldBlob;

        document.addEventListener("DOMContentLoaded", function () {
            socket = new WebSocket("ws://localhost:8080/imagews");
            imgElement = document.getElementById("result");

            const testConnectionSpeed = {
                imageAddr: "https://upload.wikimedia.org/wikipedia/commons/a/a6/Brandenburger_Tor_abends.jpg", // this is just an example, you rather want an image hosted on your server
                downloadSize: 2707459,
                run: function (mbps_max, cb_gt, cb_lt) {
                    testConnectionSpeed.mbps_max = parseFloat(mbps_max) ? parseFloat(mbps_max) : 0;
                    testConnectionSpeed.cb_gt = cb_gt;
                    testConnectionSpeed.cb_lt = cb_lt;
                    testConnectionSpeed.InitiateSpeedDetection();
                },
                InitiateSpeedDetection: function () {
                    window.setTimeout(testConnectionSpeed.MeasureConnectionSpeed, 1);
                },
                result: function () {
                    const duration = (endTime - startTime) / 1000;
                    const bitsLoaded = testConnectionSpeed.downloadSize * 8;
                    const speedBps = (bitsLoaded / duration).toFixed(2);
                    const speedKbps = (speedBps / 1024).toFixed(2);
                    const speedMbps = (speedKbps / 1024).toFixed(2);
                    if (speedMbps >= (testConnectionSpeed.max_mbps ? testConnectionSpeed.max_mbps : 1)) {
                        testConnectionSpeed.cb_gt ? testConnectionSpeed.cb_gt(speedMbps) : false;
                    } else {
                        testConnectionSpeed.cb_lt ? testConnectionSpeed.cb_lt(speedMbps) : false;
                    }
                },
                MeasureConnectionSpeed: function () {
                    const download = new Image();
                    download.onload = function () {
                        endTime = (new Date()).getTime();
                        testConnectionSpeed.result();
                    }
                    startTime = (new Date()).getTime();
                    const cacheBuster = "?nnn=" + startTime;
                    download.src = testConnectionSpeed.imageAddr + cacheBuster;
                }
            }
            // testConnectionSpeed.run(1.5, function (speedMbps) {
            //     console.log("Connection speed is greater than 1 Mbps: " + speedMbps + " Mbps");
            // }, function (speedMbps) {
            //     console.log("Connection speed is less than 1 Mbps: " + speedMbps + " Mbps");
            // });

            socket.onmessage = function (event) {
                if (event.data instanceof Blob) {
                    socket.send(`A`);
                    chunks.push(event.data);
                    if (oldBlob) {
                        URL.revokeObjectURL(oldBlob);
                    }
                    oldBlob = new Blob(chunks, {type: "image/jpeg"});
                    imgElement.src = URL.createObjectURL(oldBlob);
                }
            };

            socket.onerror = function (error) {
                console.error("WebSocket Error:", error);
            };

            socket.onclose = function () {
                console.log("WebSocket closed.");
            };
        });
    </script>
</head>
<body style="text-align: center; font-family: Arial, sans-serif;">
<h1>Photo Stream</h1>
<p>Java, WebSocket, JSP, Servlet</p>
<img id="result" style="max-width: 100%; height: auto; border: 1px solid #ccc;"/>
</body>
</html>
