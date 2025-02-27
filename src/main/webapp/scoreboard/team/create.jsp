<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>New Team</title>
    <style>
        body {
            font-family: 'Arial', sans-serif;
            text-align: center;
            margin: 0;
            padding: 0;
            background-color: #f4f4f4;
        }

        .container {
            max-width: 600px;
            margin: 50px auto;
            padding: 25px;
            background: white;
            border-radius: 12px;
            box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
        }

        h1 {
            color: #333;
            font-size: 26px;
            margin-bottom: 20px;
        }

        .form-group {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 15px;
            margin-bottom: 15px;
            text-align: left;
        }

        .form-group label {
            font-weight: bold;
            display: block;
            margin-bottom: 5px;
        }

        input {
            width: 100%;
            padding: 10px;
            border: 1px solid #ccc;
            border-radius: 5px;
            font-size: 16px;
        }

        button {
            display: inline-block;
            padding: 12px 18px;
            font-size: 16px;
            cursor: pointer;
            border-radius: 60px;
            background: #007bff;
            color: white;
            border: none;
            transition: 0.3s ease;
        }

        button:hover {
            background: #0056b3;
        }

        .link-btn {
            display: inline-block;
            padding: 10px 15px;
            background: #28a745;
            color: white;
            text-decoration: none;
            border-radius: 60px;
            margin-top: 15px;
            transition: 0.3s ease;
        }

        .link-btn:hover {
            background: #1f7a32;
        }

        #result {
            margin-top: 20px;
            font-weight: bold;
            color: red;
        }

        h3 {
            font-size: 18px;
            margin-top: 20px;
        }

        .full-width {
            grid-column: span 2;
        }
    </style>

    <script>
        function addTeam() {
            const result = document.getElementById("result");
            const name = document.getElementById("name").value;
            const players = [];

            for (let i = 1; i <= 11; i++) {
                players.push(document.getElementById(`player${i}`).value);
            }

            const team = {
                name: name,
                players: players
            };
            result.style.color = "blue";
            result.innerHTML = "Creating team...";

            fetch('/api/teams', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(team)
            })
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
                    result.innerHTML = data.message;
                    result.style.color = "green";
                })
                .catch(error => {
                    result.innerHTML = error.message;
                    result.style.color = "red";
                });
        }

        function fillPlayers() {
            const players = [
                "Abdul", "Rahman", "Manoj", "Vishnu", "Saran", "Siva", "Satish", "Suresh", "Rajesh", "Kumar", "Ravi"
            ];

            players.sort(() => Math.random() - 0.5);

            for (let i = 1; i <= 11; i++) {
                document.getElementById(`player${i}`).value = players[i - 1];
            }
        }
    </script>
</head>
<body>

<div class="container">
    <h1>Scoreboard - Create Team</h1>

    <h3><label for="name">Team Name</label></h3>
    <div class="form-group full-width">
        <input type="text" id="name" placeholder="Enter Team Name" required>
    </div>

    <h3>Players</h3>
    <button onclick="fillPlayers()">Fill Players</button>
    <div class="form-group">
        <div>
            <label for="player1">Player 1</label>
            <input type="text" id="player1" placeholder="Enter 1st Player Name" required>
        </div>
        <div>
            <label for="player2">Player 2</label>
            <input type="text" id="player2" placeholder="Enter 2nd Player Name" required>
        </div>
    </div>
    <div class="form-group">
        <div>
            <label for="player3">Player 3</label>
            <input type="text" id="player3" placeholder="Enter 3rd Player Name" required>
        </div>
        <div>
            <label for="player4">Player 4</label>
            <input type="text" id="player4" placeholder="Enter 4th Player Name" required>
        </div>
    </div>
    <div class="form-group">
        <div>
            <label for="player5">Player 5</label>
            <input type="text" id="player5" placeholder="Enter 5th Player Name" required>
        </div>
        <div>
            <label for="player6">Player 6</label>
            <input type="text" id="player6" placeholder="Enter 6th Player Name" required>
        </div>
    </div>
    <div class="form-group">
        <div>
            <label for="player7">Player 7</label>
            <input type="text" id="player7" placeholder="Enter 7th Player Name" required>
        </div>
        <div>
            <label for="player8">Player 8</label>
            <input type="text" id="player8" placeholder="Enter 8th Player Name" required>
        </div>
    </div>
    <div class="form-group">
        <div>
            <label for="player9">Player 9</label>
            <input type="text" id="player9" placeholder="Enter 9th Player Name" required>
        </div>
        <div>
            <label for="player10">Player 10</label>
            <input type="text" id="player10" placeholder="Enter 10th Player Name" required>
        </div>
    </div>
    <div class="form-group">
        <div>
            <label for="player11">Player 11</label>
            <input type="text" id="player11" placeholder="Enter 11th Player Name" required>
        </div>
    </div>

    <button onclick="addTeam()">Create Team</button>
    <p id="result"></p>
    <a href="index.jsp">All Teams</a>
</div>

</body>
</html>
