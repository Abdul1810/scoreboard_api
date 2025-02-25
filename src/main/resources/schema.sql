CREATE TABLE teams (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT UNIQUE NOT NULL,
    player1 TEXT NOT NULL,
    player2 TEXT NOT NULL,
    player3 TEXT NOT NULL,
    player4 TEXT NOT NULL,
    player5 TEXT NOT NULL,
    player6 TEXT NOT NULL,
    player7 TEXT NOT NULL,
    player8 TEXT NOT NULL,
    player9 TEXT NOT NULL,
    player10 TEXT NOT NULL,
    player11 TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE matches (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    team1_id INTEGER NOT NULL,
    team2_id INTEGER NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (team1_id) REFERENCES teams(id) ON DELETE CASCADE,
    FOREIGN KEY (team2_id) REFERENCES teams(id) ON DELETE CASCADE
);

CREATE TABLE match_stats (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    match_id INTEGER NOT NULL,
    team1_score INTEGER NOT NULL,
    team2_score INTEGER NOT NULL,
    team1_wickets INTEGER NOT NULL,
    team2_wickets INTEGER NOT NULL,
    team1_balls INTEGER NOT NULL,
    team2_balls INTEGER NOT NULL,
    current_batting TEXT NOT NULL CHECK(current_batting IN ('team1', 'team2')),
    is_completed TEXT NOT NULL CHECK(is_completed IN ('true', 'false')),
    winner TEXT NOT NULL CHECK(winner IN ('team1', 'team2', 'none', 'tie')),
    FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE
);

PRAGMA foreign_keys = ON;

-- SELECT
--     teams.id AS team_id,
--     teams.name AS team_name,
--     GROUP_CONCAT(players.name, ', ') AS players
-- FROM teams
--          LEFT JOIN players ON teams.id = players.team_id
-- GROUP BY teams.id, teams.name;
