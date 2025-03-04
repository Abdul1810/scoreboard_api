<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Team View</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            background-color: #f4f4f4;
            margin: 0;
            padding: 0;
        }

        .container {
            width: 70%;
            margin: 40px auto;
            padding: 30px;
            background-color: white;
            border-radius: 12px;
            box-shadow: 0 0 20px rgba(0, 0, 0, 0.08);
        }

        h1 {
            color: #333;
            text-align: center;
            margin-bottom: 10px;
        }

        #result {
            text-align: center;
            color: #007bff;
            margin: 15px 0;
            padding: 10px;
            background-color: #f8f9fa;
            border-radius: 6px;
        }

        .team-info {
            margin-top: 25px;
        }

        .team-info p {
            font-size: 18px;
            font-weight: 600;
            color: #444;
            margin-bottom: 15px;
            border-bottom: 1px solid #eee;
            padding-bottom: 8px;
        }

        .players {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
            gap: 15px;
            padding-left: 0;
            list-style-type: none;
        }

        .players li {
            font-size: 16px;
            color: #333;
            background-color: #f8f9fa;
            padding: 15px;
            border-radius: 8px;
            border: 1px solid #e9ecef;
            display: flex;
            justify-content: space-between;
            align-items: center;
            transition: transform 0.2s, box-shadow 0.2s;
        }

        .players li:hover {
            transform: translateY(-2px);
            box-shadow: 0 4px 8px rgba(0,0,0,0.05);
        }

        .player-name {
            font-weight: 500;
        }

        .action-buttons {
            display: flex;
            gap: 12px;
        }

        .calculate-btn {
            background: none;
            border: none;
            color: #007bff;
            font-size: 14px;
            cursor: pointer;
            padding: 0;
            opacity: 0.85;
            transition: opacity 0.2s, color 0.2s;
        }

        .calculate-btn:hover {
            color: #0056b3;
            opacity: 1;
            text-decoration: underline;
        }

        .back-btn {
            display: block;
            width: 180px;
            margin: 30px auto 10px;
            padding: 10px;
            text-align: center;
            background-color: #007bff;
            color: white;
            font-size: 16px;
            border-radius: 30px;
            text-decoration: none;
            transition: background-color 0.3s, transform 0.2s;
        }

        .back-btn:hover {
            background-color: #0056b3;
            transform: translateY(-2px);
        }
    </style>
    <script>
        document.addEventListener('DOMContentLoaded', function () {
            fetchTeamDetails();
        });

        function fetchTeamDetails() {
            const urlParams = new URLSearchParams(window.location.search);
            const teamId = urlParams.get('id');
            if (!teamId) {
                document.getElementById('team-name').innerText = 'Error';
                document.getElementById('team-info').innerHTML = '<p class="error-message">Team ID is missing.</p>';
                return;
            }

            fetch(`/api/teams?id=<%= request.getParameter("id") %>`)
                .then(response => {
                    if (response.status >= 200 && response.status < 300) {
                        return response.json();
                    } else {
                        return response.json().then(errorData => {
                            throw new Error(errorData.message || 'Something went wrong');
                        });
                    }
                })
                .then(team => {
                    document.getElementById('team-name').innerText = team.name;
                    document.getElementById('team-players').innerHTML = team.players
                        .map(player => `
                            <li>
                                <span class="player-name">${player}</span>
                                <div class="action-buttons">
                                    <button class="calculate-btn" onclick="calculateScore('${player}')">Runs</button>
                                    <button class="calculate-btn" onclick="calculateWickets('${player}')">Wickets</button>
                                </div>
                            </li>
                        `).join('');
                })
                .catch(error => {
                    console.error("Fetch Error:", error);
                    document.getElementById('team-info').innerText = error.message;
                });
        }

        function calculateScore(player) {
            document.getElementById('result').innerText = "Loading...";
            fetch("/api/total-score?player=" + player + "&teamId=<%= request.getParameter("id") %>")
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
                    document.getElementById('result').innerText = `${player} has scored ${data.total_score} runs\nBalls faced: ${data.total_balls}\nMatches played: ${data.matches_played}`;
                    <%--document.getElementById('result').innerText = `${player}'s total score is ${data.total_score} in ${data.matches_played} matches.`;--%>
                })
                .catch(error => {
                    console.error("Fetch Error:", error);
                    document.getElementById('result').innerText = error.message;
                });
        }

        function calculateWickets(player) {
            document.getElementById('result').innerText = "Loading...";
            fetch("/api/total-wickets?player=" + player + "&teamId=<%= request.getParameter("id") %>")
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
                    document.getElementById('result').innerText = `${player} has taken ${data.total_wickets} wickets\nBall bowled: ${data.balls_bowled}\nMatches bowled: ${data.matches_bowled}`;
                })
                .catch(error => {
                    console.error("Fetch Error:", error);
                    document.getElementById('result').innerText = error.message;
                });
        }
    </script>
</head>
<body>
<div class="container">
    <h1 id="team-name">Loading...</h1>
    <h3 id="result"></h3>
    <div id="team-info" class="team-info">
        <p>Players:</p>
        <ul id="team-players" class="players"></ul>
    </div>
    <a href="index.jsp" class="back-btn">← Back to Teams</a>
</div>
</body>
</html>