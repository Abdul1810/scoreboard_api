<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Create New Match</title>
    <script>
        let socket;
        let matchesData = [];

        document.addEventListener('DOMContentLoaded', function () {
            socket = new WebSocket('ws://localhost:8080/ws/matches');

            socket.onopen = function (event) {
                console.log("WebSocket connection opened.");
                document.getElementById("result").innerText = "Connected";
            };

            socket.onmessage = function (event) {
                const jsonData = JSON.parse(event.data);
                console.log("Message received: ", jsonData);
                if (jsonData.action === "CREATE") {
                    matchesData.push(jsonData.data);
                    updateMatches();
                } else if (jsonData.action === "DELETE") {
                    matchesData = matchesData.filter(m => m.id !== jsonData.data);
                    updateMatches();
                } else if (jsonData.action === "REQUEST") {
                    socket.send(JSON.stringify({
                        action: "RESPONSE",
                        id: jsonData.id,
                        data: matchesData
                    }));
                } else if (jsonData.action === "RECEIVE") {
                    matchesData = jsonData.data;
                    updateMatches();
                }
            };

            socket.onclose = function (event) {
                console.log("WebSocket connection closed.");
                document.getElementById("result").innerText = "Disconnected";
            };

            socket.onerror = function (event) {
                console.error("WebSocket error: ", event);
            };

        });

        function updateMatches() {
            const matchesDiv = document.getElementById("matches");
            matchesDiv.innerHTML = "";
            matchesData.forEach(match => {
                const matchDiv = document.createElement("div");
                matchDiv.style.display = "flex";
                matchDiv.style.alignItems = "center";
                matchDiv.style.justifyContent = "center";
                matchDiv.style.gap = "10px";

                const matchLink = document.createElement("h5");
                matchLink.innerText = match.team1 + " vs " + match.team2;

                const editButton = document.createElement("a");
                editButton.innerText = "Edit";
                editButton.href = window.location.pathname + "edit.jsp?id=" + match.id;

                const deleteButton = document.createElement("button");
                deleteButton.innerText = "Delete";
                deleteButton.onclick = function () {
                    matchesData = matchesData.filter(m => m.id !== match.id);
                    updateMatches();
                    socket.send(JSON.stringify({
                        action: "DELETE",
                        data: match.id,
                    }));
                };

                matchDiv.appendChild(matchLink);
                matchDiv.appendChild(editButton);
                matchDiv.appendChild(deleteButton);
                matchesDiv.appendChild(matchDiv);
            });
        }

        function createMatch() {
            const team1 = document.getElementById("team1").value;
            const team2 = document.getElementById("team2").value;
            const newMatch = {
                id: matchesData.length + 1,
                team1: team1,
                team2: team2,
                team1_score: 0,
                team2_score: 0,
                team1_wickets: 0,
                team2_wickets: 0,
                team1_balls: 0,
                team2_balls: 0,
                current_batting: "team1",
                is_completed: false,
                winner: "none"
            };
            matchesData.push(newMatch);
            updateMatches();
            socket.send(JSON.stringify({
                action: "CREATE",
                data: newMatch,
            }));
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
<button onclick="createMatch()">Update</button>
<div id="matches">
</div>
</body>
</html>