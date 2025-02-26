<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Loading...</title>
    <script>
        let socket;
        let current_batting = "team1";
        let team1Players = [];
        let team2Players = [];

        document.addEventListener('DOMContentLoaded', function () {
            fetch('/api/matches?id=<%= request.getParameter("id") %>')
                .then(response => response.json())
                .then(data => {
                    if (data.message) {
                        document.getElementById("result").innerText = data.message;
                        document.title = data.message;
                    } else {
                        document.title = data.team1 + ' vs ' + data.team2;
                        document.getElementById("team1").innerText = data.team1;
                        document.getElementById("team2").innerText = data.team2;
                        team1Players = data.team1_players;
                        team2Players = data.team2_players;
                    }
                    initWS();
                });
        });

        function initWS() {
            socket = new WebSocket('ws://localhost:8080/ws/stats?id=<%= request.getParameter("id") %>');

            socket.onopen = function () {
                console.log('Connected to the server');
                document.getElementById("result").innerText = "Live Connected";
            };

            socket.onmessage = function (event) {
                const data = JSON.parse(event.data);
                console.log(data);
                // {"team1_balls":0,"team2_runs":[0,0,0,0,0,0,0,0,0,0,0],"winner":"none","team2_balls":0,"current_batting":"team1","team2_score":0,"team2_wickets":0,"team1_wickets":0,"team1_runs":[0,0,0,0,0,0,0,0,0,0,0],"is_completed":"false","team1_score":0}
                // current_player score
                document.getElementById("team1score").value = data.team1_runs[data.team1_wickets];
                document.getElementById("team2score").value = data.team2_runs[data.team2_wickets];
                // document.getElementById("team1wickets").value = data.team1_wickets;
                // document.getElementById("team2wickets").value = data.team2_wickets;
                document.getElementById("team1balls").value = data.team1_balls;
                document.getElementById("team2balls").value = data.team2_balls;
                if (data.is_completed === "false") {
                    if (data.current_batting === "team1") {
                        current_batting = "team1";
                        document.getElementById("match-result").textContent = `${team1Players[data.team1_wickets]} from Team 1 is batting`;
                        document.getElementById("match-stats").textContent = `Team1: ${data.team1_score}/${data.team1_wickets}\t\tTeam2: ${data.team2_score}/${data.team2_wickets}`;
                        document.getElementById("team1stats").style.backgroundColor = "aliceblue";
                        document.getElementById("team2stats").style.backgroundColor = "white";
                    } else {
                        current_batting = "team2";
                        document.getElementById("match-result").textContent = `${team2Players[data.team2_wickets]} from Team 2 is batting`;
                        document.getElementById("match-stats").textContent = `Team1: ${data.team1_score}/${data.team1_wickets}\t\tTeam2: ${data.team2_score}/${data.team2_wickets}`;
                        document.getElementById("team2stats").style.backgroundColor = "aliceblue";
                        document.getElementById("team1stats").style.backgroundColor = "white";
                    }
                } else {
                    if (data.winner === undefined) {
                        document.getElementById("match-result").textContent = "";
                        return;
                    }
                    document.getElementById("match-result").textContent = data.winner !== 'Tie' ? data.winner + " won the match" : "Tie";
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
            font-size: 16px;
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

    </style>
</head>
<body>
<h3>Scoreboard - Update</h3>
<p id="result"></p>
<p id="match-result"></p>
<p id="match-stats"></p>
<div class="container">
    <div id="team1stats" class="team-stats">
        <label id="team1"></label>
        <label for="team1score">Score:</label>
        <input type="number" id="team1score" placeholder="Enter score" value="0" disabled="disabled">

        <%--        <label for="team1wickets">Wickets:</label>--%>
        <%--        <input type="number" id="team1wickets" placeholder="Enter wickets" value="0">--%>

        <label for="team1balls">Total Balls:</label>
        <input type="number" id="team1balls" placeholder="Enter balls" value="0" disabled="disabled">
    </div>
    <div id="team2stats" class="team-stats">
        <label id="team2"></label>
        <label for="team2score">Score:</label>
        <input type="number" id="team2score" placeholder="Enter score" value="0" disabled="disabled">

        <%--        <label for="team2wickets">Wickets:</label>--%>
        <%--        <input type="number" id="team2wickets" placeholder="Enter wickets" value="0">--%>

        <label for="team2balls">Total Balls:</label>
        <input type="number" id="team2balls" placeholder="Enter balls" value="0" disabled="disabled">
    </div>
</div>
<br>
</body>
</html>
