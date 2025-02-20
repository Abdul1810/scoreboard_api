<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Score Board</title>
  <script>
      let socket;
    document.addEventListener('DOMContentLoaded', function() {
        socket = new WebSocket('ws://localhost:8080/score');
        socket.onmessage = function(event) {
            const data = JSON.parse(event.data);
            document.getElementById("team1").innerText = data.team1;
            document.getElementById("team2").innerText = data.team2;
        };
    });
  </script>
</head>
<body style="text-align: center; font-family: Arial, sans-serif;">
<h1>
  ScoreBoard
</h1>
<h4>
  CSK - <span id="team1">Loading...</span>
</h4>
<h4>
  MI - <span id="team2">Loading...</span>
</h4>
</body>
</html>
