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
                        document.getElementById("team1table").innerText = data.team1 + " Score-table";
                        document.getElementById("team2table").innerText = data.team2 + " Score-table";
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
                updateScoreTable(data);
                document.getElementById("team1score").value = data.team1_runs[data.team2_wickets];
                document.getElementById("team2score").value = data.team2_runs[data.team1_wickets];
                // document.getElementById("team1wickets").value = data.team1_wickets;
                // document.getElementById("team2wickets").value = data.team2_wickets;
                document.getElementById("team1balls").value = data.team1_balls;
                document.getElementById("team2balls").value = data.team2_balls;
                document.getElementById("team1-stats").textContent = `${data.team1_score}/${data.team2_wickets}`;
                document.getElementById("team2-stats").textContent = `${data.team2_score}/${data.team1_wickets}`;

                if (data.is_completed === "false") {
                    if (data.current_batting === "team1") {
                        current_batting = "team1";
                        document.getElementById("match-result").textContent = `${team1Players[data.team1_wickets]} from Team 1 is batting`;
                        document.getElementById("team1stats").style.backgroundColor = "aliceblue";
                        document.getElementById("team2stats").style.backgroundColor = "white";
                        document.getElementById("team1score").disabled = false;
                        // document.getElementById("team1wickets").disabled = false;
                        document.getElementById("team1-out").disabled = false;
                        document.getElementById("team1balls").disabled = false;
                        document.getElementById("team2score").disabled = true;
                        // document.getElementById("team2wickets").disabled = true;
                        document.getElementById("team2-out").disabled = true;
                        document.getElementById("team2balls").disabled = true;
                    } else {
                        current_batting = "team2";
                        document.getElementById("match-result").textContent = `${team2Players[data.team2_wickets]} from Team 2 is batting`;
                        document.getElementById("team2stats").style.backgroundColor = "aliceblue";
                        document.getElementById("team1stats").style.backgroundColor = "white";
                        document.getElementById("team2score").disabled = false;
                        // document.getElementById("team2wickets").disabled = false;
                        document.getElementById("team2-out").disabled = false;
                        document.getElementById("team2balls").disabled = false;
                        document.getElementById("team1score").disabled = true;
                        // document.getElementById("team1wickets").disabled = true;
                        document.getElementById("team1-out").disabled = true;
                        document.getElementById("team1balls").disabled = true;
                    }
                } else {
                    if (data.winner === undefined) {
                        document.getElementById("match-result").textContent = "";
                        return;
                    }
                    document.getElementById("team1balls").value = data.team1_balls;
                    document.getElementById("team2balls").value = data.team2_balls;
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
                    document.getElementById("team1score").disabled = true;
                    // document.getElementById("team1wickets").disabled = true;
                    document.getElementById("team1-out").disabled = true;
                    document.getElementById("team1balls").disabled = true;
                    document.getElementById("team2score").disabled = true;
                    // document.getElementById("team2wickets").disabled = true;
                    document.getElementById("team2-out").disabled = true;
                    document.getElementById("team2balls").disabled = true;
                }
            };
            socket.onclose = function () {
                document.getElementById("result").innerText = "Connection closed. Refreshing...";
                setTimeout(() => {
                    location.reload();
                }, 3000);
            };
        }

        function updateScore(isOut = false) {
            const result = document.getElementById("result");
            let resultData = {};

            if (current_batting === "team1") {
                const team1score = document.getElementById("team1score").value;
                const team1balls = document.getElementById("team1balls").value;
                if (isNaN(team1score)) {
                    document.getElementById("result").innerText = 'Please enter valid score';
                    return;
                }
                if (team1score < 0) {
                    document.getElementById("result").innerText = 'Please enter valid score';
                    return;
                }
                resultData = {
                    score: team1score,
                    balls: team1balls,
                    out: isOut ? "true" : "false"
                };
            } else if (current_batting === "team2") {
                const team2score = document.getElementById("team2score").value;
                const team2balls = document.getElementById("team2balls").value;
                if (isNaN(team2score)) {
                    document.getElementById("result").innerText = 'Please enter valid score';
                    return;
                }
                if (team2score < 0) {
                    document.getElementById("result").innerText = 'Please enter valid score';
                    return;
                }
                resultData = {
                    score: team2score,
                    balls: team2balls,
                    out: isOut ? "true" : "false"
                };
            }

            result.innerText = 'Updating...';
            fetch('/update-stats?id=<%= request.getParameter("id") %>', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(resultData),
            })
                .then(response => response.json())
                .then(data => {
                    document.getElementById("result").innerText = data.message;
                });
        }

        function updateScoreTable(data) {
            const playerRow1 = document.getElementById("playerRow1");
            const runsRow1 = document.getElementById("runsRow1");
            const outRow1 = document.getElementById("outRow1");
            const playerRow2 = document.getElementById("playerRow2");
            const runsRow2 = document.getElementById("runsRow2");
            const outRow2 = document.getElementById("outRow2");

            playerRow1.innerHTML = "<th>Players</th>";
            runsRow1.innerHTML = "<td>Runs</td>";
            outRow1.innerHTML = "<td>Wickets</td>";
            playerRow2.innerHTML = "<th>Players</th>";
            runsRow2.innerHTML = "<td>Runs</td>";
            outRow2.innerHTML = "<td>Wickets</td>";

            for (let i = 0; i < team1Players.length; i++) {
                const playerCell = document.createElement("th");
                playerCell.textContent = team1Players[i] || "Player " + (i + 1);
                playerRow1.appendChild(playerCell);

                const runsCell = document.createElement("td");
                if (data.team2_wickets >= i) {
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
                if (current_batting === "team1") {
                    runsCell.textContent = "-";
                } else {
                    if (data.team1_wickets >= i) {
                        runsCell.textContent = data.team2_runs[i] || 0;
                    } else {
                        runsCell.textContent = "-";
                    }
                }
                runsRow2.appendChild(runsCell);
            }

            for (let i = 0; i < data.team2_outs.length; i++) {
                if (data.team2_outs[i] !== 0) {
                    for (let j = 0; j < data.team2_outs[i]; j++) {
                        const outCell = document.createElement("td");
                        outCell.textContent = team2Players[i];
                        outRow1.appendChild(outCell);
                    }
                }
            }

            for (let i = 0; i < data.team1_outs.length; i++) {
                if (data.team1_outs[i] !== 0) {
                    for (let j = 0; j < data.team1_outs[i]; j++) {
                        const outCell = document.createElement("td");
                        outCell.textContent = team1Players[i];
                        outRow2.appendChild(outCell);
                    }
                }
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

        button {
            background-color: #ff1a5f;
            color: white;
            font-size: 16px;
            padding: 8px 12px;
            border: none;
            border-radius: 60px;
            cursor: pointer;
            transition: background 0.3s;
        }

        button:disabled {
            background-color: #ccc;
            cursor: not-allowed;
        }

        button:hover {
            background-color: #ff3366;
        }

        button:disabled:hover {
            background-color: #ccc;
        }

        .update-btn {
            background-color: #007bff;
            color: white;
            padding: 10px 20px;
            border: none;
            border-radius: 5px;
            cursor: pointer;
            transition: background 0.3s;
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
<h3>Scoreboard - Update</h3>
<p id="result"></p>
<p id="match-result"></p>
<div class="container">
    <div id="team1stats" class="team-stats">
        <label id="team1"></label>
        <h3 id="team1-stats" style="display: flex; justify-content: space-between;margin:0"></h3>
        <label for="team1score">Score:</label>
        <input type="number" id="team1score" placeholder="Enter score" value="0">


        <label for="team1balls">Total Balls:</label>
        <input type="number" id="team1balls" placeholder="Enter balls" value="0">

        <button onclick="updateScore(true)" id="team1-out">
            🚫 Out
        </button>
    </div>
    <div id="team2stats" class="team-stats">
        <label id="team2"></label>
        <h3 id="team2-stats" style="display: flex; justify-content: space-between;margin:0"></h3>
        <label for="team2score">Score:</label>
        <input type="number" id="team2score" placeholder="Enter score" value="0">

        <label for="team2balls">Total Balls:</label>
        <input type="number" id="team2balls" placeholder="Enter balls" value="0">

        <button onclick="updateScore(true)" id="team2-out">
            🚫 Out
        </button>
    </div>
</div>
<br>
<button class="update-btn" onclick="updateScore(false)">Update</button>
<br />

<h3 id="team1table">Team1 Score-table</h3>
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
    <tr id="outRow1">
        <td>Wickets</td>
    </tr>
    </tbody>
</table>

<h3 id="team2table">Team2 Score-table</h3>
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
    <tr id="outRow2">
        <td>Wickets</td>
    </tr>
    </tbody>
</table>
</body>
</html>
