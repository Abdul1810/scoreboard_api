<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Score Updater</title>
    <script>
        document.addEventListener('DOMContentLoaded', function() {
            fetch('/api/matches?id=<%= request.getParameter("id") %>')
                .then(response => response.json())
                .then(data => {
                    if (data.error) {
                        document.getElementById("result").innerText = data.error;
                    } else {
                        document.getElementById("team1").innerText = data.team1;
                        document.getElementById("team2").innerText = data.team2;
                    }
                });
        });
        function updateScore() {
            const team1score = document.getElementById("team1score").value;
            const team2score = document.getElementById("team2score").value;
            if (isNaN(team1score) || isNaN(team2score)) {
                document.getElementById("result").innerText = 'Please enter valid scores';
                return;
            }
            if (team1score < 0 || team2score < 0) {
                document.getElementById("result").innerText = 'Please enter valid scores';
                return;
            }
            const result = document.getElementById("result");
            fetch('/update-score?id=<%= request.getParameter("id") %>', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    team1: team1score,
                    team2: team2score
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
<label id="team1"></label>
<input type="number" id="team1score" placeholder="Enter score">
<br>
<br>
<label id="team2"></label>
<input type="number" id="team2score" placeholder="Enter score">
<br>
<br>
<button onclick="updateScore()">Update</button>
</body>
</html>
