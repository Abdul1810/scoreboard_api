<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Score Board</title>
    <script>
        let socket;
        document.addEventListener('DOMContentLoaded', function () {
            socket = new WebSocket('ws://localhost:8080/ws/matches');
            socket.onmessage = function (event) {
                const data = JSON.parse(event.data);
                /*
                {
                    matches: [
                        {
                            id: 1,
                            team1: 'CSK',
                            team2: 'MI',
                        },
                        {
                            id: 2,
                            team1: 'RCB',
                            team2: 'KKR',
                        }
                    ]
                }
                 */
                const matches = document.getElementById('matches');
                matches.innerHTML = '';
                data.forEach(match => {
                    const matchDiv = document.createElement('div');
                    matchDiv.style.display = 'flex';
                    matchDiv.style.alignItems = 'center';
                    matchDiv.style.justifyContent = 'center';
                    matchDiv.style.gap = '10px';

                    const matchLink = document.createElement('h5');
                    matchLink.innerText = match.team1 + ' vs ' + match.team2;

                    const editButton = document.createElement('a');
                    editButton.innerText = 'Edit';
                    editButton.href = window.location.pathname + 'edit.jsp?id=' + match.id;

                    const deleteButton = document.createElement('button');
                    deleteButton.innerText = 'Delete';
                    deleteButton.onclick = function () {
                        fetch('/api/matches?id=' + match.id, {
                            method: 'DELETE'
                        })
                            .then(response => response.text())
                            .then(data => {
                                document.getElementById('result').innerText = data;
                            });
                    };

                    matchDiv.appendChild(matchLink);
                    matchDiv.appendChild(editButton);
                    matchDiv.appendChild(deleteButton);
                    matches.appendChild(matchDiv);
                });
            };
        });
    </script>
</head>
<body style="text-align: center; font-family: Arial, sans-serif;">
<h1>
    Current Matches
</h1>
<p id="result">
</p>
<div id="matches">
</div>
</body>
</html>
