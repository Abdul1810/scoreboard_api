<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Create New Match</title>
    <script>
        let socket;
        let matchesData = [];
        let currentMatch = {};

        document.addEventListener('DOMContentLoaded', function () {
            socket = new WebSocket('ws://localhost:8080/ws/matches?id=<%= request.getParameter("id") %>');
            setValueEmpty();

            socket.onopen = function () {
                console.log("WebSocket connection opened.");
                document.getElementById("result").innerText = "Connected";
            };

            socket.onmessage = function (event) {
                const jsonData = JSON.parse(event.data);
                console.log("Message received: ", jsonData);

                if (jsonData.action === "MODIFY") {
                    if (!jsonData.data) {
                        document.getElementById("result").innerText = "Match not found";
                        document.getElementById("match-result").innerText = "";
                        return;
                    }
                    currentMatch = jsonData.data;
                    matchesData = matchesData.map(m => m.id === currentMatch.id ? currentMatch : m);
                    updateUI();
                }
            };

            socket.onclose = function () {
                console.log("WebSocket connection closed.");
                document.getElementById("result").innerText = "Connection closed. Refreshing...";
                setTimeout(() => location.reload(), 3000);
            };

            socket.onerror = function (event) {
                console.error("WebSocket error: ", event);
            };
        });

        function updateUI() {
            document.getElementById("team1").innerText = currentMatch.team1;
            document.getElementById("team2").innerText = currentMatch.team2;
            document.getElementById("team1score").value = currentMatch.team1_score;
            document.getElementById("team1wickets").value = currentMatch.team1_wickets;
            document.getElementById("team1balls").value = currentMatch.team1_balls;
            document.getElementById("team2score").value = currentMatch.team2_score;
            document.getElementById("team2wickets").value = currentMatch.team2_wickets;
            document.getElementById("team2balls").value = currentMatch.team2_balls;

            let matchResultText = "";

            if (currentMatch.is_completed === "true") {
                matchResultText = currentMatch.winner === "tie" ? "Tie" : currentMatch.winner + " won the match";

                // Color logic for winners
                if (currentMatch.winner === "team1") {
                    document.getElementById("team1stats").style.backgroundColor = "lightgreen";
                    document.getElementById("team2stats").style.backgroundColor = "white";
                } else if (currentMatch.winner === "team2") {
                    document.getElementById("team2stats").style.backgroundColor = "lightgreen";
                    document.getElementById("team1stats").style.backgroundColor = "white";
                } else {
                    document.getElementById("team1stats").style.backgroundColor = "peachpuff";
                    document.getElementById("team2stats").style.backgroundColor = "peachpuff";
                }

                disableAllInputs();
            } else {
                matchResultText = currentMatch.current_batting === "team1" ? "Team 1 is batting" : "Team 2 is batting";

                // Color logic for batting
                if (currentMatch.current_batting === "team1") {
                    document.getElementById("team1stats").style.backgroundColor = "aliceblue";
                    document.getElementById("team2stats").style.backgroundColor = "white";
                    enableInputs("team1");
                    disableInputs("team2");
                } else {
                    document.getElementById("team2stats").style.backgroundColor = "aliceblue";
                    document.getElementById("team1stats").style.backgroundColor = "white";
                    enableInputs("team2");
                    disableInputs("team1");
                }
            }

            document.getElementById("match-result").innerText = matchResultText;
        }

        function updateScore() {
            currentMatch.team1_score = document.getElementById("team1score").value;
            currentMatch.team1_wickets = document.getElementById("team1wickets").value;
            currentMatch.team1_balls = document.getElementById("team1balls").value;
            currentMatch.team2_score = document.getElementById("team2score").value;
            currentMatch.team2_wickets = document.getElementById("team2wickets").value;
            currentMatch.team2_balls = document.getElementById("team2balls").value;

            matchesData = matchesData.map(m => m.id === currentMatch.id ? currentMatch : m);

            socket.send(JSON.stringify({
                action: "MODIFY",
                data: currentMatch,
            }));
        }

        function disableAllInputs() {
            document.querySelectorAll("input").forEach(input => input.disabled = true);
        }

        function disableInputs(team) {
            document.getElementById(`${team}score`).disabled = true;
            document.getElementById(`${team}wickets`).disabled = true;
            document.getElementById(`${team}balls`).disabled = true;
        }

        function enableInputs(team) {
            document.getElementById(`${team}score`).disabled = false;
            document.getElementById(`${team}wickets`).disabled = false;
            document.getElementById(`${team}balls`).disabled = false;
        }

        function setValueEmpty() {
            document.querySelectorAll("input").forEach(input => input.value = "");
            document.getElementById("match-result").innerText = "";
        }
    </script>
    <style>
        body {
            text-align: center;
            font-family: Arial, sans-serif;
            background-color: #f8f9fa;
            margin: 0;
            padding: 20px;
        }

        h3 {
            color: #333;
        }

        #result, #match-result {
            font-size: 18px;
            font-weight: bold;
            margin-bottom: 15px;
        }

        .container {
            display: flex;
            justify-content: center;
            gap: 30px;
        }

        .team-stats {
            background: white;
            padding: 20px;
            border-radius: 10px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
            width: 250px;
        }

        label {
            display: block;
            font-weight: bold;
            margin-bottom: 5px;
            text-align: left;
        }

        input {
            width: 100%;
            padding: 8px;
            font-size: 16px;
            border: 1px solid #ccc;
            border-radius: 5px;
            margin-bottom: 10px;
        }

        button {
            background-color: #ff1a5f;
            color: white;
            font-size: 16px;
            padding: 10px 20px;
            border: none;
            border-radius: 5px;
            cursor: pointer;
            transition: background 0.3s;
        }

        button:hover {
            background-color: #ff3366;
        }
    </style>
</head>
<body>
<h3>Score Updater</h3>
<p id="result"></p>
<p id="match-result"></p>
<div class="container">
    <div id="team1stats" class="team-stats">
        <label id="team1"></label>
        <label>Score:</label><input type="number" id="team1score">
        <label>Wickets:</label><input type="number" id="team1wickets">
        <label>Balls:</label><input type="number" id="team1balls">
    </div>
    <div id="team2stats" class="team-stats">
        <label id="team2"></label>
        <label>Score:</label><input type="number" id="team2score">
        <label>Wickets:</label><input type="number" id="team2wickets">
        <label>Balls:</label><input type="number" id="team2balls">
    </div>
</div>
<br>
<button onclick="updateScore()">Update</button>
</body>
</html>
