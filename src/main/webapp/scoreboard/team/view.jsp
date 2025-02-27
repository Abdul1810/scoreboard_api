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
            width: 60%;
            margin: 50px auto;
            padding: 20px;
            background-color: white;
            border-radius: 8px;
            box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
            text-align: center;
        }

        h1 {
            color: #333;
        }

        .team-info {
            margin-top: 20px;
            text-align: left;
        }

        .team-info p {
            font-size: 18px;
            font-weight: bold;
        }

        .players {
            display: grid;
            grid-template-columns: 1fr 1fr; /* 2 columns */
            gap: 10px;
            margin-top: 10px;
            padding-left: 0;
            list-style-type: none;
        }

        .players li {
            font-size: 16px;
            color: #555;
            background-color: #fafafa;
            padding: 10px;
            border-radius: 5px;
            border: 1px solid #ddd;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .calculate-btn {
            background: none;
            border: none;
            color: #007bff;
            font-size: 14px;
            cursor: pointer;
            text-decoration: underline;
            padding: 0;
        }

        .calculate-btn:hover {
            color: #0056b3;
        }

        .back-btn {
            display: block;
            width: 200px;
            margin: 30px auto;
            padding: 10px;
            text-align: center;
            background-color: #007bff;
            color: white;
            font-size: 16px;
            border-radius: 30px;
            text-decoration: none;
            transition: background-color 0.3s;
        }

        .back-btn:hover {
            background-color: #0056b3;
        }

        .error-message {
            color: red;
            font-size: 18px;
            text-align: center;
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
                document.getElementById('team-name').innerText = 'Try again';
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
                                ${player}
                                <button class="calculate-btn" onclick="calculateScore('${player}')">Calculate</button>
                            </li>
                        `).join('');
                })
                .catch(error => {
                    console.error("Fetch Error:", error);
                    document.getElementById('team-info').innerHTML = `<p class="error-message">${error.message}</p>`;
                });
        }

        function calculateScore(player) {
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
                    document.getElementById('result').innerText = `${player}'s total score is ${data.total_score} in ${data.matches_played} matches.`;
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
