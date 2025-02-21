<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Create New Match</title>
    <script>
        function addMatch() {
            const team1 = document.getElementById("team1").value;
            const team2 = document.getElementById("team2").value;
            const result = document.getElementById("result");
            result.innerHTML = "Creating...";
            fetch('/api/matches', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    team1: team1,
                    team2: team2
                })
            })
                .then(response => response.text())
                .then(data => {
                    result.innerHTML = data;
                });
        }
    </script>
</head>
<body style="text-align: center; font-family: Arial, sans-serif;">
<h1>
    Create New Match
</h1>
<p id="result">
</p>
<br>
<label for="team1">Team 1</label>
<input type="text" id="team1" placeholder="Enter 1st Team Name">
<br>
<br>
<label for="team2">Team 2</label>
<input type="text" id="team2" placeholder="Enter 2nd Team Name">
<br>
<br>
<button onclick="addMatch()">Update</button>
</body>
</html>
