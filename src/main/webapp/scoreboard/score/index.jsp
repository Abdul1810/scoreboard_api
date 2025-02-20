<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Score Board</title>
    <script>
        let socket;
        document.addEventListener('DOMContentLoaded', function() {
            fetch('/api/matches?id=<%= request.getParameter("id") %>')
                .then(response => response.json())
                .then(data => {
                    if (!data) {
                        return;
                    }
                    document.getElementById("team1").innerText = data.team1;
                    document.getElementById("team2").innerText = data.team2;
                });
            socket = new WebSocket('ws://localhost:8080/ws/score?id=<%= request.getParameter("id") %>');
            socket.onmessage = function(event) {
                const data = JSON.parse(event.data);
                document.getElementById("team1score").innerText = data.team1;
                document.getElementById("team2score").innerText = data.team2;
            };
        });
    </script>
</head>
<body style="text-align: center; font-family: Arial, sans-serif;">
<h1>
    ScoreBoard
</h1>
<h4>
    <span id="team1"></span> - <span id="team1score">Loading...</span>
</h4>
<h4>
    <span id="team2"></span> - <span id="team2score">Loading...</span>
</h4>
</body>
</html>
