<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Score Updater</title>
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
                        document.getElementById("team1").innerText = data.team1;
                        document.getElementById("team2").innerText = data.team2;
                    }
                });
            socket = new WebSocket('ws://localhost:8080/ws/score?id=<%= request.getParameter("id") %>');
            socket.onmessage = function (event) {
                const data = JSON.parse(event.data);
                console.log(data);
                document.getElementById("team1score").textContent = data.team1;
                document.getElementById("team2score").textContent = data.team2;
                document.getElementById("team1wickets").textContent = data.team1wickets;
                document.getElementById("team2wickets").textContent = data.team2wickets;
                document.getElementById("team1balls").textContent = data.team1balls;
                document.getElementById("team2balls").textContent = data.team2balls;
                if (data.is_completed === "false") {
                    if (data.current_batting === "team1") {
                        current_batting = "team1";
                        document.getElementById("match-result").textContent = "Team 1 is batting";
                        document.getElementById("team1stats").style.backgroundColor = "lightgreen";
                        document.getElementById("team2stats").style.backgroundColor = "white";
                        document.getElementById("team1score").disabled = false;
                        document.getElementById("team1wickets").disabled = false;
                        document.getElementById("team1balls").disabled = false;
                        document.getElementById("team2score").disabled = true;
                        document.getElementById("team2wickets").disabled = true;
                        document.getElementById("team2balls").disabled = true;
                    } else {
                        current_batting = "team2";
                        document.getElementById("match-result").textContent = "Team 2 is batting";
                        document.getElementById("team2stats").style.backgroundColor = "lightgreen";
                        document.getElementById("team1stats").style.backgroundColor = "white";
                        document.getElementById("team2score").disabled = false;
                        document.getElementById("team2wickets").disabled = false;
                        document.getElementById("team2balls").disabled = false;
                        document.getElementById("team1score").disabled = true;
                        document.getElementById("team1wickets").disabled = true;
                        document.getElementById("team1balls").disabled = true;
                    }
                } else {
                    document.getElementById("match-result").textContent = data.winner + " won the match";
                    document.getElementById("team1score").disabled = true;
                    document.getElementById("team1wickets").disabled = true;
                    document.getElementById("team1balls").disabled = true;
                    document.getElementById("team2score").disabled = true;
                    document.getElementById("team2wickets").disabled = true;
                    document.getElementById("team2balls").disabled = true;
                }
            };
            socket.onclose = function () {
                document.getElementById("result").innerText = 'Match Removed';
            };
        });

        function updateScore() {
            const team1score = document.getElementById("team1score").value;
            const team2score = document.getElementById("team2score").value;
            const team1wickets = document.getElementById("team1wickets").value;
            const team2wickets = document.getElementById("team2wickets").value;
            const team1balls = document.getElementById("team1balls").value;
            const team2balls = document.getElementById("team2balls").value;
            if (isNaN(team1score) || isNaN(team2score)) {
                document.getElementById("result").innerText = 'Please enter valid scores';
                return;
            }
            if (team1score < 0 || team2score < 0) {
                document.getElementById("result").innerText = 'Please enter valid scores';
                return;
            }
            const result = document.getElementById("result");
            let resultData = {};
            if (current_batting === "team1") {
                resultData = {
                    team1: team1score,
                    team1_wickets: team1wickets,
                    team1_balls: team1balls,
                };
            } else if (current_batting === "team2") {
                resultData = {
                    team2: team2score,
                    team2_wickets: team2wickets,
                    team2_balls: team2balls,
                };
            }
            fetch('/update-score?id=<%= request.getParameter("id") %>', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(resultData)
            })
                .then(response => response.text())
                .then(data => {
                    result.innerHTML = data;
                });
        }
    </script>
</head>
<body style="text-align: center; font-family: Arial, sans-serif;">
<h3>
    Score Updater
</h3>
<p id="result">
</p>
<p id="match-result">
</p>
<div id="team1stats">
    <label id="team1"></label>
    <br/>
    <label for="team1score">
        Score:
    </label>
    <input type="number" id="team1score" placeholder="Enter score" value="0">
    <label for="team1wickets">
        Wickets:
    </label>
    <input type="number" id="team1wickets" placeholder="Enter wickets" value="0">
    <label for="team1balls">
        Balls:
    </label>
    <input type="number" id="team1balls" placeholder="Enter balls" value="0">
</div>
<br>
<br>
<div id="team2stats">
    <label id="team2"></label>
    <br/>
    <label for="team2score">
        Score:
    </label>
    <input type="number" id="team2score" placeholder="Enter score" value="0">
    <label for="team2wickets">
        Wickets:
    </label>
    <input type="number" id="team2wickets" placeholder="Enter wickets" value="0">
    <label for="team2balls">
        Balls:
    </label>
    <input type="number" id="team2balls" placeholder="Enter balls" value="0">
</div>
<br>
<br>
<button onclick="updateScore()">Update</button>
</body>
</html>
