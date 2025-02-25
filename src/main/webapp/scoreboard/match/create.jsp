<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>New Match</title>
    <style>
        body {
            font-family: 'Arial', sans-serif;
            text-align: center;
            margin: 0;
            padding: 0;
            background-color: #f4f4f4;
        }

        .container {
            max-width: 400px;
            margin: 40px auto;
            padding: 20px;
            background: white;
            border-radius: 8px;
            box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
        }

        h1 {
            color: #333;
            font-size: 24px;
        }

        .form-group {
            margin-bottom: 15px;
            text-align: left;
        }

        label {
            font-weight: bold;
            display: block;
            margin-bottom: 5px;
        }

        select {
            width: 100%;
            padding: 10px;
            border: 1px solid #ccc;
            border-radius: 5px;
            font-size: 16px;
        }

        button, .link-btn {
            display: inline-block;
            padding: 10px 15px;
            font-size: 13px;
            cursor: pointer;
            border-radius: 60px;
            transition: 0.3s ease;
        }

        button {
            background: #007bff;
            color: white;
            border: none;
            margin-right: 10px;
        }

        button:hover {
            background: #0056b3;
        }

        .link-btn {
            background: #28a745;
            color: white;
        }

        .link-btn:hover {
            background: #1f7a32;
        }

        #result {
            margin-top: 15px;
            font-weight: bold;
            color: red;
        }
    </style>
    <script>
        let teamsData;
        let socket;
        // function fetchTeams() {
        //     fetch('/api/teams')
        //         .then(response => response.json())
        //         .then(data => {
        //             const team1Select = document.getElementById('team1');
        //             const team2Select = document.getElementById('team2');
        //
        //             team1Select.innerHTML = '';
        //             team2Select.innerHTML = '';
        //             team1Select.appendChild(new Option("Select Team 1", ""));
        //             team2Select.appendChild(new Option("Select Team 2", ""));
        //
        //             if (data && data.length > 0) {
        //                 data.forEach(team => {
        //                     const option1 = new Option(team.name, team.id);
        //                     team1Select.appendChild(option1);
        //
        //                     const option2 = new Option(team.name, team.id);
        //                     team2Select.appendChild(option2);
        //                 });
        //             } else {
        //                 const noTeamOption = new Option('No teams available', '', true, true);
        //                 team1Select.appendChild(noTeamOption);
        //                 team2Select.appendChild(noTeamOption);
        //             }
        //         })
        //         .catch(error => {
        //             console.error("Error fetching teams:", error);
        //         });
        // }

        function handleTeam1Selection() {
            const team1Select = document.getElementById('team1');
            const team2Select = document.getElementById('team2');

            const selectedTeam1 = team1Select.value;
            const team2Options = team2Select.querySelectorAll('option');

            team2Options.forEach(option => {
                option.disabled = option.value === selectedTeam1;
            });
            if (selectedTeam1 === team2Select.value) {
                team2Select.value = '';
            }
        }

        function addMatch() {
            const team1 = document.getElementById("team1").value;
            const team2 = document.getElementById("team2").value;
            const result = document.getElementById("result");

            if (!team1 || !team2) {
                result.innerHTML = "Both teams are required!";
                return;
            }

            result.innerHTML = "Creating match...";

            fetch('/api/matches', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ team1: team1, team2: team2 }),
            })
                .then(response => response.json())
                .then(data => {
                    result.innerHTML = data.message;
                    result.style.color = data.success ? "green" : "red";
                })
                .catch(error => {
                    console.error("Error:", error);
                    result.innerHTML = "Error creating match!";
                    result.style.color = "red";
                });
        }

        document.addEventListener('DOMContentLoaded', () => {
            // fetchTeams();
            socket = new WebSocket('ws://localhost:8080/ws/teams');
            socket.onopen = () => {
                console.log('WebSocket connection established.');
            };

            socket.onmessage = event => {
                console.log('Message received:', event.data);
                teamsData = JSON.parse(event.data);
                const team1Select = document.getElementById('team1');
                const team2Select = document.getElementById('team2');

                team1Select.innerHTML = '';
                team2Select.innerHTML = '';
                team1Select.appendChild(new Option("Select Team 1", ""));
                team2Select.appendChild(new Option("Select Team 2", ""));

                if (teamsData && teamsData.length > 0) {
                    teamsData.forEach(team => {
                        const option1 = new Option(team.name, team.id);
                        team1Select.appendChild(option1);

                        const option2 = new Option(team.name, team.id);
                        team2Select.appendChild(option2);
                    });
                } else {
                    const noTeamOption = new Option('No teams available', '', true, true);
                    team1Select.appendChild(noTeamOption);
                    team2Select.appendChild(noTeamOption);
                }
            };

            socket.onclose = () => {
                console.log('WebSocket connection closed.');
                setTimeout(() => {
                    document.getElementById('result').innerText = 'Connection closed. Reconnecting...';
                    location.reload();
                }, 3000);
            };

            document.getElementById('team1').addEventListener('change', handleTeam1Selection);
        });
    </script>
</head>
<body>
<div class="container">
    <h1>Scoreboard - Create Match</h1>
    <div class="form-group">
        <label for="team1">Team 1</label>
        <select id="team1">
            <option value="">Select Team 1</option>
        </select>
    </div>
    <div class="form-group">
        <label for="team2">Team 2</label>
        <select id="team2">
            <option value="">Select Team 2</option>
        </select>
    </div>
    <button onclick="addMatch()">Create Match</button>
    <p id="result"></p>
    <a href="index.jsp">All Matches</a>
</div>
</body>
</html>
