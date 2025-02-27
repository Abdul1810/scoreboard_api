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
            const urlParams = new URLSearchParams(window.location.search);
            if (!urlParams.has('id')) {
                document.getElementById("result").innerText = "Match ID is missing.";
                return;
            }
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
            };

            socket.onmessage = function (event) {
                const data = JSON.parse(event.data);
                console.log(data);
                // {"team1_balls":0,"team2_runs":[0,0,0,0,0,0,0,0,0,0,0],"winner":"none","team2_balls":0,"current_batting":"team1","team2_score":0,"team2_wickets":0,"team1_wickets":0,"team1_runs":[0,0,0,0,0,0,0,0,0,0,0],"is_completed":"false","team1_score":0}
                // current_player score
                document.getElementById("team1score").value = data.team1_runs[data.team1_wickets];
                document.getElementById("team2score").value = data.team2_runs[data.team2_wickets];
                document.getElementById("team1balls").value = data.team1_balls;
                document.getElementById("team2balls").value = data.team2_balls;

                updateScoreTable(data);
                document.getElementById("team1-stats").textContent = `${data.team1_score}/${data.team1_wickets}`;
                document.getElementById("team2-stats").textContent = `${data.team2_score}/${data.team2_wickets}`;

                if (data.is_completed === "false") {
                    if (data.current_batting === "team1") {
                        current_batting = "team1";
                        document.getElementById("match-result").textContent = `${team1Players[data.team1_wickets]} from Team 1 is batting`;
                        document.getElementById("team1stats").style.backgroundColor = "aliceblue";
                        document.getElementById("team2stats").style.backgroundColor = "white";
                    } else {
                        current_batting = "team2";
                        document.getElementById("match-result").textContent = `${team2Players[data.team2_wickets]} from Team 2 is batting`;
                        document.getElementById("team2stats").style.backgroundColor = "aliceblue";
                        document.getElementById("team1stats").style.backgroundColor = "white";
                    }
                } else {
                    if (data.winner === undefined) {
                        document.getElementById("match-result").textContent = "";
                        return;
                    }
                    document.getElementById("match-result").textContent = data.winner !== 'Tie' ? data.winner + " won the match" : "Tie";
                    document.getElementById("team1balls").value = data.team1_balls;
                    document.getElementById("team2balls").value = data.team2_balls;
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

        function updateScoreTable(data) {
            const playerRow1 = document.getElementById("playerRow1");
            const runsRow1 = document.getElementById("runsRow1");
            const playerRow2 = document.getElementById("playerRow2");
            const runsRow2 = document.getElementById("runsRow2");

            playerRow1.innerHTML = "<th>Players</th>";
            runsRow1.innerHTML = "<td>Runs</td>";
            playerRow2.innerHTML = "<th>Players</th>";
            runsRow2.innerHTML = "<td>Runs</td>";

            for (let i = 0; i < team1Players.length; i++) {
                const playerCell = document.createElement("th");
                playerCell.textContent = team1Players[i] || "Player " + (i + 1);
                playerRow1.appendChild(playerCell);

                const runsCell = document.createElement("td");
                if (data.team1_wickets > i) {
                    runsCell.textContent = data.team1_runs[i] || 0;
                } else {
                    runsCell.textContent = "-";
                }
                runsRow1.appendChild(runsCell);
            }

            for (let i = 0; i < team2Players.length; i++) {
                const playerCell = document.createElement("th");
                playerCell.textContent = team2Players[i] || "Player " + (i + 1);
                playerRow2.appendChild(playerCell);

                const runsCell = document.createElement("td");
                if (data.team2_wickets > i) {
                    runsCell.textContent = data.team2_runs[i] || 0;
                } else {
                    runsCell.textContent = "-";
                }
                runsRow2.appendChild(runsCell);
            }
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

        table {
            width: 80%;
            margin: 30px auto;
            border-collapse: collapse;
            background-color: #fff;
            border: 1px solid #ddd;
            border-radius: 10px;
        }

        table th, table td {
            padding: 12px;
            text-align: center;
            border: 1px solid #ddd;
        }

        table th {
            background-color: #f2f2f2;
            color: #333;
            font-weight: bold;
        }

        table tr:nth-child(even) {
            background-color: #f9f9f9;
        }

        table tr:nth-child(odd) {
            background-color: #ffffff;
        }

        table tbody tr:hover {
            background-color: #e6f7ff;
        }

        table td {
            font-size: 14px;
        }
    </style>
</head>
<body>
<h3>Scoreboard - View</h3>
<p id="result"></p>
<p id="match-result"></p>
<div class="container">
    <div id="team1stats" class="team-stats">
        <label id="team1"></label>
        <h3 id="team1-stats" style="display: flex; justify-content: space-between;margin:0"></h3>
        <label for="team1score">Score:</label>
        <input type="number" id="team1score" placeholder="Enter score" value="0" disabled="disabled">

        <%--        <label for="team1wickets">Wickets:</label>--%>
        <%--        <input type="number" id="team1wickets" placeholder="Enter wickets" value="0">--%>

        <label for="team1balls">Total Balls:</label>
        <input type="number" id="team1balls" placeholder="Enter balls" value="0" disabled="disabled">
    </div>
    <div id="team2stats" class="team-stats">
        <label id="team2"></label>
        <h3 id="team2-stats" style="display: flex; justify-content: space-between;margin:0"></h3>
        <label for="team2score">Score:</label>
        <input type="number" id="team2score" placeholder="Enter score" value="0" disabled="disabled">

        <%--        <label for="team2wickets">Wickets:</label>--%>
        <%--        <input type="number" id="team2wickets" placeholder="Enter wickets" value="0">--%>

        <label for="team2balls">Total Balls:</label>
        <input type="number" id="team2balls" placeholder="Enter balls" value="0" disabled="disabled">
    </div>
</div>
<br>

<h3>Team1 Scoretable</h3>
<table border="1" style="width: 80%; margin: auto; border-collapse: collapse;">
    <thead>
    <tr id="playerRow1">
        <th>Players</th>
    </tr>
    </thead>
    <tbody>
    <tr id="runsRow1">
        <td>Runs</td>
    </tr>
    </tbody>
</table>

<h3>Team2 Scoretable</h3>
<table border="1" style="width: 80%; margin: auto; border-collapse: collapse;">
    <thead>
    <tr id="playerRow2">
        <th>Players</th>
    </tr>
    </thead>
    <tbody>
    <tr id="runsRow2">
        <td>Runs</td>
    </tr>
    </tbody>
</table>
</body>
</html>
