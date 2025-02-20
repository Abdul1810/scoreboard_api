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
                    const matchLink = document.createElement('a');
                    matchLink.href = window.location.pathname + 'score?id=' + match.id;
                    matchLink.innerText = match.team1 + ' vs ' + match.team2;
                    matchDiv.appendChild(matchLink);
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
<div id="matches">
</div>
</body>
</html>
