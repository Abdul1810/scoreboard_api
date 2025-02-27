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
--     team1_score INTEGER NOT NULL,
--     team2_score INTEGER NOT NULL,
    team1_player1_runs INTEGER DEFAULT NULL,
    team1_player2_runs INTEGER DEFAULT 0,
    team1_player3_runs INTEGER DEFAULT 0,
    team1_player4_runs INTEGER DEFAULT 0,
    team1_player5_runs INTEGER DEFAULT 0,
    team1_player6_runs INTEGER DEFAULT 0,
    team1_player7_runs INTEGER DEFAULT 0,
    team1_player8_runs INTEGER DEFAULT 0,
    team1_player9_runs INTEGER DEFAULT 0,
    team1_player10_runs INTEGER DEFAULT 0,
    team1_player11_runs INTEGER DEFAULT 0,
    team2_player1_runs INTEGER DEFAULT 0,
    team2_player2_runs INTEGER DEFAULT 0,
    team2_player3_runs INTEGER DEFAULT 0,
    team2_player4_runs INTEGER DEFAULT 0,
    team2_player5_runs INTEGER DEFAULT 0,
    team2_player6_runs INTEGER DEFAULT 0,
    team2_player7_runs INTEGER DEFAULT 0,
    team2_player8_runs INTEGER DEFAULT 0,
    team2_player9_runs INTEGER DEFAULT 0,
    team2_player10_runs INTEGER DEFAULT 0,
    team2_player11_runs INTEGER DEFAULT 0,
--     team1_wickets INTEGER DEFAULT 0 NOT NULL,
--     team2_wickets INTEGER DEFAULT 0 NOT NULL,
    team1_player1_wickets INTEGER DEFAULT 0,
    team1_player2_wickets INTEGER DEFAULT 0,
    team1_player3_wickets INTEGER DEFAULT 0,
    team1_player4_wickets INTEGER DEFAULT 0,
    team1_player5_wickets INTEGER DEFAULT 0,
    team1_player6_wickets INTEGER DEFAULT 0,
    team1_player7_wickets INTEGER DEFAULT 0,
    team1_player8_wickets INTEGER DEFAULT 0,
    team1_player9_wickets INTEGER DEFAULT 0,
    team1_player10_wickets INTEGER DEFAULT 0,
    team1_player11_wickets INTEGER DEFAULT 0,
    team2_player1_wickets INTEGER DEFAULT 0,
    team2_player2_wickets INTEGER DEFAULT 0,
    team2_player3_wickets INTEGER DEFAULT 0,
    team2_player4_wickets INTEGER DEFAULT 0,
    team2_player5_wickets INTEGER DEFAULT 0,
    team2_player6_wickets INTEGER DEFAULT 0,
    team2_player7_wickets INTEGER DEFAULT 0,
    team2_player8_wickets INTEGER DEFAULT 0,
    team2_player9_wickets INTEGER DEFAULT 0,
    team2_player10_wickets INTEGER DEFAULT 0,
    team2_player11_wickets INTEGER DEFAULT 0,
    team1_balls INTEGER DEFAULT 0 NOT NULL,
    team2_balls INTEGER DEFAULT 0 NOT NULL,
    current_batting TEXT NOT NULL CHECK(current_batting IN ('team1', 'team2')) DEFAULT 'team1',
    is_completed TEXT NOT NULL CHECK(is_completed IN ('true', 'false')) DEFAULT 'false',
    winner TEXT NOT NULL CHECK(winner IN ('team1', 'team2', 'none', 'tie')) DEFAULT 'none',
    FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE
);

ALTER TABLE match_stats ADD COLUMN team1_wickets INTEGER GENERATED ALWAYS AS (
    team1_player1_wickets + team1_player2_wickets + team1_player3_wickets +
    team1_player4_wickets + team1_player5_wickets + team1_player6_wickets +
    team1_player7_wickets + team1_player8_wickets + team1_player9_wickets +
    team1_player10_wickets + team1_player11_wickets
) STORED;

ALTER TABLE match_stats ADD COLUMN team2_wickets INTEGER GENERATED ALWAYS AS (
    team2_player1_wickets + team2_player2_wickets + team2_player3_wickets +
    team2_player4_wickets + team2_player5_wickets + team2_player6_wickets +
    team2_player7_wickets + team2_player8_wickets + team2_player9_wickets +
    team2_player10_wickets + team2_player11_wickets
) STORED;

PRAGMA foreign_keys = ON;

-- SELECT
--     teams.id AS team_id,
--     teams.name AS team_name,
--     GROUP_CONCAT(players.name, ', ') AS players
-- FROM teams
--          LEFT JOIN players ON teams.id = players.team_id
-- GROUP BY teams.id, teams.name;
