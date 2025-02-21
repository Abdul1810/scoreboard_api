<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Loading...</title>
    <script>
        let socket;
        let current_batting = "team1";

        document.addEventListener('DOMContentLoaded', function () {
            fetch('/api/matches?id=<%= request.getParameter("id") %>')
                .then(response => response.json())
                .then(data => {
                    if (data.error) {
                        document.getElementById("result").innerText = data.error;
                    } else {
                        document.title = data.team1 + ' vs ' + data.team2;
                        document.getElementById("team1").innerText = data.team1;
                        document.getElementById("team2").innerText = data.team2;
                    }
                });

            socket = new WebSocket('ws://localhost:8080/ws/stats?id=<%= request.getParameter("id") %>');
            socket.onmessage = function (event) {
                const data = JSON.parse(event.data);
                document.getElementById("team1score").innerText = data.team1;
                document.getElementById("team2score").innerText = data.team2;
                document.getElementById("team1wickets").innerText = data.team1_wickets;
                document.getElementById("team2wickets").innerText = data.team2_wickets;
                document.getElementById("team1balls").innerText = data.team1_balls;
                document.getElementById("team2balls").innerText = data.team2_balls;

                if (data.is_completed === "false") {
                    current_batting = data.current_batting;
                    document.getElementById("match-result").innerText =
                        (current_batting === "team1" ? "Team 1 is batting" : "Team 2 is batting");
                    updateUI();
                } else {
                    document.getElementById("match-result").textContent = data.winner !== 'tie' ? data.winner + " won the match" : "Tie";
                    if (data.winner === "team1") {
                        document.getElementById("team1stats").style.backgroundColor = "lightgreen";
                        document.getElementById("team2stats").style.backgroundColor = "white";
                    } else if (data.winner === "team2") {
                        document.getElementById("team2stats").style.backgroundColor = "lightgreen";
                        document.getElementById("team1stats").style.backgroundColor = "white";
                    } else {
                        document.getElementById("team1stats").style.backgroundColor = "peachpuff";
                        document.getElementById("team2stats").style.backgroundColor = "peachpuff";
                    }
                }
            };

            socket.onclose = function () {
                document.getElementById("result").innerText = "Connection closed. Refreshing...";
                setTimeout(() => {
                    location.reload();
                }, 3000);
            };
        });

        function updateUI() {
            document.getElementById("team1stats").style.backgroundColor =
                (current_batting === "team1") ? "aliceblue" : "white";
            document.getElementById("team2stats").style.backgroundColor =
                (current_batting === "team2") ? "aliceblue" : "white";
        }
    </script>
</head>
<body style="text-align: center; font-family: Arial, sans-serif;">
<h2>Scoreboard</h2>
<p id="result"></p>
<p id="match-result"></p>
<div id="team1stats" style="border: 1px solid #000; padding: 10px; margin-bottom: 10px;">
    <h3><span id="team1"></span></h3>
    <p>Score: <span id="team1score">0</span> | Wickets: <span id="team1wickets">0</span> | Balls: <span id="team1balls">0</span></p>
</div>
<div id="team2stats" style="border: 1px solid #000; padding: 10px;">
    <h3><span id="team2"></span></h3>
    <p>Score: <span id="team2score">0</span> | Wickets: <span id="team2wickets">0</span> | Balls: <span id="team2balls">0</span></p>
</div>
</body>
</html>
