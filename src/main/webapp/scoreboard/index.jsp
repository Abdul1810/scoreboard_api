<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Score Board</title>
    <style>
        body {
            font-family: 'Arial', sans-serif;
            text-align: center;
            margin: 0;
            padding: 0;
            background-color: #f4f4f4;
        }

        h1 {
            color: #333;
            font-size: 28px;
            margin-top: 20px;
            font-weight: 600;
        }

        .container {
            max-width: 800px;
            margin: 20px auto;
            padding: 20px;
            background: white;
            border-radius: 12px;
            box-shadow: 0 0 15px rgba(0, 0, 0, 0.1);
        }

        .matches-list {
            margin-top: 20px;
            display: grid;
            gap: 15px;
            grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
        }

        .match-item {
            background: #fff;
            border-radius: 10px;
            padding: 15px;
            box-shadow: 0 3px 6px rgba(0, 0, 0, 0.1);
            transition: all 0.3s ease-in-out;
        }

        .match-item:hover {
            transform: translateY(-5px);
            box-shadow: 0 6px 12px rgba(0, 0, 0, 0.15);
        }

        .match-item a {
            font-size: 18px;
            color: #007bff;
            text-decoration: none;
            font-weight: 500;
            display: block;
            text-align: center;
            padding: 10px;
            border-radius: 8px;
            transition: background-color 0.3s ease;
        }

        .match-item a:hover {
            background-color: #007bff;
            color: white;
        }

        .match-date {
            font-size: 14px;
            color: #777;
            margin-top: 8px;
            text-align: center;
        }

        .no-matches {
            font-size: 18px;
            color: #555;
            font-style: italic;
        }

        #result {
            font-weight: bold;
            margin-top: 20px;
            color: #28a745;
        }

        @media (max-width: 768px) {
            h1 {
                font-size: 24px;
            }
        }
    </style>
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
                            date: 'dd-MM-yyyy'
                        },
                        {
                            id: 2,
                            team1: 'RCB',
                            team2: 'KKR',
                            date: 'dd-MM-yyyy'
                        }
                    ]
                }
                 */
                const matches = document.getElementById('matches');
                matches.innerHTML = '';
                if (data.length === 0) {
                    const noMatches = document.createElement('div');
                    noMatches.classList.add('no-matches');
                    noMatches.innerText = 'No matches found';
                    matches.appendChild(noMatches);
                    return;
                }
                data.forEach(match => {
                    const matchDiv = document.createElement('a');
                    matchDiv.href = window.location.href + 'score?id=' + match.id;
                    matchDiv.classList.add('match-item');
                    matchDiv.style.textDecoration = 'none';

                    const matchLink = document.createElement('div');
                    matchLink.innerText = match.team1 + ' vs ' + match.team2;

                    const matchDate = document.createElement('div');
                    matchDate.classList.add('match-date');
                    matchDate.innerText = match.date;

                    matchDiv.appendChild(matchLink);
                    matchDiv.appendChild(matchDate);
                    matches.appendChild(matchDiv);
                });
            };
        });
    </script>
</head>
<body>
<div class="container">
    <h1>Current Matches</h1>
    <div id="matches" class="matches-list">
    </div>
</div>
</body>
</html>
