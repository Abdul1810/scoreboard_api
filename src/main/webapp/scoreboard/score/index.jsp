<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Loading...</title>
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
            position: relative;
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

        .green-dot {
            height: 10px;
            width: 10px;
            background-color: limegreen;
            border-radius: 50%;
            display: inline-block;
        }

        .grey-dot {
            height: 10px;
            width: 10px;
            background-color: grey;
            border-radius: 50%;
            display: inline-block;
        }

        .red-dot {
            height: 10px;
            width: 10px;
            background-color: red;
            border-radius: 50%;
            display: inline-block;
        }
    </style>
    <script>
        let socket;
        let current_batting = "team1";
        let team1Players = [];
        let team2Players = [];
        let reconnectInterval = 3000;

        document.addEventListener('DOMContentLoaded', function () {
            const urlParams = new URLSearchParams(window.location.search);
            if (!urlParams.has('id')) {
                document.getElementById("result").innerText = "Match ID is missing.";
                return;
            }
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
                    document.getElementById("result").innerText = error.message;
                    document.title = error.message;
                    disableAllButtons();
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
                updateScoreTable(data);
                updateWicketsTable(data);
                current_batting = data.current_batting;
                document.getElementById("team1-stats").textContent = `${data.team1_score}/${data.team1_wickets} (${data.team1_balls})`;
                document.getElementById("team2-stats").textContent = `${data.team2_score}/${data.team2_wickets} (${data.team2_balls})`;

                if (data.is_completed === "false") {
                    if (data.current_batting === "team1") {
                        document.getElementById("match-result").textContent = `${team1Players[data.active_batsman_index-1]} is batting\n${team1Players[data.passive_batsman_index-1]} is waiting`;
                        document.getElementById("team1stats").style.backgroundColor = "aliceblue";
                        document.getElementById("team2stats").style.backgroundColor = "white";
                        const bowlerName = getBowlerName(data.team1_balls);
                        document.getElementById("bowler1").textContent = `Bowler: ${bowlerName}`;
                        updateWicketsTable(data, bowlerName);
                        document.getElementById("bowler2").textContent = "";
                    } else {
                        document.getElementById("match-result").textContent = `${team2Players[data.active_batsman_index-1]} is batting\n${team2Players[data.passive_batsman_index-1]} is waiting`;
                        document.getElementById("team2stats").style.backgroundColor = "aliceblue";
                        document.getElementById("team1stats").style.backgroundColor = "white";
                        const bowlerName = getBowlerName(data.team2_balls);
                        document.getElementById("bowler2").textContent = `Bowler: ${bowlerName}`;
                        updateWicketsTable(data, bowlerName);
                        document.getElementById("bowler1").textContent = "";
                    }
                } else {
                    if (data.winner === undefined) {
                        document.getElementById("match-result").textContent = "";
                        return;
                    }
                    document.getElementById("match-result").textContent = data.winner !== 'Tie' ? data.winner + " won the match" : "Tie";
                    updateWicketsTable(data, "");
                    if (data.winner === "team1") {
                        document.getElementById("team1stats").style.backgroundColor = "lightgreen";
                        document.getElementById("team2stats").style.backgroundColor = "white";
                        if (!document.getElementById("team1").textContent.includes("Winner")) {
                            document.getElementById("team1").textContent += " (🎉 Winner)";
                        }
                    } else if (data.winner === "team2") {
                        document.getElementById("team2stats").style.backgroundColor = "lightgreen";
                        document.getElementById("team1stats").style.backgroundColor = "white";
                        if (!document.getElementById("team2").textContent.includes("Winner")) {
                            document.getElementById("team2").textContent += " (🎉 Winner)";
                        }
                    } else {
                        document.getElementById("team1stats").style.backgroundColor = "peachpuff";
                        document.getElementById("team2stats").style.backgroundColor = "peachpuff";
                    }
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
            if (balls === 0) return current_batting === "team1" ? team2Players[0] : team1Players[0];
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

        function updateScoreTable(data) {
            const playerRow1 = document.getElementById("playerRow1");
            const runsRow1 = document.getElementById("runsRow1");
            const ballsRow1 = document.getElementById("ballsRow1");
            const outRow1 = document.getElementById("outRow1");
            const playerRow2 = document.getElementById("playerRow2");
            const runsRow2 = document.getElementById("runsRow2");
            const ballsRow2 = document.getElementById("ballsRow2");
            const outRow2 = document.getElementById("outRow2");

            playerRow1.innerHTML = "<th>Players</th>";
            runsRow1.innerHTML = "<td>Runs</td>";
            ballsRow1.innerHTML = "<td>Balls</td>";
            outRow1.innerHTML = "<td>Wicket Taker</td>";
            playerRow2.innerHTML = "<th>Players</th>";
            runsRow2.innerHTML = "<td>Runs</td>";
            ballsRow2.innerHTML = "<td>Balls</td>";
            outRow2.innerHTML = "<td>Wicket Taker</td>";

            const team1_runs = Object.values(data.team1_runs);
            const team2_runs = Object.values(data.team2_runs);

            for (let i = 0; i < team1Players.length; i++) {
                const playerCell = document.createElement("th");
                playerCell.innerHTML = team1Players[i] || "Player " + (i + 1);

                // Add green dot for active batsman and grey dot for passive batsman
                if (data.current_batting === "team1") {
                    if (i === data.active_batsman_index - 1) {
                        playerCell.innerHTML += ' <span class="green-dot"></span>';
                    } else if (i === data.passive_batsman_index - 1) {
                        playerCell.innerHTML += ' <span class="grey-dot"></span>';
                    }
                }

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
                playerCell.innerHTML = team2Players[i] || "Player " + (i + 1);

                // Add green dot for active batsman and grey dot for passive batsman
                if (data.current_batting === "team2") {
                    if (i === data.active_batsman_index - 1) {
                        playerCell.innerHTML += ' <span class="green-dot"></span>';
                    } else if (i === data.passive_batsman_index - 1) {
                        playerCell.innerHTML += ' <span class="grey-dot"></span>';
                    }
                }

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

            for (let i = 0; i < data.team1_balls_map.length; i++) {
                if (data.team1_balls_map[i] !== "0") {
                    const outCell = document.createElement("td");
                    outCell.textContent = data.team1_balls_map[i];
                    ballsRow1.appendChild(outCell);
                } else {
                    const outCell = document.createElement("td");
                    outCell.textContent = "-";
                    ballsRow1.appendChild(outCell);
                }
            }

            for (let i = 0; i < data.team2_balls_map.length; i++) {
                if (data.team2_balls_map[i] !== "0") {
                    const outCell = document.createElement("td");
                    outCell.textContent = data.team2_balls_map[i];
                    ballsRow2.appendChild(outCell);
                } else {
                    const outCell = document.createElement("td");
                    outCell.textContent = "-";
                    ballsRow2.appendChild(outCell);
                }
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

        function updateWicketsTable(data, bowlerName) {
            const playeroutRow1 = document.getElementById("playeroutRow1");
            const ballsRow1 = document.getElementById("ballsThrownRow1");
            const wicketsRow1 = document.getElementById("wicketsRow1");
            const playeroutRow2 = document.getElementById("playeroutRow2");
            const ballsRow2 = document.getElementById("ballsThrownRow2");
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

                if (data.current_batting === "team2" && team1Players[i] === bowlerName) {
                    playerCell.innerHTML += ' <span class="red-dot"></span>';
                }

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

                if (data.current_batting === "team1" && team2Players[i] === bowlerName) {
                    playerCell.innerHTML += ' <span class="red-dot"></span>';
                }

                playeroutRow2.appendChild(playerCell);

                const ballsCell = document.createElement("td");
                const playerIndex = i + 1;
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
    </script>
</head>
<body>
<h3>Scoreboard - View</h3>
<p id="result"></p>
<p id="match-result"></p>
<div class="container">
    <div id="team1stats" class="team-stats">
        <label id="team1"></label>
        <h1 id="team1-stats" style="display: flex; justify-content: space-between;margin:0"></h1>
        <br>
        <hr>
        <br>
        <div id="bowler1"></div>
    </div>
    <div id="team2stats" class="team-stats">
        <label id="team2"></label>
        <h1 id="team2-stats" style="display: flex; justify-content: space-between;margin:0"></h1>
        <br>
        <hr>
        <br>
        <div id="bowler2"></div>
    </div>
</div>
<br>

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
    <tr id="ballsRow1">
        <td>Balls</td>
    </tr>
    <tr id="outRow1">
        <td>Wicket Taker</td>
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
    <tr id="ballsRow2">
        <td>Balls</td>
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
    <tr id="ballsThrownRow1">
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
    <tr id="ballsThrownRow2">
        <td>Balls</td>
    </tr>
    <tr id="wicketsRow2">
        <td>Total Wickets</td>
    </tr>
    </tbody>
</table>
</body>
</html>
