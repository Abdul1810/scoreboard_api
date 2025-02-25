<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>All Teams</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            background-color: #f4f4f4;
            margin: 0;
            padding: 0;
        }

        .container {
            width: 80%;
            margin: 50px auto;
            padding: 20px;
            background-color: white;
            border-radius: 8px;
            box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
        }

        h1 {
            text-align: center;
            color: #333;
            margin: 0;
        }

        .team-list {
            margin-top: 20px;
        }

        .team-item {
            padding: 20px;
            margin: 10px 0;
            background-color: #fafafa;
            border: 1px solid #ddd;
            border-radius: 8px;
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
        }

        .team-item .info {
            flex: 1;
        }

        .team-item .info span {
            font-size: 18px;
            font-weight: bold;
        }

        .team-item .players {
            display: grid;
            grid-template-columns: 1fr 1fr; /* 2 columns */
            gap: 10px;
            margin-top: 10px;
        }

        .team-item .players li {
            font-size: 14px;
            color: #555;
        }

        .team-item .delete-btn {
            padding: 10px 15px;
            background-color: #dc3545;
            color: white;
            border: none;
            border-radius: 5px;
            cursor: pointer;
            transition: background-color 0.3s;
        }

        .team-item .delete-btn:hover {
            background-color: #c82333;
        }

        .no-teams {
            text-align: center;
            font-size: 18px;
            color: #666;
        }

        .create-team-btn {
            display: block;
            width: 200px;
            margin: 30px auto;
            padding: 10px;
            text-align: center;
            background-color: #28a745;
            color: white;
            font-size: 16px;
            border-radius: 30px;
            text-decoration: none;
        }

        .create-team-btn:hover {
            background-color: #218838;
        }
    </style>
    <script>
        let socket;
        document.addEventListener('DOMContentLoaded', function () {
            // fetchTeams();
            socket = new WebSocket('ws://localhost:8080/scoreboard/teams');
            socket.onopen = function () {
                console.log('WebSocket connection established.');
            };

            socket.onmessage = function (event) {
                console.log('Message received:', event.data);
                updateUI(JSON.parse(event.data));
            };

            socket.onclose = function () {
                console.log('WebSocket connection closed.');
                setTimeout(() => {
                    document.getElementById('result').innerText = 'Connection closed. Reconnecting...';
                    location.reload();
                }, 3000);
            };
        });

        function updateUI(data) {
            const teamsContainer = document.getElementById('team-list');
            teamsContainer.innerHTML = '';
        }

        <%--function fetchTeams() {--%>
        <%--    fetch('/api/teams')--%>
        <%--        .then(response => {--%>
        <%--            if (response.status >= 200 && response.status < 300) {--%>
        <%--                return response.json();--%>
        <%--            } else {--%>
        <%--                return response.json().then(errorData => {--%>
        <%--                    throw new Error(errorData.message || 'Something went wrong');--%>
        <%--                });--%>
        <%--            }--%>
        <%--        })--%>
        <%--        .then(data => {--%>
        <%--            const teamsContainer = document.getElementById('team-list');--%>
        <%--            teamsContainer.innerHTML = '';--%>
        <%--            if (data && data.length > 0) {--%>
        <%--                data.forEach(team => {--%>
        <%--                    const teamItem = document.createElement('div');--%>
        <%--                    teamItem.classList.add('team-item');--%>
        <%--                    teamItem.innerHTML = `--%>
        <%--                        <div class="info">--%>
        <%--                            <span>${team.name}</span>--%>
        <%--                            <br>--%>
        <%--                            <p>Players:</p>--%>
        <%--                            <ul class="players">--%>
        <%--                                ${team.players.slice(0, 6).map(player => `<li>${player}</li>`).join('')}--%>
        <%--                            </ul>--%>
        <%--                            <ul class="players">--%>
        <%--                                ${team.players.slice(6, 11).map(player => `<li>${player}</li>`).join('')}--%>
        <%--                            </ul>--%>
        <%--                        </div>--%>
        <%--                        <button class="delete-btn" onclick="deleteTeam('${team.id}')">X Delete</button>--%>
        <%--                    `;--%>
        <%--                    teamsContainer.appendChild(teamItem);--%>
        <%--                });--%>
        <%--            } else {--%>
        <%--                const noTeamsMessage = document.createElement('div');--%>
        <%--                noTeamsMessage.classList.add('no-teams');--%>
        <%--                noTeamsMessage.innerText = 'No teams found.';--%>
        <%--                teamsContainer.appendChild(noTeamsMessage);--%>
        <%--            }--%>
        <%--        })--%>
        <%--        .catch(error => {--%>
        <%--            console.error(error);--%>

        <%--        });--%>
        <%--}--%>

        function deleteTeam(teamId) {
            if (confirm("Are you sure you want to delete this team?")) {
                fetch(`/api/teams?id=${teamId}`, { method: 'DELETE' })
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
                        document.getElementById('result').innerText = data.message;
                            fetchTeams();
                    })
                    .catch(error => {
                        console.error("Delete Error:", error);
                            fetchTeams();
                        document.getElementById('result').innerText = error.message;
                    });
            }
        }
    </script>
</head>
<body>
<div class="container">
    <h1>Scoreboard - Teams</h1>
    <div id="result" style="display: flex; justify-content: center;">
    </div>
    <a href="create.jsp" class="create-team-btn">+ Create New Team</a>
    <div id="team-list" class="team-list">
    </div>
</div>
</body>
</html>
