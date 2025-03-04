<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Loading...</title>
    <script src="https://cdn.jsdelivr.net/npm/canvas-confetti@1.4.0/dist/confetti.browser.min.js"></script>
    <script>
        let socket;
        let current_batting = "team1";
        let team1Players = [];
        let team2Players = [];
        let reconnectInterval = 3000;

        document.addEventListener('DOMContentLoaded', function () {
            fetch('/api/matches?id=<%= request.getParameter("id") %>')
                .then(response => {
                    if (response.status >= 200 && response.status < 300) {
                        return response.json();
                    } else {
                        return response.json().then(errorData => {
                            throw new Error(errorData.message || 'Something went wrong');
                        });
                    }
                })
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
                        document.getElementById("team1outtable").innerText = data.team1 + " Wicket-table";
                        document.getElementById("team2outtable").innerText = data.team2 + " Wicket-table";
                        team1Players = data.team1_players;
                        team2Players = data.team2_players;
                    }
                    initWS();
                })
                .catch(error => {
                    console.error("Fetch Error:", error);
                    document.getElementById('result').innerText = error.message;
                    document.title = error.message;
                    disableAllButtons();
                });
        });

        function initWS() {
            socket = new WebSocket('ws://localhost:8080/ws/stats?id=<%= request.getParameter("id") %>');

            socket.onopen = function () {
                console.log('Connected to the server');
                document.getElementById("result").innerText = "";
            };

            socket.onmessage = function (event) {
                const data = JSON.parse(event.data);
                console.log(data);
                updateScoreTable(data);
                updateWicketsTable(data);
                current_batting = data.current_batting || "team1";
                document.getElementById("team1-stats").textContent = `${data.team1_score}/${data.team2_wickets} (${data.team1_balls})`;
                document.getElementById("team2-stats").textContent = `${data.team2_score}/${data.team1_wickets} (${data.team2_balls})`;

                if (data.is_completed === "false") {
                    if (data.current_batting === "team1") {
                        document.getElementById("match-result").textContent = `${team1Players[data.active_batsman_index-1]} is batting\n${team1Players[data.passive_batsman_index-1]} is waiting`;
                        document.getElementById("team1stats").style.backgroundColor = "aliceblue";
                        document.getElementById("team2stats").style.backgroundColor = "white";
                        document.getElementsByClassName("team-stats")[0].querySelectorAll("button").forEach(button => button.disabled = false);
                        document.getElementsByClassName("team-stats")[1].querySelectorAll("button").forEach(button => button.disabled = true);
                        document.getElementById("team2-out").disabled = true;
                        document.getElementById("bowler1").textContent = `Bowler: ${getBowlerName(data.team1_balls)}`;
                        document.getElementById("bowler2").textContent = "";
                    } else {
                        document.getElementById("match-result").textContent = `${team2Players[data.active_batsman_index-1]} is batting\n${team2Players[data.passive_batsman_index-1]} is waiting`;
                        document.getElementById("team2stats").style.backgroundColor = "aliceblue";
                        document.getElementById("team1stats").style.backgroundColor = "white";
                        document.getElementsByClassName("team-stats")[1].querySelectorAll("button").forEach(button => button.disabled = false);
                        document.getElementsByClassName("team-stats")[0].querySelectorAll("button").forEach(button => button.disabled = true);
                        document.getElementById("team1-out").disabled = true;
                        document.getElementById("bowler2").textContent = `Bowler: ${getBowlerName(data.team2_balls)}`;
                        document.getElementById("bowler1").textContent = "";
                    }
                } else {
                    if (data.winner === undefined) {
                        document.getElementById("match-result").textContent = "";
                        disableAllButtons();
                        return;
                    }
                    document.getElementById("match-result").textContent = data.winner !== 'Tie' ? data.winner + " won the match" : "Tie";
                    if (data.winner === "team1") {
                        document.getElementById("team1stats").style.backgroundColor = "lightgreen";
                        document.getElementById("team2stats").style.backgroundColor = "white";
                        if (!document.getElementById("team1").textContent.includes("Winner")) {
                            document.getElementById("team1").textContent += " (🎉 Winner)";
                            triggerConfetti("team1stats");
                        }
                        triggerConfetti("team1stats");
                    } else if (data.winner === "team2") {
                        document.getElementById("team2stats").style.backgroundColor = "lightgreen";
                        document.getElementById("team1stats").style.backgroundColor = "white";
                        if (!document.getElementById("team2").textContent.includes("Winner")) {
                            document.getElementById("team2").textContent += " (🎉 Winner)";
                            triggerConfetti("team2stats");
                        }
                    } else {
                        document.getElementById("team1stats").style.backgroundColor = "peachpuff";
                        document.getElementById("team2stats").style.backgroundColor = "peachpuff";
                    }
                    disableAllButtons();
                }
            };

            socket.onclose = function () {
                document.getElementById("result").innerText = "Connection closed. Attempting to reconnect...";
                setTimeout(initWS, reconnectInterval);
            };

            socket.onerror = function () {
                document.getElementById("result").innerText = "Connection error. Attempting to reconnect...";
                socket.close();
            };
        }

        function getBowlerName(balls) {
            let bowlerIndex;
            if (balls <= 66) {
                let over = Math.floor(balls / 6);
                let ball = balls % 6;
                bowlerIndex = over + (ball > 0 ? 1 : 0);
            } else {
                balls -= 66;
                let over = Math.floor(balls / 6);
                let ball = balls % 6;
                bowlerIndex = over + (ball > 0 ? 1 : 0);
            }

            if (bowlerIndex < 0 || bowlerIndex > 11) {
                return "Bowler";
            } else {
                return current_batting === "team1" ? team2Players[bowlerIndex - 1] : team1Players[bowlerIndex - 1];
            }
        }

        function disableAllButtons() {
            const buttons = document.querySelectorAll('button');
            buttons.forEach(button => button.disabled = true);
        }

        function disableCurrentTeamButtons() {
            const team1Buttons = document.getElementById("team1-container").querySelectorAll("button");
            const team2Buttons = document.getElementById("team2-container").querySelectorAll("button");
            const team1OutButton = document.getElementById("team1-out");
            const team2OutButton = document.getElementById("team2-out");
            if (current_batting === "team1") {
                team1Buttons.forEach(button => button.disabled = true);
                team1OutButton.disabled = true;
            } else {
                team2Buttons.forEach(button => button.disabled = true);
                team2OutButton.disabled = true;
            }
        }

        function updateScore(value, team) {
            const resultData = {
                update: `${value}`,
            }
            disableCurrentTeamButtons();
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
            outRow1.innerHTML = "<td>Wicket Taker</td>";
            playerRow2.innerHTML = "<th>Players</th>";
            runsRow2.innerHTML = "<td>Runs</td>";
            outRow2.innerHTML = "<td>Wicket Taker</td>";

            const team1_runs = Object.values(data.team1_runs);
            const team2_runs = Object.values(data.team2_runs);

            for (let i = 0; i < team1Players.length; i++) {
                const playerCell = document.createElement("th");
                playerCell.textContent = team1Players[i] || "Player " + (i + 1);
                playerRow1.appendChild(playerCell);

                const runsCell = document.createElement("td");
                if (data.team2_wickets >= i || team1_runs[i] !== 0) {
                    runsCell.textContent = team1_runs[i] || 0;
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
                if (current_batting === "team1" && data.is_completed === false) {
                    runsCell.textContent = "-";
                } else {
                    if (data.team1_wickets >= i || team2_runs[i] !== 0) {
                        runsCell.textContent = team2_runs[i] || 0;
                    } else {
                        runsCell.textContent = "-";
                    }
                }
                runsRow2.appendChild(runsCell);
            }

            for (let i = 0; i < data.team1_wickets_map.length; i++) {
                if (data.team1_wickets_map[i] !== null) {
                    const outCell = document.createElement("td");
                    outCell.textContent = data.team1_wickets_map[i];
                    outRow1.appendChild(outCell);
                } else {
                    const outCell = document.createElement("td");
                    outCell.textContent = "-";
                    outRow1.appendChild(outCell);
                }
            }

            for (let i = 0; i < data.team2_wickets_map.length; i++) {
                if (data.team2_wickets_map[i] !== null) {
                    const outCell = document.createElement("td");
                    outCell.textContent = data.team2_wickets_map[i];
                    outRow2.appendChild(outCell);
                } else {
                    const outCell = document.createElement("td");
                    outCell.textContent = "-";
                    outRow2.appendChild(outCell);
                }
            }
        }

        function updateWicketsTable(data) {
            const playeroutRow1 = document.getElementById("playeroutRow1");
            const ballsRow1 = document.getElementById("ballsRow1");
            const wicketsRow1 = document.getElementById("wicketsRow1");
            const playeroutRow2 = document.getElementById("playeroutRow2");
            const ballsRow2 = document.getElementById("ballsRow2");
            const wicketsRow2 = document.getElementById("wicketsRow2");

            playeroutRow1.innerHTML = "<th>Players</th>";
            ballsRow1.innerHTML = "<td>Balls</td>";
            wicketsRow1.innerHTML = "<td>Total Wickets</td>";
            playeroutRow2.innerHTML = "<th>Players</th>";
            ballsRow2.innerHTML = "<td>Balls</td>";
            wicketsRow2.innerHTML = "<td>Total Wickets</td>";

            const team1_balls = data.team1_balls;
            const team2_balls = data.team2_balls;
            const team1_outs = Object.values(data.team1_outs);
            const team2_outs = Object.values(data.team2_outs);

            for (let i = 0; i < team1Players.length; i++) {
                const playerCell = document.createElement("th");
                playerCell.textContent = team1Players[i] || "Player " + (i + 1);
                playeroutRow1.appendChild(playerCell);

                const ballsCell = document.createElement("td");
                const playerIndex = i + 1;
                let balls = 0;
                if (team2_balls <= 66) {
                    if (playerIndex * 6 <= team2_balls) {
                        balls = 6;
                    } else if ((playerIndex - 1) * 6 < team2_balls) {
                        balls = team2_balls - (playerIndex - 1) * 6;
                    }
                } else {
                    balls = 6;
                    let remainingBalls = team2_balls - 66;
                    if (playerIndex <= 9) {
                        if (playerIndex * 6 <= remainingBalls) {
                            balls += 6;
                        } else if ((playerIndex - 1) * 6 < remainingBalls) {
                            balls += remainingBalls - (playerIndex - 1) * 6;
                        }
                    }
                }
                ballsCell.textContent = balls;
                ballsRow1.appendChild(ballsCell);
            }

            for (let i = 0; i < team2Players.length; i++) {
                const playerCell = document.createElement("th");
                playerCell.textContent = team2Players[i] || "Player " + (i + 1);
                playeroutRow2.appendChild(playerCell);

                const ballsCell = document.createElement("td");
                const playerIndex = i + 1
                let balls = 0;
                if (team1_balls <= 66) {
                    if (playerIndex * 6 <= team1_balls) {
                        balls = 6;
                    } else if ((playerIndex - 1) * 6 < team1_balls) {
                        balls = team1_balls - (playerIndex - 1) * 6;
                    }
                } else {
                    balls = 6;
                    let remainingBalls = team1_balls - 66;
                    if (playerIndex <= 9) {
                        if (playerIndex * 6 <= remainingBalls) {
                            balls += 6;
                        } else if ((playerIndex - 1) * 6 < remainingBalls) {
                            balls += remainingBalls - (playerIndex - 1) * 6;
                        }
                    }
                }
                ballsCell.textContent = balls;
                ballsRow2.appendChild(ballsCell);
            }

            for (let i = 0; i < team1_outs.length; i++) {
                if (team1_outs[i] !== 0) {
                    const outCell = document.createElement("td");
                    outCell.textContent = team1_outs[i];
                    wicketsRow1.appendChild(outCell);
                } else {
                    const outCell = document.createElement("td");
                    outCell.textContent = "-";
                    wicketsRow1.appendChild(outCell);
                }
            }

            for (let i = 0; i < team2_outs.length; i++) {
                if (team2_outs[i] !== 0) {
                    const outCell = document.createElement("td");
                    outCell.textContent = team2_outs[i];
                    wicketsRow2.appendChild(outCell);
                } else {
                    const outCell = document.createElement("td");
                    outCell.textContent = "-";
                    wicketsRow2.appendChild(outCell);
                }
            }
        }

        function triggerConfetti(containerId) {
            const container = document.getElementById(containerId);
            const rect = container.getBoundingClientRect();
            const confettiSettings = {
                target: { x: rect.left + rect.width / 2, y: rect.top + rect.height / 2, radius: rect.width / 2 },
                max: 200,
                props: ['circle', 'square', 'triangle', 'line'],
                colors: [[165, 104, 246], [230, 61, 135], [0, 199, 228], [253, 214, 126]],
                clock: 25,
                rotate: true,
                start_from_edge: true,
                respawn: false,
            };
            confetti(confettiSettings);
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
            padding: 14px;
            border-radius: 10px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
            width: 350px;
            text-align: center;
        }

        label {
            font-weight: bold;
            margin-bottom: 5px;
        }

        .score-buttons {
            display: flex;
            justify-content: center;
            gap: 10px;
            margin-top: 10px;
        }

        .score-buttons button {
            background-color: #ede4e6;
            color: white;
            font-size: 20px;
            padding: 10px;
            border: none;
            border-radius: 30px;
            cursor: pointer;
            transition: background 0.3s;
            width: 80px;
            height: 50px;
            display: flex;
            align-items: center;
            justify-content: center;
        }

        .score-buttons button:hover {
            background-color: #f4729e;
        }

        .score-buttons button:disabled {
            opacity: 0.5;
            cursor: not-allowed;
        }

        .score-buttons button:disabled:hover {
            background: #ede4e6;
            opacity: 0.5;
        }

        .out-button {
            background-color: #ff1a5f;
            color: white;
            font-size: 16px;
            padding: 8px 12px;
            border: none;
            border-radius: 60px;
            cursor: pointer;
            transition: background 0.3s;
        }

        .out-button:disabled {
            opacity: 0.5;
            cursor: not-allowed;
        }

        .out-button:hover {
            background-color: #b40330;
        }

        .out-button:disabled:hover {
            opacity: 0.5;
        }

        .out-button:disabled {
            opacity: 0.5;
            cursor: not-allowed;
        }

        .update-btn {
            background-color: #0a6dd8;
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

        table tbody tr:hover {
            background-color: #e6f7ff;
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
        <h3 id="team1-stats" style="display: flex; justify-content: center;margin:0"></h3>

        <div class="score-buttons" id="team1-container">
            <button onclick="updateScore(1, 'team1')">1️⃣</button>
            <button onclick="updateScore(2, 'team1')">✌️</button>
            <button onclick="updateScore(4, 'team1')">4️⃣</button>
            <button onclick="updateScore(6, 'team1')">🏏6️⃣</button>
        </div>
        <br/>
        <button class="out-button" onclick="updateScore('out', 'team1')" id="team1-out">🚫 Out</button>
        <br/>
        <br/>
        <div id="bowler1"></div>
    </div>

    <div id="team2stats" class="team-stats">
        <label id="team2"></label>
        <h3 id="team2-stats" style="display: flex; justify-content: center;margin:0"></h3>
        <div class="score-buttons" id="team2-container">
            <button onclick="updateScore(1, 'team2')">1️⃣</button>
            <button onclick="updateScore(2, 'team2')">✌️</button>
            <button onclick="updateScore(4, 'team2')">4️⃣</button>
            <button onclick="updateScore(6, 'team2')">🏏6️⃣</button>
        </div>
        <br/>
        <button class="out-button" onclick="updateScore('out', 'team2')" id="team2-out">🚫 Out</button>
        <br/>
        <br/>
        <div id="bowler2"></div>
    </div>
</div>
<br/>
<h3 id="team1table">Team1 Score-table</h3>
<table border="1">
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
        <td>Wicket Taker</td>
    </tr>
    </tbody>
</table>

<h3 id="team2table">Team2 Score-table</h3>
<table border="1">
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
        <td>Wicket Taker</td>
    </tr>
    </tbody>
</table>
<h3 id="team1outtable">Team1 Wicket-table</h3>
<table border="1">
    <thead>
    <tr id="playeroutRow1">
        <th>Players</th>
    </tr>
    </thead>
    <tbody>
    <tr id="ballsRow1">
        <td>Balls</td>
    </tr>
    <tr id="wicketsRow1">
        <td>Total Wickets</td>
    </tr>
    </tbody>
</table>

<h3 id="team2outtable">Team2 Wicket-table</h3>
<table border="1">
    <thead>
    <tr id="playeroutRow2">
        <th>Players</th>
    </tr>
    </thead>
    <tbody>
    <tr id="ballsRow2">
        <td>Balls</td>
    </tr>
    <tr id="wicketsRow2">
        <td>Total Wickets</td>
    </tr>
    </tbody>
</table>
</body>
</html>
