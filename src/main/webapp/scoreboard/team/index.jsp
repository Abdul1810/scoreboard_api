<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>All Teams</title>
    <script>
        document.addEventListener('DOMContentLoaded', function () {
            fetchTeams();
        });

        function fetchTeams() {
            fetch('/api/teams')
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
                    const teamsContainer = document.getElementById('team-list');
                    teamsContainer.innerHTML = '';
                    if (data && data.length > 0) {
                        data.forEach(team => {
                            const teamItem = document.createElement('div');
                            teamItem.classList.add('team-item');
                            teamItem.innerHTML = `
                                <div class="info">
                                    <span>${team.name}</span>
                                    <br>
                                    <p>Players:</p>
                                    <ul class="players">
                                        ${team.players.slice(0, 6).map(player => `<li>${player}</li>`).join('')}
                                    </ul>
                                    <ul class="players">
                                        ${team.players.slice(6, 11).map(player => `<li>${player}</li>`).join('')}
                                    </ul>
                                </div>
                                <div class="actions">
                                    <a class="open-btn" href="view.jsp?id=${team.id}">View</a>
                                    <br>
                                    <button class="delete-btn" onclick="deleteTeam('${team.id}')">Delete</button>
                                </div>
                            `;
                            teamsContainer.appendChild(teamItem);
                        });
                    } else {
                        const noTeamsMessage = document.createElement('div');
                        noTeamsMessage.classList.add('no-teams');
                        noTeamsMessage.innerText = 'No teams found.';
                        teamsContainer.appendChild(noTeamsMessage);
                    }
                })
                .catch(error => {
                    console.error(error);

                });
        }

        function deleteTeam(teamId) {
            if (confirm("Are you sure you want to delete this team?")) {
                fetch(`/api/teams?id=${teamId}`, {method: 'DELETE'})
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
