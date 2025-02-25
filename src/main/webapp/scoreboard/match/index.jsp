<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>All Matches</title>
    <style  >
        body {
            font-family: 'Arial', sans-serif;
            text-align: center;
            margin: 0;
            padding: 0;
            background-color: #f4f4f4;
        }

        h1 {
            color: #333;
            font-size: 24px;
            margin: 0;
        }

        .container {
            max-width: 400px;
            margin: 20px auto;
            padding: 20px;
            background: white;
            border-radius: 8px;
            box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
        }

        .matches-list {
            margin-top: 20px;
        }

        .match-item {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 10px 15px;
            background: #fff;
            border-radius: 5px;
            margin-bottom: 10px;
            box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
        }

        .match-item h5 {
            margin: 0;
            font-size: 18px;
            color: #444;
        }

        .match-actions {
            display: flex;
            gap: 10px;
        }

        a, button {
            border: none;
            padding: 8px 12px;
            cursor: pointer;
            font-size: 13px;
            border-radius: 60px;
            transition: 0.3s ease;
        }

        a {
            background: #007bff;
            color: white;
        }

        a:hover {
            background: #0056b3;
        }

        button {
            background: #dc3545;
            color: white;
        }

        button:hover {
            background: #b52b38;
        }

        .create-links {
            margin-top: 20px;
        }

        .create-links a {
            display: inline-block;
            margin: 5px;
            padding: 10px 15px;
            background: #28a745;
            color: white;
        }

        .create-links a:hover {
            background: #1f7a32;
        }

        #result {
            font-weight: bold;
            margin-top: 10px;
        }

    </style>
    <script>
        let socket;
        document.addEventListener('DOMContentLoaded', function () {
            socket = new WebSocket('ws://localhost:8080/ws/matches');

            socket.onopen = function () {
                console.log("WebSocket Connection Established");
            };

            socket.onerror = function (error) {
                console.error("WebSocket Error: ", error);
            };

            socket.onclose = function () {
                document.getElementById("result").innerText = "Connection closed. Refreshing...";
                setTimeout(() => {
                    location.reload();
                }, 3000);
                console.warn("WebSocket Connection Closed");
            };

            socket.onmessage = function (event) {
                const data = JSON.parse(event.data);

                const matchesContainer = document.getElementById('matches');
                matchesContainer.innerHTML = '';

                if (data.length === 0) {
                    matchesContainer.innerHTML = '<p>No matches found</p>';
                    return;
                }

                data.forEach(match => {
                    const matchDiv = document.createElement('div');
                    matchDiv.classList.add('match-item');

                    const matchTitle = document.createElement('h5');
                    matchTitle.innerText = match.team1 + ' vs ' + match.team2;

                    const actionsDiv = document.createElement('div');
                    actionsDiv.classList.add('match-actions');

                    const editLink = document.createElement('a');
                    editLink.innerText = 'Edit';
                    editLink.href = `edit.jsp?id=${match.id}`;

                    const deleteButton = document.createElement('button');
                    deleteButton.innerText = 'Delete';
                    deleteButton.onclick = function () {
                        if (confirm("Are you sure you want to delete this match?")) {
                            fetch(`/api/matches?id=${match.id}`, { method: 'DELETE' })
                                .then(response => response.json())
                                .then(data => {
                                    document.getElementById('result').innerText = data.message;
                                })
                                .catch(error => {
                                    console.error("Delete Error:", error);
                                });
                        }
                    };

                    actionsDiv.appendChild(editLink);
                    actionsDiv.appendChild(deleteButton);
                    matchDiv.appendChild(matchTitle);
                    matchDiv.appendChild(actionsDiv);
                    matchesContainer.appendChild(matchDiv);
                });
            };
        });
    </script>
</head>
<body>
<div class="container">
    <h1>Scoreboard - Matches</h1>
    <div class="create-links">
        <a href="create.jsp">+ Create New Match</a>
        <a href="../team/create.jsp">+ Create New Team</a>
    </div>
    <p id="result"></p>
    <div id="matches" class="matches-list">
        <p>Loading matches...</p>
    </div>
</div>
</body>
</html>
