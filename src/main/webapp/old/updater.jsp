<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Score Updater</title>
    <script>
        function updateScore() {
            const team1 = document.getElementById("team1").value;
            const team2 = document.getElementById("team2").value;
            const result = document.getElementById("result");
            fetch('/update-stats', {
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
    Score Updater
</h1>
<p id="result">
</p>
<br>
<label for="team1">CSK:</label>
<input type="number" id="team1" placeholder="Enter score">
<br>
<br>
<label for="team2">MI:</label>
<input type="number" id="team2" placeholder="Enter score">
<br>
<br>
<button onclick="updateScore()">Update</button>
</body>
</html>
